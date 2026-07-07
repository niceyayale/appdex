package io.appdex.core.apk.signing

import io.appdex.core.io.ByteReader
import io.appdex.core.io.SeekableChannel

/**
 * APK 签名块检测。
 *
 * 位置:ZIP 条目数据之后、Central Directory 之前。
 * magic:ASCII "APK Sig Block 42"(16 字节,位于块末尾)。
 *
 * v2 签名 ID:0x7109871a
 * v3 签名 ID:0xf05368c0
 */
class ApkSigningBlockReader {

    /**
     * 检测签名块。
     * @param cdOffset Central Directory 的起始偏移(从 EOCD 获得)
     * @return 签名信息;无签名块时返回 hasV2Signature=false, hasV3Signature=false
     */
    fun detect(channel: SeekableChannel, cdOffset: Long): ApkSignatureInfo {
        if (cdOffset < 32L) {
            return ApkSignatureInfo(false, false, 0L, 0L)
        }
        // 签名块尾部的 magic 是 "APK Sig Block 42"(16 字节)
        // magic 紧贴在 CD 之前
        // 签名块结构:[size_of_block(uint64)][pairs...][size_of_block(uint64)][magic(16)]
        // magic 位于 cdOffset - 16
        val reader = ByteReader(channel)
        val magicOff = cdOffset - 16
        reader.position(magicOff)
        val magicBytes = reader.readBytes(16)
        val expectedMagic = "APK Sig Block 42".toByteArray(Charsets.US_ASCII)
        if (!magicBytes.contentEquals(expectedMagic)) {
            return ApkSignatureInfo(false, false, 0L, 0L)
        }
        // magic 之前 8 字节是块大小(footer)
        reader.position(magicOff - 8)
        val blockSize = reader.readInt64LE()
        // 块从 (cdOffset - blockSize - 8) 开始(8 是 footer size)
        val blockStart = cdOffset - blockSize - 8
        // 遍历 id-value pairs
        reader.position(blockStart + 8) // 跳过开头的 8 字节 size_of_block
        var hasV2 = false
        var hasV3 = false
        val endOfPairs = blockStart + blockSize - 24 // 减去 footer(8 size + 16 magic)
        while (reader.position() < endOfPairs) {
            val pairLen = reader.readInt64LE()
            val id = reader.readInt32LE()
            when (id) {
                0x7109871a -> hasV2 = true
                0xf05368c0L.toInt() -> hasV3 = true
            }
            // 跳过 pair 剩余:pairLen - 4(id 已读)
            reader.skip(pairLen - 4)
        }

        return ApkSignatureInfo(hasV2, hasV3, blockStart, blockSize)
    }
}

data class ApkSignatureInfo(
    val hasV2Signature: Boolean,
    val hasV3Signature: Boolean,
    val signingBlockOffset: Long,
    val signingBlockSize: Long,
)
