package com.appdex.hex

import java.io.File
import java.io.RandomAccessFile
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 十六进制数据行。
 */
data class HexRow(
    val offset: Long,
    val bytes: ByteArray,
) {
    val hexString: String
        get() = bytes.joinToString(" ") { "%02X".format(it) }

    val asciiString: String
        get() = bytes.joinToString("") {
            if (it in 32..126) it.toChar().toString() else "."
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HexRow) return false
        return offset == other.offset && bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int {
        var result = offset.hashCode()
        result = 31 * result + bytes.contentHashCode()
        return result
    }
}

/**
 * 十六进制编辑器仓库。
 *
 * Library 层 — 纯 JVM，不依赖 Android Framework。
 */
@Singleton
class HexRepository @Inject constructor() {

    companion object {
        const val BYTES_PER_ROW = 16
        const val MAX_FILE_SIZE = 10L * 1024 * 1024 // 10MB
    }

    /**
     * 读取文件全部字节。
     * @throws IllegalArgumentException 文件过大或不存在
     */
    fun readBytes(filePath: String): ByteArray {
        val file = File(filePath)
        if (!file.exists()) {
            throw IllegalArgumentException("文件不存在: $filePath")
        }
        if (file.length() > MAX_FILE_SIZE) {
            throw IllegalArgumentException("文件过大 (${file.length()} bytes)，最大支持 ${MAX_FILE_SIZE / 1024 / 1024}MB")
        }
        return file.readBytes()
    }

    /**
     * 将字节数组写入文件。
     */
    fun writeBytes(filePath: String, bytes: ByteArray) {
        val file = File(filePath)
        file.writeBytes(bytes)
    }

    /**
     * 获取文件大小。
     */
    fun getFileSize(filePath: String): Long {
        return File(filePath).length()
    }

    /**
     * 将字节数据转换为十六进制行列表。
     */
    fun toHexRows(bytes: ByteArray, startOffset: Long = 0): List<HexRow> {
        val rows = mutableListOf<HexRow>()
        var offset = startOffset
        var i = 0
        while (i < bytes.size) {
            val end = minOf(i + BYTES_PER_ROW, bytes.size)
            val rowBytes = bytes.copyOfRange(i, end)
            rows.add(HexRow(offset, rowBytes))
            offset += rowBytes.size
            i = end
        }
        return rows
    }

    /**
     * 在字节数组中搜索十六进制模式。
     * @return 匹配的偏移量列表
     */
    fun searchHex(bytes: ByteArray, pattern: ByteArray): List<Long> {
        val results = mutableListOf<Long>()
        if (pattern.isEmpty()) return results
        for (i in 0..bytes.size - pattern.size) {
            var match = true
            for (j in pattern.indices) {
                if (bytes[i + j] != pattern[j]) {
                    match = false
                    break
                }
            }
            if (match) {
                results.add(i.toLong())
            }
        }
        return results
    }

    /**
     * 在字节数组中搜索 ASCII 文本。
     */
    fun searchAscii(bytes: ByteArray, text: String): List<Long> {
        return searchHex(bytes, text.toByteArray(Charsets.UTF_8))
    }

    /**
     * 解析十六进制字符串为字节数组。
     * 支持 "48656C6C6F" 或 "48 65 6C 6C 6F" 格式。
     */
    fun parseHexString(hex: String): ByteArray {
        val cleaned = hex.replace(" ", "").replace("\n", "").replace("\t", "")
        if (cleaned.length % 2 != 0) {
            throw IllegalArgumentException("十六进制字符串长度必须为偶数")
        }
        return ByteArray(cleaned.length / 2) { i ->
            val high = cleaned[i * 2].digitToIntOrNull(16)
                ?: throw IllegalArgumentException("无效的十六进制字符: ${cleaned[i * 2]}")
            val low = cleaned[i * 2 + 1].digitToIntOrNull(16)
                ?: throw IllegalArgumentException("无效的十六进制字符: ${cleaned[i * 2 + 1]}")
            ((high shl 4) or low).toByte()
        }
    }
}
