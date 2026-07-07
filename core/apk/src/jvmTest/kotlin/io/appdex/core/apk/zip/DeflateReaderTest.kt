package io.appdex.core.apk.zip

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.zip.Deflater

class DeflateReaderTest {

    @Test
    fun `inflates deflated data`() {
        val original = "hello world this is a test of deflate".toByteArray()
        // raw DEFLATE(no zlib header)
        val deflater = Deflater(Deflater.DEFAULT_COMPRESSION, true)
        deflater.setInput(original)
        deflater.finish()
        val compressed = ByteArray(256)
        val n = deflater.deflate(compressed)
        val comp = compressed.copyOfRange(0, n)

        val inflated = inflate(comp, original.size)
        assertArrayEquals(original, inflated)
    }
}
