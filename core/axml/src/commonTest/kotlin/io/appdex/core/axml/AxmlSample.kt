package io.appdex.core.axml

/**
 * 测试工具:构造二进制 AXML 字节流。
 *
 * 二进制 AXML 结构:
 * [XML header chunk]
 *   [StringPool chunk]
 *   [XML_START_TAG chunk] * N
 *   [XML_END_TAG chunk] * N
 */
object AxmlSample {

    /**
     * 构造最小 AXML:单根元素 <manifest/>。
     * StringPool 含: "manifest"
     */
    fun buildMinimalManifest(): ByteArray {
        val strings = listOf("manifest")
        val stringPool = buildStringPool(strings)
        val xmlHeader = buildXmlHeader()
        val startTag = buildStartTag(stringIndex = 0, attributes = emptyList())
        val endTag = buildEndTag(stringIndex = 0)

        return xmlHeader + stringPool + startTag + endTag
    }

    /**
     * 构造带属性的单元素:<element attr="value"/>
     */
    fun buildWithAttribute(): ByteArray {
        val strings = listOf("element", "attr", "value")
        val stringPool = buildStringPool(strings)
        val xmlHeader = buildXmlHeader()
        // attribute: ns=0xFFFFFFFF(无), name=1("attr"), rawValue=2("value")
        val attr = AttrSpec(nsIndex = 0xFFFFFFFF.toInt(), nameIndex = 1, rawValueIndex = 2, typedValue = "value".toByteArray())
        val startTag = buildStartTag(stringIndex = 0, attributes = listOf(attr))
        val endTag = buildEndTag(stringIndex = 0)
        return xmlHeader + stringPool + startTag + endTag
    }

    // ---- chunk builders ----

    private fun buildXmlHeader(): ByteArray {
        // XML chunk: type=0x0003, headerSize=8, chunkSize=8(空容器)
        val buf = ByteArray(8)
        le16(buf, 0, 0x0003)
        le16(buf, 2, 8)
        le32(buf, 4, 8)
        return buf
    }

    private fun buildStringPool(strings: List<String>): ByteArray {
        // 简化:UTF-8,无 style
        val stringData = mutableListOf<Byte>()
        val offsets = mutableListOf<Int>()

        for (s in strings) {
            offsets.add(stringData.size)
            val bytes = s.toByteArray(Charsets.UTF_8)
            // UTF-8 字符串:两个长度(len, len)—— 实际 Android 格式:utf8Len(1或2字节) + chars + null
            // 简化:假设 len < 128,用 1 字节
            stringData.add(bytes.size.toByte()) // 字符数(utf16 length,这里简化用字节长度)
            stringData.add(bytes.size.toByte()) // 字节长度
            bytes.forEach { stringData.add(it) }
            stringData.add(0) // null terminator
        }

        val headerSize = 8
        val stringCount = strings.size
        // stringPool 头:8(header) + 4(stringCount) + 4(styleCount) + 4(flags) + 4(stringsStart) + 4(stylesStart) = 28
        // + stringCount * 4 (offsets) + stringData
        val stringsStart = 28 + stringCount * 4
        val chunkSize = stringsStart + stringData.size

        val buf = mutableListOf<Byte>()
        // header
        le16(buf, 0x0001) // type
        le16(buf, 28)      // headerSize
        le32(buf, chunkSize.toLong())
        le32(buf, stringCount.toLong())
        le32(buf, 0) // styleCount
        le32(buf, 0x100) // flags: UTF-8
        le32(buf, stringsStart.toLong())
        le32(buf, 0) // stylesStart
        // offsets
        for (off in offsets) le32(buf, off.toLong())
        // string data
        buf.addAll(stringData)

        return buf.toByteArray()
    }

    private data class AttrSpec(
        val nsIndex: Int,
        val nameIndex: Int,
        val rawValueIndex: Int,
        val typedValue: ByteArray,
    )

    private fun buildStartTag(stringIndex: Int, attributes: List<AttrSpec>): ByteArray {
        // XML_START_TAG: type=0x0102, headerSize=16, chunkSize=36 + 20*attrCount
        // 内容:type(2)+headerSize(2)+chunkSize(4) = 8
        //      + lineNumber(4)+comment(4)+nsPrefix(4)+name(4) = 16
        //      + attrStart(2)+attrSize(2)+attrCount(2)+idIdx(2)+classIdx(2)+styleIdx(2) = 12
        //      + attrs (attrCount * 20)
        // 总计 = 36 + attrCount * 20
        val attrBytes = attributes.size * 20
        val chunkSize = 36 + attrBytes
        val buf = mutableListOf<Byte>()
        le16(buf, 0x0102) // type
        le16(buf, 16)     // headerSize
        le32(buf, chunkSize.toLong())
        le32(buf, 1) // lineNumber
        le32(buf, 0xFFFFFFFF) // comment
        le32(buf, 0xFFFFFFFF) // nsPrefix
        le32(buf, stringIndex.toLong()) // name
        le16(buf, 0x14) // attrStart = 20
        le16(buf, 0x14) // attrSize = 20
        le16(buf, attributes.size) // attrCount
        le16(buf, 0) // idIdx
        le16(buf, 0) // classIdx
        le16(buf, 0) // styleIdx
        for (a in attributes) {
            le32(buf, a.nsIndex.toLong())
            le32(buf, a.nameIndex.toLong())
            le32(buf, a.rawValueIndex.toLong())
            // typedValue: size(2) + res0(1) + dataType(1) + data(4)
            le16(buf, 8) // size
            le16(buf, 0x0300) // res0=0, dataType=3(STRING_REF)—— 简化
            le32(buf, a.rawValueIndex.toLong()) // data = string index
        }
        return buf.toByteArray()
    }

    private fun buildEndTag(stringIndex: Int): ByteArray {
        // XML_END_TAG: type(2)+headerSize(2)+chunkSize(4) = 8
        //              + lineNumber(4)+comment(4)+nsPrefix(4)+name(4) = 16
        //              总计 = 24
        val buf = mutableListOf<Byte>()
        le16(buf, 0x0103) // type
        le16(buf, 16)     // headerSize
        le32(buf, 24)     // chunkSize
        le32(buf, 1)      // lineNumber
        le32(buf, 0xFFFFFFFF) // comment
        le32(buf, 0xFFFFFFFF) // nsPrefix
        le32(buf, stringIndex.toLong()) // name
        return buf.toByteArray()
    }

    private fun le16(buf: MutableList<Byte>, v: Int) {
        buf.add(v.toByte())
        buf.add((v shr 8).toByte())
    }

    private fun le16(buf: ByteArray, off: Int, v: Int) {
        buf[off] = v.toByte()
        buf[off + 1] = (v shr 8).toByte()
    }

    private fun le32(buf: MutableList<Byte>, v: Long) {
        for (i in 0 until 4) buf.add((v shr (i * 8)).toByte())
    }

    private fun le32(buf: ByteArray, off: Int, v: Long) {
        for (i in 0 until 4) buf[off + i] = (v shr (i * 8)).toByte()
    }
}
