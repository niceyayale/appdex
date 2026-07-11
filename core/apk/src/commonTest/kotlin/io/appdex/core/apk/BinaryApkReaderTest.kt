package io.appdex.core.apk

import io.appdex.core.apk.zip.ZipSample
import io.appdex.core.io.InMemorySeekableChannel
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class BinaryApkReaderTest {

    @Test
    fun `lists entries via high-level api`() {
        val bytes = ZipSample.buildWithStoredEntries(
            "AndroidManifest.xml" to byteArrayOf(1, 2, 3),
            "classes.dex" to byteArrayOf(4, 5),
        )
        val reader = BinaryApkReader()
        val entries = reader.listEntries(InMemorySeekableChannel(bytes))
        assertEquals(2, entries.size)
        assertEquals("AndroidManifest.xml", entries[0].name)
    }

    @Test
    fun `reads entry content via high-level api`() {
        val bytes = ZipSample.buildWithStoredEntries("a" to byteArrayOf(10, 20))
        val reader = BinaryApkReader()
        val entries = reader.listEntries(InMemorySeekableChannel(bytes))
        val content = reader.readEntry(InMemorySeekableChannel(bytes), entries[0])
        assertArrayEquals(byteArrayOf(10, 20), content)
    }
}
