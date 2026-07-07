package io.appdex.core.axml.chunk

/**
 * AXML chunk 通用头。
 * 所有 chunk 都以 type(uint16) + headerSize(uint16) + chunkSize(uint32) 开头。
 */
data class ChunkHeader(
    val type: Int,
    val headerSize: Int,
    val chunkSize: Long,
)

/** chunk type 常量。 */
object ChunkType {
    const val STRING_POOL = 0x0001
    const val XML = 0x0003
    const val XML_START_TAG = 0x0102
    const val XML_END_TAG = 0x0103
    const val XML_TEXT = 0x0104
    const val RESOURCE_MAP = 0x0180
}
