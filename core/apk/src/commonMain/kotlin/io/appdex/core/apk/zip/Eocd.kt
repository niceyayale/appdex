package io.appdex.core.apk.zip

/**
 * End of Central Directory Record。
 * 位于 ZIP 文件末尾。
 */
data class Eocd(
    /** Central directory 条目数。 */
    val entryCount: Int,
    /** Central directory 字节大小。 */
    val cdSize: Long,
    /** Central directory 起始偏移。 */
    val cdOffset: Long,
)

/** ZIP 条目(来自 Central Directory)。 */
data class ZipEntry(
    val name: String,
    val compressionMethod: Int,
    val compressedSize: Long,
    val uncompressedSize: Long,
    val crc32: Long,
    val localHeaderOffset: Long,
)
