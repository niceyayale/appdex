package io.appdex.core.io

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ByteReaderTest {

    @Test
    fun `reads little-endian uint16`() {
        val reader = ByteReader(InMemorySeekableChannel(byteArrayOf(0x34, 0x12)))
        assertEquals(0x1234, reader.readUInt16LE())
    }

    @Test
    fun `reads little-endian int32`() {
        val reader = ByteReader(InMemorySeekableChannel(byteArrayOf(0x78, 0x56, 0x34, 0x12)))
        assertEquals(0x12345678, reader.readInt32LE())
    }

    @Test
    fun `reads little-endian uint32 as long`() {
        // 0xFFFFFFFF = 4294967295L, 超出 Int 范围
        val reader = ByteReader(InMemorySeekableChannel(byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte())))
        assertEquals(4294967295L, reader.readUInt32LE())
    }

    @Test
    fun `reads little-endian int64`() {
        val bytes = byteArrayOf(0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07)
        val reader = ByteReader(InMemorySeekableChannel(bytes))
        assertEquals(0x0706050403020100L, reader.readInt64LE())
    }

    @Test
    fun `reads uint8`() {
        val reader = ByteReader(InMemorySeekableChannel(byteArrayOf(0xFF.toByte())))
        assertEquals(255, reader.readUInt8())
    }

    @Test
    fun `reads bytes block`() {
        val reader = ByteReader(InMemorySeekableChannel(byteArrayOf(1, 2, 3, 4, 5)))
        assertArrayEquals(byteArrayOf(1, 2, 3), reader.readBytes(3))
    }

    @Test
    fun `position tracks cursor`() {
        val reader = ByteReader(InMemorySeekableChannel(byteArrayOf(1, 2, 3, 4)))
        reader.readInt32LE()
        assertEquals(4L, reader.position())
    }

    @Test
    fun `position sets cursor`() {
        val ch = InMemorySeekableChannel(byteArrayOf(1, 2, 3, 4))
        val reader = ByteReader(ch)
        reader.position(2L)
        assertEquals(3, reader.readUInt8())
    }

    @Test
    fun `skip advances cursor`() {
        val reader = ByteReader(InMemorySeekableChannel(byteArrayOf(1, 2, 3, 4)))
        reader.skip(2L)
        assertEquals(3, reader.readUInt8())
    }
}
