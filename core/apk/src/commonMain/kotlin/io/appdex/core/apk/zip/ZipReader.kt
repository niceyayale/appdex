package io.appdex.core.apk.zip

import io.appdex.core.io.ByteReader
import io.appdex.core.io.SeekableChannel

/**
 * ZIP 格式读取器。只读。
 *
 * 实现基于 PKWARE APPNOTE 6.3.x,支持 STORED(0)和 DEFLATE(8)。
 * 不支持 ZIP64(后续 plan 再加)。
 */
class ZipReader {

    /**
     * 读取 EOCD。
     * 从文件末尾向前扫描签名 0x06054b50,最多 65557 字节(22 + 65535 注释)。
     */
    fun readEocd(channel: SeekableChannel): Eocd {
        val fileSize = channel.size
        val maxEocdSize = 22L + 65535L
        val searchStart = maxOf(0L, fileSize - maxEocdSize)
        val searchLen = (fileSize - searchStart).toInt()
        channel.position(searchStart)
        val buf = ByteArray(searchLen)
        var read = 0
        while (read < searchLen) {
            val tmp = ByteArray(searchLen - read)
            val n = channel.read(tmp, searchLen - read)
            if (n <= 0) break
            tmp.copyInto(buf, read, 0, n)
            read += n
        }

        // 从后向前找签名 0x06054b50
        var eocdOff = -1
        for (i in (read - 22) downTo 0) {
            if (buf[i] == 0x50.toByte() && buf[i + 1] == 0x4b.toByte() &&
                buf[i + 2] == 0x05.toByte() && buf[i + 3] == 0x06.toByte()
            ) {
                eocdOff = i
                break
            }
        }
        require(eocdOff >= 0) { "EOCD signature not found" }

        val absOff = searchStart + eocdOff
        val reader = ByteReader(channel)
        reader.position(absOff + 4)
        reader.skip(4) // disk number, cd disk
        val entryCount = reader.readUInt16LE()
        reader.skip(2) // total entries on this disk (same as entryCount for non-ZIP64)
        val cdSize = reader.readUInt32LE()
        val cdOffset = reader.readUInt32LE()
        return Eocd(entryCount, cdSize, cdOffset)
    }

    // listEntries / readEntry 在后续 Task 加
}
