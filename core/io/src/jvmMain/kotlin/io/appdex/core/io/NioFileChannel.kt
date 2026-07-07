package io.appdex.core.io

import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.OpenOption
import java.nio.file.Path
import java.nio.file.StandardOpenOption

/**
 * JVM 平台 [SeekableChannel] 实现,基于 [FileChannel]。
 */
class NioFileChannel(
    private val channel: FileChannel,
    private val onClose: () -> Unit = {},
) : SeekableChannel {

    override val size: Long
        get() = channel.size()

    override fun position(newPos: Long) {
        require(newPos in 0..channel.size()) {
            "position $newPos out of [0, ${channel.size()}]"
        }
        channel.position(newPos)
    }

    override fun read(buf: ByteArray, len: Int): Int {
        require(len >= 0 && len <= buf.size) { "invalid len=$len" }
        val bb = ByteBuffer.wrap(buf, 0, len)
        return channel.read(bb)
    }

    override fun write(buf: ByteArray, off: Int, len: Int) {
        require(off >= 0 && len >= 0 && off + len <= buf.size) {
            "invalid off=$off len=$len size=${buf.size}"
        }
        channel.write(ByteBuffer.wrap(buf, off, len))
    }

    override fun close() {
        channel.close()
        onClose()
    }

    companion object {
        fun open(path: Path, mode: Mode): NioFileChannel {
            val options = mutableSetOf<OpenOption>()
            when (mode) {
                Mode.Read -> options += StandardOpenOption.READ
                Mode.Write -> options += listOf(
                    StandardOpenOption.READ,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                )
                Mode.ReadWrite -> options += listOf(
                    StandardOpenOption.READ,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.CREATE,
                )
                Mode.Append -> options += listOf(
                    StandardOpenOption.WRITE,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND,
                )
            }
            val ch = FileChannel.open(path, *options.toTypedArray())
            if (mode == Mode.Append) ch.position(ch.size())
            return NioFileChannel(ch)
        }
    }
}
