package io.appdex.core.axml

/**
 * AXML 读取结果:解析后的 XML 文本。
 */
data class AxmlDocument(
    /** XML 文本表示。 */
    val xml: String,
)

/**
 * 二进制 AndroidManifest.xml 读取器。
 */
interface AxmlReader {
    /**
     * 读取二进制 AXML 并转为文本 XML。
     * @throws IllegalArgumentException 二进制格式不合法
     */
    fun read(binary: ByteArray): AxmlDocument
}
