package io.appdex.core.axml.chunk

import io.appdex.core.io.ByteReader

/**
 * AXML StringPool。
 */
class StringPool(
    val strings: List<String>,
) {
    companion object {
        /**
         * 从 chunk 起始位置解析 StringPool。
         * 调用前 reader 应定位到 type 字段处。
         *
         * chunk 头格式(共 28 字节,与 Android ResStringPool_header 一致):
         * type(2) + headerSize(2) + chunkSize(4)
         * + stringCount(4) + styleCount(4) + flags(4)
         * + stringsStart(4) + stylesStart(4)
         *
         * 注:stringCount/styleCount/flags 均为 uint32(4 字节),
         * 不是 uint16。flags 含 0x100 表示 UTF-8 编码。
         */
        fun parse(reader: ByteReader): StringPool {
            val startPos = reader.position()
            val type = reader.readUInt16LE()
            require(type == ChunkType.STRING_POOL) { "not a string pool: 0x${type.toString(16)}" }
            val headerSize = reader.readUInt16LE()
            val chunkSize = reader.readUInt32LE()
            val stringCount = reader.readUInt32LE().toInt()
            val styleCount = reader.readUInt32LE().toInt()
            val flags = reader.readUInt32LE()
            val stringsStart = reader.readUInt32LE().toInt()
            val stylesStart = reader.readUInt32LE()

            val isUtf8 = (flags and 0x100L) != 0L

            // 读 offsets
            val offsets = IntArray(stringCount)
            for (i in 0 until stringCount) {
                offsets[i] = reader.readInt32LE()
            }

            // 读 strings
            val strings = mutableListOf<String>()
            for (i in 0 until stringCount) {
                val strStart = startPos + stringsStart + offsets[i]
                reader.position(strStart.toLong())
                val s = if (isUtf8) readUtf8String(reader) else readUtf16String(reader)
                strings.add(s)
            }
            return StringPool(strings)
        }

        private fun readUtf8String(reader: ByteReader): String {
            // UTF-8: 两个长度(utf16Count, utf8Count),各 1 或 2 字节
            // 简化:假设长度 < 128(1 字节)
            val utf16Len = reader.readUInt8()
            val utf8Len = reader.readUInt8()
            val bytes = reader.readBytes(utf8Len)
            reader.readUInt8() // null terminator
            return String(bytes, Charsets.UTF_8)
        }

        private fun readUtf16String(reader: ByteReader): String {
            // UTF-16: uint16 长度(字符数),null terminator
            val len = reader.readUInt16LE()
            val bytes = reader.readBytes(len * 2)
            reader.readUInt16LE() // null terminator
            return String(bytes, Charsets.UTF_16LE)
        }
    }
}
