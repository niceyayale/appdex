package com.appdex.axmleditor

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * AXML 编码器:将文本 XML 编码为 Android 二进制 AXML 格式。
 *
 * 二进制 AXML 结构:
 * 1. XML header (type=0x0003, headerSize=8, chunkSize=total)
 * 2. StringPool (type=0x0001)
 * 3. XML events: START_TAG(0x0102) / END_TAG(0x0103) / TEXT(0x0104)
 *
 * 每个 XML event chunk header:
 * - type(2) + headerSize(2) + chunkSize(4)
 * - lineNumber(4) + comment(4)
 *
 * START_TAG 额外字段:
 * - nsPrefix(4) + nameIdx(4) + attrStart(2) + attrSize(2) + attrCount(2)
 * - idIdx(2) + classIdx(2) + styleIdx(2) → 共 20 字节 header extension
 * - 每个 attribute: ns(4) + name(4) + rawValue(4) + typedSize(2) + res0(1) + dataType(1) + data(4) = 20 bytes
 */
@Suppress("MagicNumber")
object AxmlEncoder {

    // ── Res_value types ──
    private const val TYPE_NULL = 0x00
    private const val TYPE_STRING = 0x03
    private const val TYPE_INT_DEC = 0x10
    private const val TYPE_INT_HEX = 0x11
    private const val TYPE_INT_BOOL = 0x12
    private const val TYPE_REFERENCE = 0x01

    fun encode(xmlText: String): ByteArray {
        val parser = SimpleXmlParser(xmlText)
        val elements = parser.parse()

        // 1. 收集所有字符串
        val stringSet = mutableSetOf("")
        for (elem in elements) {
            stringSet.add(elem.name)
            for (attr in elem.attributes) {
                if (attr.namespace.isNotEmpty()) stringSet.add(attr.namespace)
                stringSet.add(attr.name)
                if (attr.value.isNotEmpty()) stringSet.add(attr.value)
            }
            if (elem.text.isNotEmpty()) stringSet.add(elem.text)
        }
        val strings = stringSet.toList()
        val stringIndices = strings.withIndex().associate { it.value to it.index }

        // 2. 构建 StringPool
        val stringPoolBytes = buildStringPool(strings)

        // 3. 构建 XML events
        val eventBuffer = ByteBuffer.allocate(256 * 1024).order(ByteOrder.LITTLE_ENDIAN)
        var lineNumber = 1

        for (elem in elements) {
            // START_TAG
            val nameIdx = stringIndices[elem.name] ?: 0
            val attrCount = elem.attributes.size
            val startTagSize = 36 + attrCount * 20 // header(8) + ext(28) + attrs

            eventBuffer.putShort(0x0102.toShort()) // type = XML_START_TAG
            eventBuffer.putShort(36)      // headerSize = 8 + 28
            eventBuffer.putInt(startTagSize)
            eventBuffer.putInt(lineNumber) // lineNumber
            eventBuffer.putInt(-1)         // comment (-1 = none)
            eventBuffer.putInt(-1)         // nsPrefix
            eventBuffer.putInt(nameIdx)    // nameIdx
            eventBuffer.putShort(0x0014.toShort())   // attrStart = 20 (offset from start of this chunk's header to attrs)
            eventBuffer.putShort(0x0014.toShort())   // attrSize = 20
            eventBuffer.putShort(attrCount.toShort()) // attrCount
            eventBuffer.putShort(0.toShort())        // idIdx
            eventBuffer.putShort(0.toShort())        // classIdx
            eventBuffer.putShort(0.toShort())        // styleIdx

            // attributes
            for (attr in elem.attributes) {
                val nsIdx = if (attr.namespace.isNotEmpty()) stringIndices[attr.namespace] ?: -1 else -1
                val attrNameIdx = stringIndices[attr.name] ?: 0
                val rawValueIdx = if (attr.value.isNotEmpty()) stringIndices[attr.value] ?: -1 else -1

                eventBuffer.putInt(nsIdx)
                eventBuffer.putInt(attrNameIdx)
                eventBuffer.putInt(rawValueIdx)
                eventBuffer.putShort(8.toShort()) // typedValue size
                eventBuffer.put(0)      // res0

                val (dataType, data) = resolveTypeAndData(attr.value)
                eventBuffer.put(dataType.toByte())
                eventBuffer.putInt(data)
            }

            // TEXT (if any)
            if (elem.text.isNotEmpty()) {
                val textIdx = stringIndices[elem.text] ?: 0
                eventBuffer.putShort(0x0104.toShort()) // type = XML_TEXT
                eventBuffer.putShort(16.toShort())     // headerSize
                eventBuffer.putInt(16 + 4)   // chunkSize (header + textIdx)
                eventBuffer.putInt(lineNumber)
                eventBuffer.putInt(-1)       // comment
                eventBuffer.putInt(textIdx)
            }

            lineNumber++

            // END_TAG
            eventBuffer.putShort(0x0103.toShort()) // type = XML_END_TAG
            eventBuffer.putShort(16.toShort())     // headerSize
            eventBuffer.putInt(16)       // chunkSize
            eventBuffer.putInt(lineNumber)
            eventBuffer.putInt(-1)       // comment
            eventBuffer.putInt(-1)       // nsPrefix
            eventBuffer.putInt(nameIdx)  // nameIdx
        }

        val eventBytes = ByteArray(eventBuffer.position())
        eventBuffer.rewind()
        eventBuffer.get(eventBytes)

        // 4. XML header
        val totalSize = 8 + stringPoolBytes.size + eventBytes.size
        val header = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
        header.putShort(0x0003.toShort()) // type = XML
        header.putShort(8.toShort())      // headerSize
        header.putInt(totalSize)

        return header.array() + stringPoolBytes + eventBytes
    }

    private fun buildStringPool(strings: List<String>): ByteArray {
        val isUtf8 = true
        val flags = if (isUtf8) 0x100 else 0

        // offsets from stringsStart
        val offsets = IntArray(strings.size)
        val stringsData = ByteBuffer.allocate(strings.sumOf { it.toByteArray(Charsets.UTF_8).size + 5 }).order(ByteOrder.LITTLE_ENDIAN)
        var currentOffset = 0
        for (i in strings.indices) {
            offsets[i] = currentOffset
            val bytes = strings[i].toByteArray(Charsets.UTF_8)
            if (isUtf8) {
                stringsData.put(encodeUtf8Len(bytes.size)) // utf16 len (simplified)
                stringsData.put(encodeUtf8Len(bytes.size)) // utf8 len
                stringsData.put(bytes)
                stringsData.put(0) // null terminator
                currentOffset += bytes.size + 3
            } else {
                stringsData.putShort(bytes.size.toShort())
                stringsData.put(bytes)
                stringsData.putShort(0)
                currentOffset += bytes.size + 4
            }
        }

        val stringsEnd = stringsData.position()
        val stringsStart = 28 // header is 28 bytes

        val header = ByteBuffer.allocate(stringsStart).order(ByteOrder.LITTLE_ENDIAN)
        header.putShort(0x0001.toShort()) // type = STRING_POOL
        header.putShort(stringsStart.toShort()) // headerSize
        header.putInt(stringsStart + stringsEnd) // chunkSize
        header.putInt(strings.size) // stringCount
        header.putInt(0) // styleCount
        header.putInt(flags)
        header.putInt(stringsStart) // stringsStart
        header.putInt(0) // stylesStart

        val result = ByteArray(stringsStart + stringsEnd)
        System.arraycopy(header.array(), 0, result, 0, stringsStart)
        stringsData.rewind()
        stringsData.get(result, stringsStart, stringsEnd)

        return result
    }

    private fun encodeUtf8Len(len: Int): Byte {
        return if (len < 128) len.toByte() else ((len shr 8) or 0x80).toByte()
    }

    private fun resolveTypeAndData(value: String): Pair<Int, Int> {
        // Boolean
        if (value == "true") return TYPE_INT_BOOL to 1
        if (value == "false") return TYPE_INT_BOOL to 0

        // Reference @id/ @+id/
        if (value.startsWith("@") || value.startsWith("@+")) return TYPE_REFERENCE to 0

        // Hex int 0x...
        if (value.startsWith("0x") || value.startsWith("0X")) {
            val parsed = value.removePrefix("0x").removePrefix("0X").toIntOrNull(16)
            if (parsed != null) return TYPE_INT_HEX to parsed
        }

        // Decimal int
        value.toIntOrNull()?.let { return TYPE_INT_DEC to it }

        // Default: string
        return TYPE_STRING to 0
    }

    // ── Simple XML Parser ──
    private data class XmlElement(
        val name: String,
        val attributes: List<XmlAttribute>,
        val text: String,
    )

    private data class XmlAttribute(
        val namespace: String,
        val name: String,
        val value: String,
    )

    private class SimpleXmlParser(private val xml: String) {
        fun parse(): List<XmlElement> {
            val elements = mutableListOf<XmlElement>()
            val tags = extractTags()
            for (tag in tags) {
                if (tag.isClosing) continue
                val name = extractTagName(tag.content)
                val attrs = extractAttributes(tag.content)
                val text = tag.text
                elements.add(XmlElement(name, attrs, text))
            }
            return elements
        }

        private data class TagInfo(
            val content: String,
            val isClosing: Boolean,
            val text: String,
        )

        private fun extractTags(): List<TagInfo> {
            val tags = mutableListOf<TagInfo>()
            var i = 0
            while (i < xml.length) {
                val start = xml.indexOf('<', i)
                if (start == -1) break
                val end = xml.indexOf('>', start)
                if (end == -1) break

                val content = xml.substring(start + 1, end)
                val isClosing = content.startsWith("/")
                val isSelfClosing = content.endsWith("/")

                val text = if (!isClosing && !isSelfClosing) {
                    val nextTag = xml.indexOf('<', end + 1)
                    if (nextTag != -1 && nextTag > end + 1) {
                        xml.substring(end + 1, nextTag).trim()
                    } else ""
                } else ""

                tags.add(TagInfo(content, isClosing, text))
                i = end + 1
            }
            return tags
        }

        private fun extractTagName(content: String): String {
            var s = content
            if (s.startsWith("/")) s = s.substring(1)
            if (s.endsWith("/")) s = s.dropLast(1)
            val spaceIdx = s.indexOf(' ')
            return if (spaceIdx != -1) s.substring(0, spaceIdx) else s
        }

        private fun extractAttributes(content: String): List<XmlAttribute> {
            val attrs = mutableListOf<XmlAttribute>()
            val regex = Regex("""(\S+?):(\S+?)="([^"]*)"""")
            for (match in regex.findAll(content)) {
                attrs.add(XmlAttribute(match.groupValues[1], match.groupValues[2], match.groupValues[3]))
            }
            // Also match attributes without namespace
            val regexNoNs = Regex("""\s(\w+)="([^"]*)"""")
            for (match in regexNoNs.findAll(content)) {
                val name = match.groupValues[1]
                if (attrs.none { it.name == name }) {
                    attrs.add(XmlAttribute("", name, match.groupValues[2]))
                }
            }
            return attrs
        }
    }
}
