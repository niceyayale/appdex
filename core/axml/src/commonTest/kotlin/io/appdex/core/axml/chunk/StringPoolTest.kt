package io.appdex.core.axml.chunk

import io.appdex.core.axml.AxmlSample
import io.appdex.core.io.ByteReader
import io.appdex.core.io.InMemorySeekableChannel
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class StringPoolTest {

    @Test
    fun `parses minimal string pool`() {
        val bytes = AxmlSample.buildMinimalManifest()
        // bytes[0..8) = XML header;bytes[8..] = StringPool
        val ch = InMemorySeekableChannel(bytes)
        val reader = ByteReader(ch)
        reader.position(8) // 跳过 XML header
        val pool = StringPool.parse(reader)
        assertEquals(listOf("manifest"), pool.strings)
    }

    @Test
    fun `parses string pool with multiple strings`() {
        val bytes = AxmlSample.buildWithAttribute()
        val ch = InMemorySeekableChannel(bytes)
        val reader = ByteReader(ch)
        reader.position(8)
        val pool = StringPool.parse(reader)
        assertEquals(listOf("element", "attr", "value"), pool.strings)
    }
}
