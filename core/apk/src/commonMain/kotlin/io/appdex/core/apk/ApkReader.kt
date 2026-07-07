package io.appdex.core.apk

import io.appdex.core.io.SeekableChannel

/**
 * APK 条目(ZIP 内文件)。
 */
data class ApkEntry(
    val name: String,
    val compressionMethod: Int,
    val compressedSize: Long,
    val uncompressedSize: Long,
    val crc32: Long,
    /** 条目数据在 APK 文件中的起始偏移(local header 之后是数据)。 */
    val localHeaderOffset: Long,
)

/**
 * APK 签名信息。
 */
data class ApkSignatureInfo(
    val hasV2Signature: Boolean,
    val hasV3Signature: Boolean,
    /** 签名块在 APK 文件中的偏移。 */
    val signingBlockOffset: Long,
    val signingBlockSize: Long,
)

/**
 * APK 读取器接口。只读。
 */
interface ApkReader {
    /** 列出 APK 内所有条目。 */
    fun listEntries(channel: SeekableChannel): List<ApkEntry>

    /** 读取指定条目的(解压后)字节内容。 */
    fun readEntry(channel: SeekableChannel, entry: ApkEntry): ByteArray

    /** 读取签名信息。无签名时 hasV2Signature/hasV3Signature 均为 false。 */
    fun readSignatureInfo(channel: SeekableChannel): ApkSignatureInfo
}
