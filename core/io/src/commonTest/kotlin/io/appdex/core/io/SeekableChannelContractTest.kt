package io.appdex.core.io

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * SeekableChannel 契约测试。所有 SeekableChannel 实现都应通过此测试。
 * 子类通过抽象方法 channelWith(bytes) 提供实现。
 */
abstract class SeekableChannelContractTest {

    /** 用给定初始内容构造一个可读写的 SeekableChannel。 */
    protected abstract fun channelWith(initialBytes: ByteArray): SeekableChannel

    @Test
    fun `size returns initial content length`() {
        val ch = channelWith(byteArrayOf(1, 2, 3, 4))
        assertEquals(4L, ch.size)
        ch.close()
    }

    @Test
    fun `read returns bytes from start`() {
        val ch = channelWith(byteArrayOf(10, 20, 30))
        val buf = ByteArray(3)
        val n = ch.read(buf, 3)
        assertEquals(3, n)
        assertArrayEquals(byteArrayOf(10, 20, 30), buf)
        ch.close()
    }

    @Test
    fun `position moves read cursor`() {
        val ch = channelWith(byteArrayOf(1, 2, 3, 4, 5))
        ch.position(2L)
        val buf = ByteArray(2)
        val n = ch.read(buf, 2)
        assertEquals(2, n)
        assertArrayEquals(byteArrayOf(3, 4), buf)
        ch.close()
    }

    @Test
    fun `write appends and size grows`() {
        val ch = channelWith(byteArrayOf(1, 2))
        ch.position(ch.size)
        ch.write(byteArrayOf(3, 4), 0, 2)
        assertEquals(4L, ch.size)
        ch.position(0L)
        val buf = ByteArray(4)
        ch.read(buf, 4)
        assertArrayEquals(byteArrayOf(1, 2, 3, 4), buf)
        ch.close()
    }

    @Test
    fun `position beyond size throws`() {
        val ch = channelWith(byteArrayOf(1))
        assertThrows<IllegalArgumentException> { ch.position(5L) }
        ch.close()
    }
}
