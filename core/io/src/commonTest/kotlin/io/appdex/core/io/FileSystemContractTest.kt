package io.appdex.core.io

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * SeekableChannel 本地 use 扩展。
 * 原因:Kotlin 2.0.21 commonMain stdlib 不含 kotlin.Closeable(2.1.0 才加入),
 * 故 stdlib 的 use 扩展也不可用。这里在测试包内本地定义。
 * 未来升级到 Kotlin 2.1.0+ 后可删除此扩展,改用 stdlib。
 */
internal inline fun <T : SeekableChannel, R> T.use(block: (T) -> R): R =
    try { block(this) } finally { close() }

/**
 * FileSystem 契约测试。所有 FileSystem 实现都应通过。
 * 子类通过 fs() 提供一个空的新实例。
 */
abstract class FileSystemContractTest {

    protected abstract fun fs(): FileSystem

    @Test
    fun `stat on missing returns null`() {
        val fs = fs()
        assertNull(fs.stat("/missing"))
    }

    @Test
    fun `mkdir creates directory entry`() {
        val fs = fs()
        fs.mkdir("/dir")
        val e = fs.stat("/dir")
        assertNotNull(e)
        assertEquals(EntryType.Directory, e!!.type)
        assertEquals("dir", e.name)
    }

    @Test
    fun `mkdir on existing throws`() {
        val fs = fs()
        fs.mkdir("/dir")
        assertThrows<FileAlreadyExistsException> { fs.mkdir("/dir") }
    }

    @Test
    fun `mkdir without parent throws`() {
        val fs = fs()
        assertThrows<NoSuchFileException> { fs.mkdir("/no/parent") }
    }

    @Test
    fun `open write creates file`() {
        val fs = fs()
        val ch = fs.open("/file", Mode.Write)
        ch.write(byteArrayOf(1, 2, 3), 0, 3)
        ch.close()
        val e = fs.stat("/file")!!
        assertEquals(EntryType.File, e.type)
        assertEquals(3L, e.size)
    }

    @Test
    fun `open read returns content`() {
        val fs = fs()
        fs.open("/file", Mode.Write).use { it.write(byteArrayOf(10, 20), 0, 2) }
        val ch = fs.open("/file", Mode.Read)
        val buf = ByteArray(2)
        val n = ch.read(buf, 2)
        assertEquals(2, n)
        assertArrayEquals(byteArrayOf(10, 20), buf)
        ch.close()
    }

    @Test
    fun `append mode keeps existing`() {
        val fs = fs()
        fs.open("/file", Mode.Write).use { it.write(byteArrayOf(1, 2), 0, 2) }
        fs.open("/file", Mode.Append).use { it.write(byteArrayOf(3, 4), 0, 2) }
        val ch = fs.open("/file", Mode.Read)
        val buf = ByteArray(4)
        assertEquals(4, ch.read(buf, 4))
        assertArrayEquals(byteArrayOf(1, 2, 3, 4), buf)
        ch.close()
    }

    @Test
    fun `list lists directory children`() {
        val fs = fs()
        fs.mkdir("/d")
        fs.open("/d/a", Mode.Write).use { it.write(byteArrayOf(1), 0, 1) }
        fs.open("/d/b", Mode.Write).use { it.write(byteArrayOf(2), 0, 1) }
        val entries = fs.list("/d").sortedBy { it.name }
        assertEquals(2, entries.size)
        assertEquals("a", entries[0].name)
        assertEquals("b", entries[1].name)
    }

    @Test
    fun `delete removes file`() {
        val fs = fs()
        fs.open("/f", Mode.Write).use { it.write(byteArrayOf(1), 0, 1) }
        fs.delete("/f")
        assertNull(fs.stat("/f"))
    }

    @Test
    fun `delete on missing throws`() {
        val fs = fs()
        assertThrows<NoSuchFileException> { fs.delete("/missing") }
    }

    @Test
    fun `rename moves file`() {
        val fs = fs()
        fs.open("/a", Mode.Write).use { it.write(byteArrayOf(1, 2, 3), 0, 3) }
        fs.rename("/a", "/b")
        assertNull(fs.stat("/a"))
        val e = fs.stat("/b")!!
        assertEquals(3L, e.size)
    }

    @Test
    fun `open read on missing throws`() {
        val fs = fs()
        assertThrows<NoSuchFileException> { fs.open("/missing", Mode.Read) }
    }
}
