package io.appdex.core.apk.zip

/**
 * DEFLATE 解压。平台实现。
 * commonMain 声明,jvmMain 用 java.util.zip.Inflater。
 */
internal expect fun inflate(compressed: ByteArray, uncompressedSize: Int): ByteArray
