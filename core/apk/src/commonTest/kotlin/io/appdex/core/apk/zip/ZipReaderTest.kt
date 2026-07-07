package io.appdex.core.apk.zip

import io.appdex.core.io.InMemorySeekableChannel
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ZipReaderTest {

    @Test
    fun `reads eocd of single-entry zip`() {
        val bytes = ZipSample.buildWithStoredEntries("a.txt" to byteArrayOf(1, 2, 3))
        val ch = InMemorySeekableChannel(bytes)
        val eocd = ZipReader().readEocd(ch)
        assertEquals(1, eocd.entryCount)
        // CD 在 local header(30 + 5 name) + 3 data = 38 字节之后
        assertEquals(38L, eocd.cdOffset)
    }

    @Test
    fun `reads eocd of two-entry zip`() {
        val bytes = ZipSample.buildWithStoredEntries(
            "a.txt" to byteArrayOf(1),
            "b.txt" to byteArrayOf(2, 3),
        )
        val ch = InMemorySeekableChannel(bytes)
        val eocd = ZipReader().readEocd(ch)
        assertEquals(2, eocd.entryCount)
    }

    @Test
    fun `reads eocd with trailing comment`() {
        val bytes = ZipSample.buildWithStoredEntries("a" to byteArrayOf(1))
        val withComment = bytes + byteArrayOf('Z'.code.toByte(), 'Z'.code.toByte())
        val ch = InMemorySeekableChannel(withComment)
        val eocd = ZipReader().readEocd(ch)
        assertEquals(1, eocd.entryCount)
    }
}
