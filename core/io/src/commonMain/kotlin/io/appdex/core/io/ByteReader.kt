package io.appdex.core.io

/**
 * 在 [SeekableChannel] 之上提供 little-endian 字节读取工具。
 *
 * Android 二进制格式(APK/AXML/ARSC/DEX)统一使用小端字节序。
 * 非线程安全。
 *
 * 注:[SeekableChannel] 接口只暴露 `position(Long)` 设置方法,没有读取当前
 * position 的方法;本类用 [_cursor] 字段自行跟踪游标,在 [readFully] 内同步更新,
 * 在 [position] / [skip] 内同步设置。
 */
class ByteReader(private val channel: SeekableChannel) {

    @Suppress("MagicNumber")
    private val scratch = ByteArray(8)

    /** 当前游标位置。 */
    fun position(): Long = _cursor

    /** 设置游标。 */
    fun position(newPos: Long) {
        channel.position(newPos)
        _cursor = newPos
    }

    /** 跳过 [n] 字节。 */
    fun skip(n: Long) {
        position(_cursor + n)
    }

    @Suppress("MagicNumber")
    fun readUInt8(): Int {
        readFully(scratch, 1)
        return scratch[0].toInt() and 0xFF
    }

    @Suppress("MagicNumber")
    fun readUInt16LE(): Int {
        readFully(scratch, 2)
        return (scratch[0].toInt() and 0xFF) or
            ((scratch[1].toInt() and 0xFF) shl 8)
    }

    fun readInt16LE(): Short = readUInt16LE().toShort()

    @Suppress("MagicNumber")
    fun readInt32LE(): Int {
        readFully(scratch, 4)
        return (scratch[0].toInt() and 0xFF) or
            ((scratch[1].toInt() and 0xFF) shl 8) or
            ((scratch[2].toInt() and 0xFF) shl 16) or
            ((scratch[3].toInt() and 0xFF) shl 24)
    }

    /** 读 4 字节无符号整数,用 Long 避免溢出。 */
    @Suppress("MagicNumber")
    fun readUInt32LE(): Long = readInt32LE().toLong() and 0xFFFFFFFFL

    @Suppress("MagicNumber")
    fun readInt64LE(): Long {
        readFully(scratch, 8)
        var v = 0L
        for (i in 7 downTo 0) {
            v = (v shl 8) or (scratch[i].toLong() and 0xFFL)
        }
        return v
    }

    fun readBytes(n: Int): ByteArray {
        require(n >= 0) { "n must be >= 0: $n" }
        val buf = ByteArray(n)
        if (n > 0) readFully(buf, n)
        return buf
    }

    private fun readFully(buf: ByteArray, len: Int) {
        require(len <= buf.size) { "len $len exceeds buf size ${buf.size}" }
        var read = 0
        while (read < len) {
            // channel.read 把数据写到 tmp 开头(偏移 0);用临时缓冲再 copyInto
            // 到目标偏移,处理部分读。注:不使用 System.arraycopy(commonMain 不可用)。
            val tmp = ByteArray(len - read)
            val n = channel.read(tmp, len - read)
            check(n > 0) { "unexpected EOF at ${_cursor + read}, need $len bytes" }
            tmp.copyInto(buf, read, 0, n)
            read += n
            _cursor += n
        }
    }

    private var _cursor: Long = 0L
}
