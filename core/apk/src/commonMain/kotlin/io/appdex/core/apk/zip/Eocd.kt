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
