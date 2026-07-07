package io.appdex.core.arsc

import io.appdex.core.arsc.chunk.TableChunkType
import io.appdex.core.io.ByteReader
import io.appdex.core.io.InMemorySeekableChannel

class BinaryArscReader : ArscReader {

    override fun read(binary: ByteArray): ArscTable {
        val reader = ByteReader(InMemorySeekableChannel(binary))
        // TABLE header
        val type = reader.readUInt16LE()
        require(type == TableChunkType.TABLE) { "not an ARSC: 0x${type.toString(16)}" }
        reader.skip(2) // headerSize
        reader.readUInt32LE() // chunkSize
        val packageCount = reader.readUInt32LE()

        // 跳过全局 StringPool(本 MVP 不用它)
        val spType = reader.readUInt16LE()
        if (spType == TableChunkType.STRING_POOL) {
            reader.skip(2) // headerSize
            val spChunkSize = reader.readUInt32LE()
            reader.skip(spChunkSize - 8) // 跳到 chunk 结束
        }

        // 解析 packages
        val packages = mutableListOf<ArscPackage>()
        repeat(packageCount.toInt()) {
            val pkg = parsePackage(reader)
            if (pkg != null) packages.add(pkg)
        }
        return ArscTable(packages)
    }

    private fun parsePackage(reader: ByteReader): ArscPackage? {
        val chunkStart = reader.position()
        val type = reader.readUInt16LE()
        if (type != TableChunkType.PACKAGE) {
            // 跳过未知 chunk
            reader.skip(2)
            val sz = reader.readUInt32LE()
            reader.position(chunkStart + sz)
            return null
        }
        reader.skip(2) // headerSize
        reader.readUInt32LE() // chunkSize
        val id = reader.readInt32LE()
        val nameBytes = reader.readBytes(256)
        val name = nameBytes.toString(Charsets.UTF_16LE).trimEnd('\u0000')
        return ArscPackage(id, name)
    }
}
