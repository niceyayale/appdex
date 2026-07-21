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

    @Test
    fun `lists entries of zip`() {
        val bytes = ZipSample.buildWithStoredEntries(
            "a.txt" to byteArrayOf(1, 2, 3),
            "b.txt" to byteArrayOf(4, 5),
        )
        val ch = InMemorySeekableChannel(bytes)
        val zip = ZipReader()
        val eocd = zip.readEocd(ch)
        val entries = zip.listEntries(ch, eocd)
        assertEquals(2, entries.size)
        assertEquals("a.txt", entries[0].name)
        assertEquals(0, entries[0].compressionMethod)
        assertEquals(3L, entries[0].uncompressedSize)
    }

    @Test
    fun `reads stored entry content`() {
        val bytes = ZipSample.buildWithStoredEntries("a.txt" to byteArrayOf(10, 20, 30))
        val ch = InMemorySeekableChannel(bytes)
        val zip = ZipReader()
        val eocd = zip.readEocd(ch)
        val entries = zip.listEntries(ch, eocd)
        val content = zip.readEntry(ch, entries[0])
        assertArrayEquals(byteArrayOf(10, 20, 30), content)
    }

    @Test
    fun `reads deflated entry content`() {
        // 用 java.util.zip 构造一个 DEFLATE 压缩的 ZIP
        val content = ByteArray(1000) { it.toByte() } // 重复模式,deflate 压缩率高
        val zipBytes = buildDeflatedZip("a.txt" to content)
        val ch = InMemorySeekableChannel(zipBytes)
        val zip = ZipReader()
        val eocd = zip.readEocd(ch)
        val entries = zip.listEntries(ch, eocd)
        assertEquals(8, entries[0].compressionMethod)
        val read = zip.readEntry(ch, entries[0])
        assertArrayEquals(content, read)
    }

    private fun buildDeflatedZip(vararg entries: Pair<String, ByteArray>): ByteArray {
        val baos = java.io.ByteArrayOutputStream()
        val zos = java.util.zip.ZipOutputStream(baos)
        for ((name, content) in entries) {
            val entry = java.util.zip.ZipEntry(name)
            entry.method = java.util.zip.ZipEntry.DEFLATED
            zos.putNextEntry(entry)
            zos.write(content)
            zos.closeEntry()
        }
        zos.close()
        return baos.toByteArray()
    }
}
