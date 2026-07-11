package io.appdex.core.apk.zip

import java.util.zip.Inflater

internal actual fun inflate(compressed: ByteArray, uncompressedSize: Int): ByteArray {
    val inflater = Inflater(true) // raw DEFLATE(no zlib header)
    inflater.setInput(compressed)
    val out = ByteArray(uncompressedSize)
    val n = inflater.inflate(out)
    inflater.end()
    return out.copyOf(n)
}
