package com.appdex.axmleditor

import com.appdex.apk.ApkFile
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AXML 编辑器 Repository: 解码(二进制→文本)和编码(文本→二进制)。
 *
 * 解码使用内置的 BinaryAxmlDecoder。
 * 编码使用内置的 AxmlEncoder 将文本 XML 转为二进制 AXML。
 */
@Singleton
class AxmlEditorRepository @Inject constructor() {

    fun decode(binary: ByteArray): String {
        val decoder = BinaryAxmlDecoder(binary)
        val xml = decoder.decode()
        return formatXml(xml)
    }

    fun decodeFromApk(apkPath: String, entryName: String): String {
        return ApkFile(apkPath).use { apk ->
            val bytes = apk.getEntryInputStream(entryName)?.readBytes()
                ?: throw IllegalArgumentException("Entry not found: $entryName")
            decode(bytes)
        }
    }

    fun encode(xmlText: String): ByteArray {
        return AxmlEncoder.encode(xmlText)
    }

    private fun formatXml(raw: String): String {
        val lines = raw.lines().filter { it.isNotBlank() }
        val sb = StringBuilder()
        var depth = 0
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("</")) depth--
            sb.append("  ".repeat(maxOf(0, depth))).append(trimmed).append("\n")
            if (trimmed.startsWith("<") && !trimmed.startsWith("</") &&
                !trimmed.startsWith("<?") && !trimmed.endsWith("/>") &&
                !trimmed.contains("</")
            ) depth++
        }
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n${sb}"
    }
}
