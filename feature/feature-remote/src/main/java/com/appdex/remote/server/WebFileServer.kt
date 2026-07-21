package com.appdex.remote.server

import android.content.Context
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStream
import android.util.Log
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URLEncoder
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

/**
 * Lightweight embedded HTTP server using java.net.ServerSocket.
 * No external dependencies — works on all Android versions.
 * Provides a clean HTML interface for browsing, downloading, and uploading files.
 */
class WebFileServer(
    private val context: Context,
    private val rootPath: String,
    private val port: Int = 8080,
    private val authToken: String? = null
) {
    @Volatile
    private var running = false
    private var serverSocket: ServerSocket? = null
    private val executor = Executors.newCachedThreadPool()

    val isRunning: Boolean get() = running

    fun start() {
        if (running) return
        serverSocket = ServerSocket(port)
        serverSocket?.soTimeout = 0 // Accept blocks indefinitely (normal)
        running = true

        Thread {
            while (running) {
                try {
                    val client = serverSocket?.accept() ?: break
                    client.soTimeout = 30000 // 30s read timeout per client
                    executor.execute { handleClient(client) }
                } catch (e: IOException) {
                    if (running) {
                        Log.w("WebFileServer", "Accept error: ${e.message}")
                        continue
                    } else break
                }
            }
        }.apply {
            isDaemon = true
            name = "WebFileServer-Accept"
            start()
        }
    }

    fun stop() {
        running = false
        try { serverSocket?.close() } catch (e: Exception) { Log.w("AppX", "Suppressed exception", e) }
        serverSocket = null
        executor.shutdownNow()
    }

    private fun handleClient(socket: Socket) {
        try {
            val input = BufferedReader(InputStreamReader(socket.getInputStream()))
            val output = socket.getOutputStream()

            val requestLine = input.readLine() ?: return
            val parts = requestLine.split(" ")
            if (parts.size < 3) return
            val method = parts[0]
            val rawUri = parts[1]

            // Read headers
            val headers = mutableMapOf<String, String>()
            while (true) {
                val line = input.readLine() ?: break
                if (line.isEmpty()) break
                val colonIdx = line.indexOf(":")
                if (colonIdx > 0) {
                    headers[line.substring(0, colonIdx).trim().lowercase()] = line.substring(colonIdx + 1).trim()
                }
            }

            // Auth check
            if (authToken != null) {
                val authHeader = headers["authorization"]
                if (authHeader == null || !authHeader.startsWith("Bearer ") || authHeader.removePrefix("Bearer ").trim() != authToken) {
                    sendResponse(output, 401, "Unauthorized", "text/plain")
                    output.flush()
                    return
                }
            }

            // Parse path and query
            val questionIdx = rawUri.indexOf('?')
            val path = if (questionIdx >= 0) rawUri.substring(0, questionIdx) else rawUri
            val query = if (questionIdx >= 0) rawUri.substring(questionIdx + 1) else ""

            when {
                path == "/" || path.startsWith("/browse") -> {
                    val dirPath = extractQueryParam(query, "path") ?: "/"
                    handleBrowse(output, dirPath)
                }
                path.startsWith("/download") -> {
                    val filePath = extractQueryParam(query, "path") ?: "/"
                    handleDownload(output, filePath)
                }
                path.startsWith("/upload") && method == "POST" -> {
                    // Read body
                    val contentLength = headers["content-length"]?.toIntOrNull() ?: 0
                    val body = if (contentLength > 0) {
                        val chars = CharArray(contentLength)
                        input.read(chars, 0, contentLength)
                        String(chars)
                    } else ""
                    handleUpload(output, query, body)
                }
                else -> {
                    sendResponse(output, 404, "Not Found", "text/plain")
                }
            }

            output.flush()
        } catch (e: Exception) {
            Log.e("WebFileServer", "Client handler error: ${e.message}", e)
        } finally {
            try { socket.close() } catch (e: Exception) { Log.w("AppX", "Suppressed exception", e) }
        }
    }

    private fun handleBrowse(output: OutputStream, path: String) {
        val file = resolvePath(path)
        if (file == null) {
            sendResponse(output, 404, "Path not found", "text/plain")
            return
        }
        if (file.isFile) {
            handleDownload(output, path)
            return
        }

        val files = file.listFiles()?.sortedWith(
            compareByDescending<File> { it.isDirectory }.thenBy { it.name.lowercase() }
        ) ?: emptyList()

        val parentPath = file.parentFile?.absolutePath?.removePrefix(rootPath)?.ifEmpty { "/" } ?: "/"
        val html = buildHtmlPage(path, files, parentPath)
        sendResponse(output, 200, html, "text/html; charset=UTF-8")
    }

    private fun handleDownload(output: OutputStream, path: String) {
        val file = resolvePath(path)
        if (file == null || !file.isFile) {
            sendResponse(output, 404, "File not found", "text/plain")
            return
        }

        val contentType = guessContentType(file.extension)
        val header = StringBuilder()
        header.append("HTTP/1.1 200 OK\r\n")
        header.append("Content-Type: $contentType\r\n")
        header.append("Content-Disposition: attachment; filename=\"${file.name}\"\r\n")
        header.append("Content-Length: ${file.length()}\r\n")
        header.append("Connection: close\r\n")
        header.append("\r\n")
        output.write(header.toString().toByteArray(Charsets.UTF_8))

        file.inputStream().use { it.copyTo(output) }
    }

    private fun handleUpload(output: OutputStream, query: String, body: String) {
        val path = extractQueryParam(query, "path") ?: "/"
        val dir = resolvePath(path)
        if (dir == null || !dir.isDirectory) {
            sendResponse(output, 400, "Invalid directory", "text/plain")
            return
        }

        val params = parseFormData(body)
        val fileName = params["filename"] ?: "upload_${System.currentTimeMillis()}"
        val fileContent = params["file"] ?: ""

        val targetFile = File(dir, fileName)
        targetFile.writeText(fileContent)

        val redirectUrl = "/browse?path=${URLEncoder.encode(path, "UTF-8")}"
        val response = StringBuilder()
        response.append("HTTP/1.1 302 Found\r\n")
        response.append("Location: $redirectUrl\r\n")
        response.append("Content-Length: 0\r\n")
        response.append("Connection: close\r\n")
        response.append("\r\n")
        output.write(response.toString().toByteArray(Charsets.UTF_8))
    }

    private fun resolvePath(path: String): File? {
        val root = File(rootPath)
        val target = File(root, path.removePrefix("/"))
        if (!target.canonicalPath.startsWith(root.canonicalPath)) return null
        return if (target.exists()) target else null
    }

    private fun buildHtmlPage(currentPath: String, files: List<File>, parentPath: String): String {
        val sb = StringBuilder()
        sb.append("<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"UTF-8\">")
        sb.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">")
        sb.append("<title>AppX File Server</title>")
        sb.append("<style>")
        sb.append("*{margin:0;padding:0;box-sizing:border-box}")
        sb.append("body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;background:#f0f2f5;color:#1a1a2e}")
        sb.append(".header{background:linear-gradient(135deg,#667eea,#764ba2);color:white;padding:20px;text-align:center}")
        sb.append(".header h1{font-size:24px;margin-bottom:4px}")
        sb.append(".header p{font-size:13px;opacity:0.85}")
        sb.append(".container{max-width:900px;margin:16px auto;padding:0 12px}")
        sb.append(".breadcrumb{background:white;padding:12px 16px;border-radius:10px;margin-bottom:12px;font-size:14px;box-shadow:0 1px 3px rgba(0,0,0,0.08)}")
        sb.append(".breadcrumb a{color:#667eea;text-decoration:none}")
        sb.append(".breadcrumb a:hover{text-decoration:underline}")
        sb.append(".file-list{background:white;border-radius:10px;overflow:hidden;box-shadow:0 1px 3px rgba(0,0,0,0.08)}")
        sb.append(".file-item{display:flex;align-items:center;padding:12px 16px;border-bottom:1px solid #f0f0f0;cursor:pointer;transition:background 0.15s}")
        sb.append(".file-item:hover{background:#f8f9ff}")
        sb.append(".file-item:last-child{border-bottom:none}")
        sb.append(".file-icon{font-size:22px;margin-right:12px;width:28px;text-align:center}")
        sb.append(".file-name{flex:1;font-size:14px}")
        sb.append(".file-size{font-size:12px;color:#888;margin-right:12px}")
        sb.append(".file-date{font-size:12px;color:#aaa}")
        sb.append(".upload-section{background:white;border-radius:10px;padding:16px;margin-top:12px;box-shadow:0 1px 3px rgba(0,0,0,0.08)}")
        sb.append(".upload-section h3{font-size:14px;margin-bottom:8px;color:#667eea}")
        sb.append(".upload-section input[type=text],.upload-section textarea{width:100%;padding:8px;margin-bottom:8px;border:1px solid #ddd;border-radius:6px;font-size:13px}")
        sb.append(".upload-btn{background:#667eea;color:white;border:none;padding:8px 20px;border-radius:6px;font-size:13px;cursor:pointer}")
        sb.append(".upload-btn:hover{background:#5568d3}")
        sb.append(".footer{text-align:center;padding:16px;font-size:12px;color:#aaa}")
        sb.append("</style></head><body>")

        sb.append("<div class=\"header\"><h1>AppX File Server</h1><p>Remote file management — Port $port</p></div>")
        sb.append("<div class=\"container\">")

        // Breadcrumb
        sb.append("<div class=\"breadcrumb\">📂 <a href=\"/browse?path=/\">Root</a>")
        sb.append(buildBreadcrumb(currentPath))
        sb.append("</div>")

        sb.append("<div class=\"file-list\">")

        // Parent dir
        if (currentPath != "/") {
            val encodedParent = URLEncoder.encode(parentPath, "UTF-8")
            sb.append("<div class=\"file-item\" onclick=\"window.location.href='/browse?path=$encodedParent'\">")
            sb.append("<span class=\"file-icon\">📁</span><span class=\"file-name\">.. (Parent)</span>")
            sb.append("</div>")
        }

        val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        for (f in files) {
            val relativePath = f.absolutePath.removePrefix(rootPath).ifEmpty { "/" }
            val encodedPath = URLEncoder.encode(relativePath, "UTF-8")
            val icon = if (f.isDirectory) "📁" else getFileIcon(f.extension)
            val sizeStr = if (f.isDirectory) "—" else formatFileSize(f.length())
            val dateStr = dateFormatter.format(Date(f.lastModified()))

            if (f.isDirectory) {
                sb.append("<div class=\"file-item\" onclick=\"window.location.href='/browse?path=$encodedPath'\">")
            } else {
                sb.append("<div class=\"file-item\" onclick=\"window.location.href='/download?path=$encodedPath'\">")
            }
            sb.append("<span class=\"file-icon\">$icon</span>")
            sb.append("<span class=\"file-name\">${escapeHtml(f.name)}</span>")
            sb.append("<span class=\"file-size\">$sizeStr</span>")
            sb.append("<span class=\"file-date\">$dateStr</span>")
            sb.append("</div>")
        }

        sb.append("</div>") // file-list

        // Upload section
        val encodedCurrent = URLEncoder.encode(currentPath, "UTF-8")
        sb.append("<div class=\"upload-section\">")
        sb.append("<h3>📤 Upload File</h3>")
        sb.append("<form action=\"/upload?path=$encodedCurrent\" method=\"post\" enctype=\"application/x-www-form-urlencoded\">")
        sb.append("<input type=\"text\" name=\"filename\" placeholder=\"File name\">")
        sb.append("<textarea name=\"file\" placeholder=\"File content...\" style=\"height:80px;\"></textarea>")
        sb.append("<button type=\"submit\" class=\"upload-btn\">Upload</button>")
        sb.append("</form>")
        sb.append("</div>")

        sb.append("<div class=\"footer\">AppX • Open Source Android Toolkit</div>")
        sb.append("</div></body></html>")

        return sb.toString()
    }

    private fun buildBreadcrumb(path: String): String {
        if (path == "/" || path.isEmpty()) return ""
        val parts = path.trim('/').split('/')
        val sb = StringBuilder()
        var accumulated = ""
        for (part in parts) {
            accumulated += "/$part"
            val encoded = URLEncoder.encode(accumulated, "UTF-8")
            sb.append(" / <a href=\"/browse?path=$encoded\">${escapeHtml(part)}</a>")
        }
        return sb.toString()
    }

    private fun getFileIcon(ext: String): String = when (ext.lowercase()) {
        "apk" -> "📦"
        "jpg", "jpeg", "png", "gif", "bmp", "webp" -> "🖼️"
        "mp4", "avi", "mkv", "mov" -> "🎬"
        "mp3", "wav", "flac", "ogg" -> "🎵"
        "zip", "rar", "7z", "tar", "gz" -> "🗜️"
        "txt", "md" -> "📄"
        "kt", "java", "py", "js", "ts", "html", "css", "xml", "json" -> "💻"
        "pdf" -> "📕"
        else -> "📄"
    }

    private fun formatFileSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        if (bytes < 1024 * 1024) return "%.1f KB".format(bytes / 1024.0)
        if (bytes < 1024 * 1024 * 1024) return "%.1f MB".format(bytes / (1024.0 * 1024))
        return "%.1f GB".format(bytes / (1024.0 * 1024 * 1024))
    }

    private fun escapeHtml(text: String): String {
        return text.replace("&", "&amp;").replace("<", "&lt;")
            .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;")
    }

    private fun guessContentType(ext: String): String = when (ext.lowercase()) {
        "html", "htm" -> "text/html"
        "css" -> "text/css"
        "js" -> "application/javascript"
        "json" -> "application/json"
        "xml" -> "application/xml"
        "txt", "md" -> "text/plain"
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "gif" -> "image/gif"
        "webp" -> "image/webp"
        "svg" -> "image/svg+xml"
        "pdf" -> "application/pdf"
        "mp4" -> "video/mp4"
        "mp3" -> "audio/mpeg"
        "apk" -> "application/vnd.android.package-archive"
        "zip" -> "application/zip"
        else -> "application/octet-stream"
    }

    private fun extractQueryParam(query: String, key: String): String? {
        for (pair in query.split("&")) {
            val kv = pair.split("=", limit = 2)
            if (kv.size == 2 && kv[0] == key) {
                return URLDecoder.decode(kv[1], "UTF-8")
            }
        }
        return null
    }

    private fun parseFormData(body: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        for (pair in body.split("&")) {
            val kv = pair.split("=", limit = 2)
            if (kv.size == 2) {
                val key = URLDecoder.decode(kv[0], "UTF-8")
                val value = URLDecoder.decode(kv[1], "UTF-8")
                result[key] = value
            }
        }
        return result
    }

    private fun sendResponse(output: OutputStream, code: Int, response: String, contentType: String) {
        val bytes = response.toByteArray(Charsets.UTF_8)
        val sb = StringBuilder()
        sb.append("HTTP/1.1 $code ${statusText(code)}\r\n")
        sb.append("Content-Type: $contentType\r\n")
        sb.append("Content-Length: ${bytes.size}\r\n")
        sb.append("Connection: close\r\n")
        sb.append("\r\n")
        output.write(sb.toString().toByteArray(Charsets.UTF_8))
        output.write(bytes)
    }

    private fun statusText(code: Int): String = when (code) {
        200 -> "OK"
        302 -> "Found"
        400 -> "Bad Request"
        404 -> "Not Found"
        500 -> "Internal Server Error"
        else -> "Unknown"
    }
}
