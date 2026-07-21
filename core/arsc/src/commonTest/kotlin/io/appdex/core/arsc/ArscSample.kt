package io.appdex.core.arsc

/**
 * 测试工具:构造最小二进制 ARSC。
 * 结构:[TABLE header][global StringPool][PACKAGE chunk]
 */
object ArscSample {

    fun buildMinimal(): ByteArray {
        val globalPool = buildStringPool(listOf("res"))
        val packageChunk = buildPackage(id = 1, name = "io.appdex.test")

        val tableHeaderSize = 12 // type(2) + headerSize(2) + chunkSize(4) + packageCount(4)
        val chunkSize = tableHeaderSize + globalPool.size + packageChunk.size

        val buf = mutableListOf<Byte>()
        le16(buf, 0x0002) // type TABLE
        le16(buf, 12)     // headerSize
        le32(buf, chunkSize.toLong())
        le32(buf, 1) // packageCount
        buf.addAll(globalPool.toList())
        buf.addAll(packageChunk.toList())
        return buf.toByteArray()
    }

    private fun buildPackage(id: Int, name: String): ByteArray {
        val nameBytes = name.toByteArray(Charsets.UTF_16LE).copyOf(256) // 固定 128 chars = 256 bytes
        // package chunk header: type(2)+headerSize(2)+chunkSize(4)+id(4)+name(256)+typeStrings(4)+lastPublicType(4)+keyStrings(4)+lastPublicKey(4) = 268 + header = 284
        val headerSize = 268
        // 简化:只含 header,无子 chunk
        val chunkSize = headerSize
        val buf = mutableListOf<Byte>()
        le16(buf, 0x0200) // type PACKAGE
        le16(buf, headerSize)
        le32(buf, chunkSize.toLong())
        le32(buf, id.toLong())
        buf.addAll(nameBytes.toList())
        le32(buf, 0) // typeStrings offset
        le32(buf, 0) // lastPublicType
        le32(buf, 0) // keyStrings offset
        le32(buf, 0) // lastPublicKey
        return buf.toByteArray()
    }

    private fun buildStringPool(strings: List<String>): ByteArray {
        // 复用 axml 的逻辑(简化版)
        val stringData = mutableListOf<Byte>()
        val offsets = mutableListOf<Int>()
        for (s in strings) {
            offsets.add(stringData.size)
            val bytes = s.toByteArray(Charsets.UTF_8)
            stringData.add(bytes.size.toByte())
            stringData.add(bytes.size.toByte())
            bytes.forEach { stringData.add(it) }
            stringData.add(0)
        }
        val stringsStart = 28 + strings.size * 4
        val chunkSize = stringsStart + stringData.size
        val buf = mutableListOf<Byte>()
        le16(buf, 0x0001)
        le16(buf, 28)
        le32(buf, chunkSize.toLong())
        le32(buf, strings.size.toLong())
        le32(buf, 0)
        le32(buf, 0x100) // UTF-8
        le32(buf, stringsStart.toLong())
        le32(buf, 0)
        for (off in offsets) le32(buf, off.toLong())
        buf.addAll(stringData)
        return buf.toByteArray()
    }

    private fun le16(buf: MutableList<Byte>, v: Int) {
        buf.add(v.toByte()); buf.add((v shr 8).toByte())
    }

    private fun le32(buf: MutableList<Byte>, v: Long) {
        for (i in 0 until 4) buf.add((v shr (i * 8)).toByte())
    }
}
