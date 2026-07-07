package io.appdex.core.apk.zip

/**
 * 测试工具:构造最小 ZIP 字节流。
 *
 * ZIP 结构:
 * [Local File Header 1][data 1][Local File Header 2][data 2]
 * [Central Directory Entry 1][Central Directory Entry 2]
 * [EOCD]
 *
 * 这里用 STORED(method=0,不压缩)以简化测试。
 */
object ZipSample {

    /**
     * 构造一个含 N 个 STORED 条目的 ZIP。
     * @param entries (name, content) 对
     */
    fun buildWithStoredEntries(vararg entries: Pair<String, ByteArray>): ByteArray {
        val dataBlocks = mutableListOf<ByteArray>()
        val centralEntries = mutableListOf<ByteArray>()
        var offset = 0

        for ((name, content) in entries) {
            val nameBytes = name.toByteArray(Charsets.UTF_8)
            val crc = crc32(content)

            // Local File Header + data
            val local = buildLocalHeader(nameBytes, content, crc)
            dataBlocks.add(local + content)

            // Central Directory Entry
            val central = buildCentralEntry(nameBytes, content, crc, offset)
            centralEntries.add(central)

            offset += local.size + content.size
        }

        val cdStart = offset
        var cdSize = 0
        centralEntries.forEach { cdSize += it.size }

        val eocd = buildEocd(entries.size, cdSize, cdStart)
        val allData = if (dataBlocks.isEmpty()) ByteArray(0) else dataBlocks.reduce { acc, b -> acc + b }
        val allCd = if (centralEntries.isEmpty()) ByteArray(0) else centralEntries.reduce { acc, b -> acc + b }
        return allData + allCd + eocd
    }

    private fun buildLocalHeader(name: ByteArray, content: ByteArray, crc: Long): ByteArray {
        val header = ByteArray(30 + name.size)
        // signature 0x04034b50
        header[0] = 0x50; header[1] = 0x4b; header[2] = 0x03; header[3] = 0x04
        // version needed = 20
        le16(header, 4, 20)
        // flags = 0
        le16(header, 6, 0)
        // compression method = 0 (stored)
        le16(header, 8, 0)
        // mod time / date = 0
        // crc32
        le32(header, 14, crc)
        // compressed size
        le32(header, 18, content.size.toLong())
        // uncompressed size
        le32(header, 22, content.size.toLong())
        // name length
        le16(header, 26, name.size)
        // extra length = 0
        le16(header, 28, 0)
        // name
        name.copyInto(header, 30)
        return header
    }

    private fun buildCentralEntry(name: ByteArray, content: ByteArray, crc: Long, localHeaderOffset: Int): ByteArray {
        val entry = ByteArray(46 + name.size)
        // signature 0x02014b50
        entry[0] = 0x50; entry[1] = 0x4b; entry[2] = 0x01; entry[3] = 0x02
        // version made by = 20
        le16(entry, 4, 20)
        // version needed = 20
        le16(entry, 6, 20)
        // flags = 0
        le16(entry, 8, 0)
        // method = 0
        le16(entry, 10, 0)
        // crc32
        le32(entry, 16, crc)
        // compressed size
        le32(entry, 20, content.size.toLong())
        // uncompressed size
        le32(entry, 24, content.size.toLong())
        // name length
        le16(entry, 28, name.size)
        // extra length = 0
        le16(entry, 30, 0)
        // comment length = 0
        le16(entry, 32, 0)
        // disk number = 0
        // internal attrs = 0
        // external attrs = 0
        // local header offset
        le32(entry, 42, localHeaderOffset.toLong())
        // name
        name.copyInto(entry, 46)
        return entry
    }

    private fun buildEocd(entryCount: Int, cdSize: Int, cdOffset: Int): ByteArray {
        val eocd = ByteArray(22)
        // signature 0x06054b50
        eocd[0] = 0x50; eocd[1] = 0x4b; eocd[2] = 0x05; eocd[3] = 0x06
        // disk number = 0
        // CD disk = 0
        // CD entries on this disk
        le16(eocd, 8, entryCount)
        // total CD entries
        le16(eocd, 10, entryCount)
        // CD size
        le32(eocd, 12, cdSize.toLong())
        // CD offset
        le32(eocd, 16, cdOffset.toLong())
        // comment length = 0
        return eocd
    }

    private fun le16(buf: ByteArray, off: Int, v: Int) {
        buf[off] = v.toByte()
        buf[off + 1] = (v shr 8).toByte()
    }

    private fun le32(buf: ByteArray, off: Int, v: Long) {
        buf[off] = v.toByte()
        buf[off + 1] = (v shr 8).toByte()
        buf[off + 2] = (v shr 16).toByte()
        buf[off + 3] = (v shr 24).toByte()
    }

    @Suppress("TooGenericExceptionCaught")
    private fun crc32(data: ByteArray): Long {
        val crc = java.util.zip.CRC32()
        crc.update(data)
        return crc.value
    }
}
