package io.appdex.core.arsc

/**
 * ARSC 解析结果。
 */
data class ArscTable(
    val packages: List<ArscPackage>,
)

data class ArscPackage(
    val id: Int,
    val name: String,
)

interface ArscReader {
    fun read(binary: ByteArray): ArscTable
}
