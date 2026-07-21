package com.appdex.apk

import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Decodes Android binary XML (AXML) format to readable XML text.
 * Used for parsing AndroidManifest.xml and other binary XML resources in APK files.
 */
class BinaryXmlDecoder(private val data: ByteArray) {

    private val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
    private val stringPool = mutableListOf<String>()
    private val resourceIds = mutableListOf<Int>()

    fun decode(): String {
        if (data.size < 8) return ""

        val magic = buffer.short.toInt() and 0xFFFF
        if (magic != 0x0003) return ""

        buffer.position(0)
        val sb = StringBuilder()
        var depth = 0

        while (buffer.remaining() >= 8) {
            val chunkType = buffer.short.toInt() and 0xFFFF
            val headerSizeField = buffer.short.toInt() and 0xFFFF
            val chunkSize = buffer.int

            when (chunkType) {
                CHUNK_AXML_FILE -> {
                    // Skip header
                }
                CHUNK_STRING_POOL -> {
                    parseStringPool(chunkSize)
                }
                CHUNK_RESOURCE_IDS -> {
                    parseResourceIds(chunkSize)
                }
                CHUNK_XML_START_NAMESPACE -> {
                    buffer.position(buffer.position() + chunkSize - 8)
                }
                CHUNK_XML_END_NAMESPACE -> {
                    buffer.position(buffer.position() + chunkSize - 8)
                }
                CHUNK_XML_START_TAG -> {
                    parseStartTag(sb, depth)
                    depth++
                }
                CHUNK_XML_END_TAG -> {
                    depth--
                    sb.append("  ".repeat(depth.coerceAtLeast(0)))
                    sb.append("</")
                    val endNamePos = buffer.position()
                    buffer.int // lineNumber
                    buffer.int // comment
                    val nameIdx = buffer.int
                    val name = getString(nameIdx)
                    sb.append(name)
                    sb.append(">\n")
                    buffer.position(endNamePos + chunkSize - 8)
                }
                CHUNK_XML_CDATA -> {
                    buffer.position(buffer.position() + chunkSize - 8)
                }
                else -> {
                    if (chunkSize > 8) {
                        buffer.position(buffer.position() + chunkSize - 8)
                    }
                }
            }
        }

        return sb.toString().trim()
    }

    private fun parseStringPool(chunkSize: Int) {
        val startPos = buffer.position() - 8
        val stringCount = buffer.int
        val styleCount = buffer.int
        val flags = buffer.int
        val stringsStart = buffer.int
        val stylesStart = buffer.int

        val isUTF8 = (flags and FLAG_UTF8) != 0

        // Read string offsets
        val offsets = IntArray(stringCount)
        for (i in 0 until stringCount) {
            offsets[i] = buffer.int
        }

        // Read strings
        val stringsBaseOffset = startPos + stringsStart
        stringPool.clear()

        for (i in 0 until stringCount) {
            val offset = stringsBaseOffset + offsets[i]
            buffer.position(offset)

            if (isUTF8) {
                // UTF-8: first byte is length (or 2 bytes if high bit set)
                var len = buffer.get().toInt() and 0xFF
                if (len and 0x80 != 0) {
                    len = ((len and 0x7F) shl 8) or (buffer.get().toInt() and 0xFF)
                }
                // Skip UTF-8 encoded length (same encoding)
                var encodedLen = buffer.get().toInt() and 0xFF
                if (encodedLen and 0x80 != 0) {
                    encodedLen = ((encodedLen and 0x7F) shl 8) or (buffer.get().toInt() and 0xFF)
                }
                val bytes = ByteArray(encodedLen)
                buffer.get(bytes)
                stringPool.add(String(bytes, Charsets.UTF_8))
            } else {
                // UTF-16: first 2 bytes are length
                var len = buffer.short.toInt() and 0xFFFF
                if (len and 0x8000 != 0) {
                    len = ((len and 0x7FFF) shl 16) or (buffer.short.toInt() and 0xFFFF)
                }
                val bytes = ByteArray(len * 2)
                buffer.get(bytes)
                stringPool.add(String(bytes, Charsets.UTF_16LE))
            }
        }

        // Move past this chunk
        buffer.position(startPos + chunkSize)
    }

    private fun parseResourceIds(chunkSize: Int) {
        val startPos = buffer.position() - 8
        val count = (chunkSize - 8) / 4
        resourceIds.clear()
        for (i in 0 until count) {
            resourceIds.add(buffer.int)
        }
        buffer.position(startPos + chunkSize)
    }

    private fun parseStartTag(sb: StringBuilder, depth: Int) {
        val startPos = buffer.position()

        buffer.int // lineNumber
        buffer.int // comment

        val nsIdx = buffer.int
        val nameIdx = buffer.int

        buffer.short // attrStart
        buffer.short // attrSize
        val attrCount = buffer.short.toInt() and 0xFFFF
        buffer.short // idIdx
        buffer.short // classIdx
        buffer.short // styleIdx

        val name = getString(nameIdx)

        sb.append("  ".repeat(depth))
        sb.append("<").append(name)

        for (i in 0 until attrCount) {
            val attrNsIdx = buffer.int
            val attrNameIdx = buffer.int
            val attrRawValueIdx = buffer.int
            buffer.short // typedValue size (always 8)
            buffer.get() // res0 (always 0)
            val attrType = buffer.get().toInt() and 0xFF
            val attrData = buffer.int

            val attrName = getString(attrNameIdx)
            val attrNs = getString(attrNsIdx)

            val value = formatAttributeValue(attrType, attrData, attrRawValueIdx)

            val fullAttrName = if (attrNs.isNotEmpty()) {
                if (attrNs == "android" || attrNs.contains("schemas.android.com")) {
                    "android:$attrName"
                } else {
                    "$attrNs:$attrName"
                }
            } else {
                attrName
            }

            sb.append(" ")
            sb.append(fullAttrName)
            sb.append("=\"")
            sb.append(escapeXml(value))
            sb.append("\"")
        }

        sb.append(">\n")

        // Move to end of this chunk
        val headerSize = 4 + 4 + 4 + 4 + 2 + 2 + 2 + 2 + 2 + 2 // = 28
        val attrsSize = attrCount * 20 // each attr is 20 bytes
        buffer.position(startPos + headerSize + attrsSize)
    }

    private fun formatAttributeValue(type: Int, data: Int, rawValueIdx: Int): String {
        // Check if there's a raw value
        if (rawValueIdx >= 0 && rawValueIdx < stringPool.size) {
            val raw = stringPool[rawValueIdx]
            if (raw.isNotEmpty()) return raw
        }

        val actualType = type
        return when (actualType) {
            TYPE_STRING -> getString(data)
            TYPE_INT_DEC -> data.toString()
            TYPE_INT_HEX -> "0x${data.toLong().and(0xFFFFFFFFL).toString(16)}"
            TYPE_INT_BOOLEAN -> if (data != 0) "true" else "false"
            TYPE_REFERENCE -> "@${if (data < 0) "android:" else ""}0x${data.toLong().and(0xFFFFFFFFL).toString(16)}"
            TYPE_ATTRIBUTE -> "?0x${data.toLong().and(0xFFFFFFFFL).toString(16)}"
            TYPE_FLOAT -> java.lang.Float.intBitsToFloat(data).toString()
            TYPE_DIMENSION -> formatDimension(data)
            TYPE_FRACTION -> formatFraction(data)
            else -> "0x${data.toLong().and(0xFFFFFFFFL).toString(16)}"
        }
    }

    private fun formatDimension(data: Int): String {
        val value = data shr 8
        val unit = data and 0xFF
        val unitStr = when (unit and 0x0F) {
            COMPLEX_UNIT_PX -> "px"
            COMPLEX_UNIT_DIP -> "dp"
            COMPLEX_UNIT_SP -> "sp"
            COMPLEX_UNIT_PT -> "pt"
            COMPLEX_UNIT_IN -> "in"
            COMPLEX_UNIT_MM -> "mm"
            else -> ""
        }
        return "$value$unitStr"
    }

    private fun formatFraction(data: Int): String {
        val value = (data shr 4) and 0x0FFFFFFF
        val type = data and 0x0F
        val typeStr = if (type and 1 == 0) "%" else "%p"
        return "${value / 1000.0}$typeStr"
    }

    private fun getString(index: Int): String {
        if (index < 0 || index >= stringPool.size) return ""
        return stringPool[index]
    }

    private fun escapeXml(s: String): String {
        return s
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    companion object {
        private const val CHUNK_AXML_FILE = 0x0003
        private const val CHUNK_STRING_POOL = 0x0001
        private const val CHUNK_RESOURCE_IDS = 0x0180
        private const val CHUNK_XML_START_NAMESPACE = 0x0100
        private const val CHUNK_XML_END_NAMESPACE = 0x0101
        private const val CHUNK_XML_START_TAG = 0x0102
        private const val CHUNK_XML_END_TAG = 0x0103
        private const val CHUNK_XML_CDATA = 0x0104

        private const val FLAG_UTF8 = 0x00000100

        private const val TYPE_STRING = 0x03
        private const val TYPE_INT_DEC = 0x10
        private const val TYPE_INT_HEX = 0x11
        private const val TYPE_INT_BOOLEAN = 0x12
        private const val TYPE_REFERENCE = 0x01
        private const val TYPE_ATTRIBUTE = 0x02
        private const val TYPE_FLOAT = 0x04
        private const val TYPE_DIMENSION = 0x05
        private const val TYPE_FRACTION = 0x06

        private const val COMPLEX_UNIT_PX = 0
        private const val COMPLEX_UNIT_DIP = 1
        private const val COMPLEX_UNIT_SP = 2
        private const val COMPLEX_UNIT_PT = 3
        private const val COMPLEX_UNIT_IN = 4
        private const val COMPLEX_UNIT_MM = 5
    }
}
