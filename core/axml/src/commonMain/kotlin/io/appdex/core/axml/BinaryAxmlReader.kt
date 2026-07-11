package io.appdex.core.axml

import io.appdex.core.axml.chunk.ChunkType
import io.appdex.core.axml.chunk.StringPool
import io.appdex.core.io.ByteReader
import io.appdex.core.io.InMemorySeekableChannel

/**
 * 二进制 AXML 读取实现。
 *
 * 解析:XML header → StringPool → XML 事件序列(StartTag/Text/EndTag)→ 文本 XML。
 */
@Suppress("MagicNumber", "UnusedPrivateProperty")
class BinaryAxmlReader : AxmlReader {

    override fun read(binary: ByteArray): AxmlDocument {
        val reader = ByteReader(InMemorySeekableChannel(binary))
        // 1. XML header
        val xmlType = reader.readUInt16LE()
        require(xmlType == ChunkType.XML) { "not an AXML file: 0x${xmlType.toString(16)}" }
        reader.skip(2) // headerSize
        val xmlChunkSize = reader.readUInt32LE()

        // 2. StringPool(第一个子 chunk)
        val stringPool = StringPool.parse(reader)

        // 3. 遍历后续 chunks
        val sb = StringBuilder()
        var depth = 0

        while (reader.position() < binary.size.toLong()) {
            val chunkStart = reader.position()
            val type = reader.readUInt16LE()
            val headerSize = reader.readUInt16LE()
            val chunkSize = reader.readUInt32LE()
            val chunkEnd = chunkStart + chunkSize

            when (type) {
                ChunkType.XML_START_TAG -> {
                    reader.skip(4) // lineNumber
                    reader.skip(4) // comment
                    reader.skip(4) // nsPrefix
                    val nameIdx = reader.readInt32LE()
                    reader.skip(2) // attrStart
                    reader.skip(2) // attrSize
                    val attrCount = reader.readUInt16LE()
                    reader.skip(6) // idIdx, classIdx, styleIdx

                    val name = stringPool.strings.getOrNull(nameIdx) ?: ""
                    sb.append("  ".repeat(depth)).append("<").append(name)

                    // 属性
                    for (i in 0 until attrCount) {
                        val nsIdx = reader.readInt32LE()
                        val attrNameIdx = reader.readInt32LE()
                        val rawValueIdx = reader.readInt32LE()
                        reader.skip(2) // typedValue size
                        reader.skip(1) // res0
                        val dataType = reader.readUInt8()
                        val data = reader.readInt32LE()
                        val attrName = stringPool.strings.getOrNull(attrNameIdx) ?: ""
                        // 简化:如果有 rawValue,用它;否则用 data
                        val value = if (rawValueIdx >= 0 && rawValueIdx < stringPool.strings.size) {
                            stringPool.strings[rawValueIdx]
                        } else {
                            data.toString()
                        }
                        if (nsIdx >= 0 && nsIdx < stringPool.strings.size) {
                            sb.append(" ").append(stringPool.strings[nsIdx]).append(":")
                        }
                        sb.append(attrName).append("=\"").append(value).append("\"")
                    }
                    sb.append(">\n")
                    depth++
                }
                ChunkType.XML_END_TAG -> {
                    reader.skip(4) // lineNumber
                    reader.skip(4) // comment
                    reader.skip(4) // nsPrefix
                    val nameIdx = reader.readInt32LE()
                    depth--
                    val name = stringPool.strings.getOrNull(nameIdx) ?: ""
                    sb.append("  ".repeat(depth)).append("</").append(name).append(">\n")
                }
                ChunkType.XML_TEXT -> {
                    reader.skip(4) // lineNumber
                    reader.skip(4) // comment
                    val textIdx = reader.readInt32LE()
                    if (textIdx >= 0 && textIdx < stringPool.strings.size) {
                        sb.append(stringPool.strings[textIdx])
                    }
                }
                ChunkType.RESOURCE_MAP -> {
                    // 跳过,不解析
                }
                else -> {
                    // 未知 chunk,跳过
                }
            }
            reader.position(chunkEnd)
        }

        return AxmlDocument(sb.toString().trim())
    }
}
