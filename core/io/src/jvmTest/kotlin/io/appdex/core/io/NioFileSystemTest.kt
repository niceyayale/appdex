package io.appdex.core.io

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class NioFileSystemTest : FileSystemContractTest() {

    @TempDir
    lateinit var tempDir: Path

    override fun fs(): FileSystem = NioFileSystem(tempDir.toFile())

    @Test
    fun `stat on real file returns size`() {
        val fs = fs()
        fs.open("/real", Mode.Write).use { it.write(byteArrayOf(1, 2, 3, 4, 5), 0, 5) }
        val e = fs.stat("/real")!!
        assertEquals(5L, e.size)
    }
}
