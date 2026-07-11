package io.appdex.core.apk

import io.appdex.core.apk.signing.ApkSignatureInfo
import io.appdex.core.apk.signing.ApkSigningBlockReader
import io.appdex.core.apk.zip.ZipEntry
import io.appdex.core.apk.zip.ZipReader
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

/**
 * APK 读取实现。
 *
 * 组合 ZipReader 和 ApkSigningBlockReader。
 */
class BinaryApkReader : ApkReader {

    override fun listEntries(channel: SeekableChannel): List<ApkEntry> {
        val zip = ZipReader()
        val eocd = zip.readEocd(channel)
        return zip.listEntries(channel, eocd).map { e ->
            ApkEntry(e.name, e.compressionMethod, e.compressedSize, e.uncompressedSize, e.crc32, e.localHeaderOffset)
        }
    }

    override fun readEntry(channel: SeekableChannel, entry: ApkEntry): ByteArray {
        val zip = ZipReader()
        val zipEntry = ZipEntry(
            entry.name, entry.compressionMethod, entry.compressedSize,
            entry.uncompressedSize, entry.crc32, entry.localHeaderOffset,
        )
        return zip.readEntry(channel, zipEntry)
    }

    override fun readSignatureInfo(channel: SeekableChannel): ApkSignatureInfo {
        val zip = ZipReader()
        val eocd = zip.readEocd(channel)
        return ApkSigningBlockReader().detect(channel, eocd.cdOffset)
    }
}
