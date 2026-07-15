package com.appdex.axmleditor

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * 二进制 AXML 解码器 — 自包含实现。
 *
 * 解析: XML header → StringPool → XML 事件序列(StartTag/Text/EndTag) → 文本 XML。
 */
@Suppress("MagicNumber")
class BinaryAxmlDecoder(private val data: ByteArray) {

    private val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)

    fun decode(): String {
        // 1. XML header
        val xmlType = readUShort(0)
        require(xmlType == 0x0003) { "Not an AXML file: 0x${xmlType.toString(16)}" }
        val headerSize = readUShort(2)

        // 2. StringPool (starts right after header)
        val strings = parseStringPool(headerSize)

        // 3. Iterate chunks after StringPool
        val sb = StringBuilder()
        var depth = 0
        var offset = findChunkAfterStringPool(headerSize, strings)

        while (offset < data.size) {
            val type = readUShort(offset)
            val chunkHeaderSize = readUShort(offset + 2)
            val chunkSize = readUInt(offset + 4).toInt()

            when (type) {
                0x0102 -> { // XML_START_TAG
                    val nameIdx = readInt(offset + chunkHeaderSize + 4)
                    val attrCount = readUShort(offset + chunkHeaderSize + 10)
                    val attrStart = readUShort(offset + chunkHeaderSize + 8).toInt()

                    val name = strings.getOrNull(nameIdx) ?: ""
                    sb.append("  ".repeat(depth)).append("<").append(name)

                    // Attributes
                    var attrOff = offset + chunkHeaderSize + attrStart
                    for (i in 0 until attrCount) {
                        val nsIdx = readInt(attrOff)
                        val attrNameIdx = readInt(attrOff + 4)
                        val rawValueIdx = readInt(attrOff + 8)
                        val dataType = data[attrOff + 15].toInt() and 0xFF
                        val dataVal = readInt(attrOff + 16)

                        val attrName = strings.getOrNull(attrNameIdx) ?: ""
                        val value = if (rawValueIdx >= 0 && rawValueIdx < strings.size) {
                            strings[rawValueIdx]
                        } else {
                            resolveTypedValue(dataType, dataVal)
                        }

                        if (nsIdx >= 0 && nsIdx < strings.size) {
                            val ns = strings[nsIdx]
                            // Simplify namespace: show prefix only
                            val prefix = ns.substringAfterLast("/").substringAfterLast(":")
                            sb.append(" ").append(prefix).append(":")
                        }
                        sb.append(attrName).append("=\"").append(value).append("\"")

                        attrOff += 20
                    }
                    sb.append(">\n")
                    depth++
                }
                0x0103 -> { // XML_END_TAG
                    val nameIdx = readInt(offset + chunkHeaderSize + 4)
                    depth--
                    val name = strings.getOrNull(nameIdx) ?: ""
                    sb.append("  ".repeat(maxOf(0, depth))).append("</").append(name).append(">\n")
                }
                0x0104 -> { // XML_TEXT
                    val textIdx = readInt(offset + chunkHeaderSize)
                    if (textIdx >= 0 && textIdx < strings.size) {
                        sb.append(strings[textIdx])
                    }
                }
            }

            if (chunkSize <= 0) break
            offset += chunkSize
        }

        return sb.toString().trim()
    }

    private fun resolveTypedValue(dataType: Int, data: Int): String {
        return when (dataType) {
            0x10 -> data.toString() // INT_DEC
            0x11 -> "0x${data.toString(16)}" // INT_HEX
            0x12 -> if (data != 0) "true" else "false" // INT_BOOL
            0x01 -> "@0x${data.toString(16)}" // REFERENCE
            else -> data.toString()
        }
    }

    // ── StringPool Parser ──
    private fun parseStringPool(baseOffset: Int): List<String> {
        val type = readUShort(baseOffset)
        if (type != 0x0001) return emptyList()

        val headerSize = readUShort(baseOffset + 2)
        val stringCount = readUInt(baseOffset + 8).toInt()
        val flags = readUInt(baseOffset + 16)
        val stringsStart = readUInt(baseOffset + 20).toInt()

        val isUtf8 = (flags and 0x100L) != 0L
        val strings = mutableListOf<String>()

        for (i in 0 until stringCount) {
            val strOffset = readUInt(baseOffset + headerSize + i * 4).toInt()
            val strPos = baseOffset + stringsStart + strOffset
            if (strPos >= data.size) {
                strings.add("")
                continue
            }

            val s = if (isUtf8) readUtf8String(strPos) else readUtf16String(strPos)
            strings.add(s)
        }

        return strings
    }

    private fun findChunkAfterStringPool(baseOffset: Int, strings: List<String>): Int {
        val type = readUShort(baseOffset)
        if (type != 0x0001) return baseOffset
        val chunkSize = readUInt(baseOffset + 4).toInt()
        return baseOffset + chunkSize
    }

    private fun readUtf8String(pos: Int): String {
        var p = pos
        // Skip UTF-16 length (1 or 2 bytes)
        val len1 = data[p++].toInt() and 0xFF
        if (len1 and 0x80 != 0) p++ // 2-byte length

        // UTF-8 length (1 or 2 bytes)
        val uLen = data[p++].toInt() and 0xFF
        val uLen2 = if (uLen and 0x80 != 0) {
            val u2 = data[p++].toInt() and 0xFF
            ((uLen and 0x7F) shl 8) or u2
        } else uLen

        val bytes = ByteArray(uLen2)
        val copyLen = minOf(uLen2, data.size - p)
        if (copyLen > 0) System.arraycopy(data, p, bytes, 0, copyLen)
        return String(bytes, Charsets.UTF_8)
    }

    private fun readUtf16String(pos: Int): String {
        val len = readUShort(pos)
        val byteLen = len * 2
        if (pos + 2 + byteLen > data.size) return ""
        val bytes = ByteArray(byteLen)
        System.arraycopy(data, pos + 2, bytes, 0, byteLen)
        return String(bytes, Charsets.UTF_16LE)
    }

    // ── Binary helpers ──
    private fun readUShort(offset: Int): Int {
        if (offset + 1 >= data.size) return 0
        return (data[offset].toInt() and 0xFF) or ((data[offset + 1].toInt() and 0xFF) shl 8)
    }

    private fun readUInt(offset: Int): Long {
        if (offset + 3 >= data.size) return 0
        return (data[offset].toLong() and 0xFF) or
            ((data[offset + 1].toLong() and 0xFF) shl 8) or
            ((data[offset + 2].toLong() and 0xFF) shl 16) or
            ((data[offset + 3].toLong() and 0xFF) shl 24)
    }

    private fun readInt(offset: Int): Int = readUInt(offset).toInt()
}
