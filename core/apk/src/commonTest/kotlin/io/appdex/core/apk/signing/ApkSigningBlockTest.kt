package io.appdex.core.apk.signing

import io.appdex.core.io.InMemorySeekableChannel
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ApkSigningBlockTest {

    @Test
    fun `no signing block returns false`() {
        // CD 直接在数据后,没有签名块
        val bytes = ByteArray(100)
        val ch = InMemorySeekableChannel(bytes)
        val info = ApkSigningBlockReader().detect(ch, cdOffset = 50)
        assertFalse(info.hasV2Signature)
        assertFalse(info.hasV3Signature)
    }

    @Test
    fun `detects v2 signing block`() {
        // 构造一个最小签名块(只有 v2 pair,空 value)
        val (bytes, cdOffset) = buildSignedApk(hasV2 = true, hasV3 = false)
        val ch = InMemorySeekableChannel(bytes)
        val info = ApkSigningBlockReader().detect(ch, cdOffset)
        assertTrue(info.hasV2Signature)
        assertFalse(info.hasV3Signature)
        assertTrue(info.signingBlockOffset > 0)
    }

    @Test
    fun `detects v2 and v3 in signing block`() {
        val (bytes, cdOffset) = buildSignedApk(hasV2 = true, hasV3 = true)
        val ch = InMemorySeekableChannel(bytes)
        val info = ApkSigningBlockReader().detect(ch, cdOffset)
        assertTrue(info.hasV2Signature)
        assertTrue(info.hasV3Signature)
        assertTrue(info.signingBlockOffset > 0)
    }

    /** 构造带签名块的伪 APK:数据 + 签名块 + CD + EOCD。 */
    private fun buildSignedApk(hasV2: Boolean, hasV3: Boolean): Pair<ByteArray, Long> {
        val data = ByteArray(10) // 假数据
        val dataLen = data.size.toLong()

        // 签名块内容(pairs)
        val pairs = mutableListOf<Byte>()
        if (hasV2) {
            // pair: length(8) + id(4) + value(0)
            addLe64(pairs, 4L) // pair length = 4(id only, no value)
            addLe32(pairs, 0x7109871a)
        }
        if (hasV3) {
            addLe64(pairs, 4L)
            addLe32(pairs, 0xf05368c0)
        }
        val pairsBytes = pairs.toByteArray()

        // size_of_block 字段值:不含头部 8 字节 size 字段本身(per spec)
        // = pairs + footer_size(8) + magic(16)
        val blockSize = pairsBytes.size.toLong() + 8L + 16L
        val signingBlock = mutableListOf<Byte>()
        addLe64(signingBlock, blockSize)
        signingBlock.addAll(pairsBytes.toList())
        addLe64(signingBlock, blockSize)
        signingBlock.addAll("APK Sig Block 42".toByteArray().toList())
        val sbBytes = signingBlock.toByteArray()

        // CD(最小:一个空条目)
        val cd = ByteArray(46) // 单个 central directory entry(全 0 即可,签名块检测不解析 CD 内容)
        // EOCD
        val eocd = ByteArray(22)
        eocd[0] = 0x50; eocd[1] = 0x4b; eocd[2] = 0x05; eocd[3] = 0x06

        val cdOffset = dataLen + sbBytes.size
        addLe32(eocd, 16, cdOffset)

        val result = data + sbBytes + cd + eocd
        return result to cdOffset
    }

    private fun addLe32(buf: MutableList<Byte>, v: Long) {
        buf.add(v.toByte())
        buf.add((v shr 8).toByte())
        buf.add((v shr 16).toByte())
        buf.add((v shr 24).toByte())
    }

    private fun addLe32(buf: ByteArray, off: Int, v: Long) {
        buf[off] = v.toByte()
        buf[off + 1] = (v shr 8).toByte()
        buf[off + 2] = (v shr 16).toByte()
        buf[off + 3] = (v shr 24).toByte()
    }

    private fun addLe64(buf: MutableList<Byte>, v: Long) {
        for (i in 0 until 8) buf.add((v shr (i * 8)).toByte())
    }
}
