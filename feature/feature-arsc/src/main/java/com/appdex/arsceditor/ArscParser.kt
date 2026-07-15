package com.appdex.arsceditor

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * ARSC 二进制解析器 — 完整版。
 *
 * 解析 TABLE chunk 中的:
 * - Global StringPool
 * - Package chunk (0x0200)
 *   - Type StringPool, Key StringPool
 *   - TypeSpec chunks (0x0202)
 *   - Type chunks (0x0201)
 */
@Suppress("MagicNumber")
class ArscParser(private val data: ByteArray) {

    fun parse(): ArscParseResult {
        val type = readUShort(0)
        require(type == 0x0002) { "Not an ARSC file: 0x${type.toString(16)}" }
        val headerSize = readUShort(2)
        val packageCount = readUInt(8)

        var offset = headerSize
        val packages = mutableListOf<ArscPackageData>()

        val firstChunkType = readUShort(offset)
        if (firstChunkType == 0x0001) {
            val spSize = readUInt(offset + 4)
            offset += spSize
        }

        repeat(packageCount) {
            if (offset >= data.size) return@repeat
            val pkg = parsePackage(offset)
            if (pkg != null) {
                packages.add(pkg.data)
                offset = pkg.endOffset
            }
        }

        return ArscParseResult(packages)
    }

    private data class ParsedPackage(val data: ArscPackageData, val endOffset: Int)

    private fun parsePackage(offset: Int): ParsedPackage? {
        val chunkType = readUShort(offset)
        if (chunkType != 0x0200) return null

        val headerSize = readUShort(offset + 2)
        val chunkSize = readUInt(offset + 4)
        val endOffset = offset + chunkSize

        val pkgId = readInt(offset + 8)
        val nameBytes = ByteArray(256)
        System.arraycopy(data, offset + 12, nameBytes, 0, 256)
        val name = String(nameBytes, Charsets.UTF_16LE).trimEnd('\u0000')

        val typeStringsOffset = readUInt(offset + 12 + 256)
        val keyStringsOffset = readUInt(offset + 12 + 256 + 8)

        val typeStrings = if (typeStringsOffset > 0) {
            parseStringPool(offset + typeStringsOffset)
        } else emptyList()

        val keyStrings = if (keyStringsOffset > 0) {
            parseStringPool(offset + keyStringsOffset)
        } else emptyList()

        val entries = mutableListOf<ArscEntryData>()
        var subOffset = offset + headerSize

        while (subOffset < endOffset) {
            val subType = readUShort(subOffset)
            val subChunkSize = readUInt(subOffset + 4)

            when (subType) {
                0x0201 -> {
                    val typeEntries = parseTypeChunk(subOffset, typeStrings, keyStrings, pkgId)
                    entries.addAll(typeEntries)
                }
            }

            if (subChunkSize <= 0) break
            subOffset += subChunkSize
        }

        return ParsedPackage(ArscPackageData(pkgId, name, typeStrings, keyStrings, entries), endOffset)
    }

    private fun parseTypeChunk(
        offset: Int,
        typeStrings: List<String>,
        keyStrings: List<String>,
        pkgId: Int,
    ): List<ArscEntryData> {
        val headerSize = readUShort(offset + 2)
        val typeId = data[offset + 8].toInt() and 0xFF
        val entryCount = readUInt(offset + 12)
        val entriesStart = readUInt(offset + 16)

        val configSize = readUInt(offset + 20)
        val config = parseConfig(offset + 20, configSize)

        val typeName = typeStrings.getOrNull(typeId - 1) ?: "type$typeId"
        val entries = mutableListOf<ArscEntryData>()

        val entriesOffset = offset + entriesStart
        for (i in 0 until entryCount) {
            val entryOffset = readUInt(offset + headerSize + i * 4)
            if (entryOffset == -1) continue

            val entryPos = entriesOffset + entryOffset
            if (entryPos + 8 > data.size) continue

            val entryFlags = readUShort(entryPos + 2)
            val keyRef = readInt(entryPos + 4)
            val keyName = keyStrings.getOrNull(keyRef) ?: "key$keyRef"

            val resourceId = (pkgId shl 24) or (typeId shl 16) or i

            val value = if (entryFlags and 0x0001 != 0) {
                val parentRef = readInt(entryPos + 8)
                val count = readUInt(entryPos + 12)
                val sb = StringBuilder()
                if (parentRef != 0) sb.append("parent=0x${parentRef.toString(16)}, ")
                sb.append("count=$count")
                sb.toString()
            } else {
                val dataType = data[entryPos + 11].toInt() and 0xFF
                val valData = readInt(entryPos + 12)
                resolveValue(dataType, valData)
            }

            entries.add(ArscEntryData(resourceId, typeName, keyName, value, config))
        }

        return entries
    }

    private fun parseConfig(offset: Int, size: Int): String {
        if (size < 28) return "default"
        val locale = readUInt(offset + 8)
        val screenType = readUInt(offset + 12)

        val parts = mutableListOf<String>()

        if (locale != 0) {
            val b1 = (locale ushr 24) and 0xFF
            val b2 = (locale ushr 16) and 0xFF
            if (b1 in 0x61..0x7A && b2 in 0x61..0x7A) {
                parts.add("${b1.toChar()}${b2.toChar()}")
            }
        }

        val density = (screenType ushr 16) and 0xFFFF
        if (density != 0 && density != 0xFFFF) {
            parts.add("${density}dpi")
        }

        val sdk = screenType and 0xFFFF
        if (sdk != 0) {
            parts.add("v$sdk")
        }

        return if (parts.isEmpty()) "default" else parts.joinToString("-")
    }

    private fun resolveValue(dataType: Int, data: Int): String {
        return when (dataType) {
            0x03 -> "string"
            0x01 -> "@0x${data.toString(16)}"
            0x10 -> data.toString()
            0x11 -> "0x${data.toString(16)}"
            0x12 -> if (data != 0) "true" else "false"
            0x00 -> "null"
            else -> "type=$dataType data=0x${data.toString(16)}"
        }
    }

    private fun parseStringPool(offset: Int): List<String> {
        val type = readUShort(offset)
        if (type != 0x0001) return emptyList()

        val headerSize = readUShort(offset + 2)
        val stringCount = readUInt(offset + 8)
        val flags = readUInt(offset + 16)
        val stringsStart = readUInt(offset + 20)

        val isUtf8 = (flags and 0x100) != 0
        val strings = mutableListOf<String>()

        for (i in 0 until stringCount) {
            val strOffset = readUInt(offset + headerSize + i * 4)
            val strPos = offset + stringsStart + strOffset
            if (strPos >= data.size) {
                strings.add("")
                continue
            }

            val s = if (isUtf8) readUtf8String(strPos) else readUtf16String(strPos)
            strings.add(s)
        }

        return strings
    }

    private fun readUtf8String(pos: Int): String {
        var p = pos
        val len1 = data[p++].toInt() and 0xFF
        if (len1 and 0x80 != 0) p++

        val uLen = data[p++].toInt() and 0xFF
        val uLen2 = if (uLen and 0x80 != 0) {
            val u2 = data[p++].toInt() and 0xFF
            ((uLen and 0x7F) shl 8) or u2
        } else uLen

        val bytes = ByteArray(uLen2)
        val copyLen = minOf(uLen2, data.size - p)
        if (copyLen > 0) System.arraycopy(data, p, bytes, 0, copyLen)
        return String(bytes, Charsets.UTF_8)
    }

    private fun readUtf16String(pos: Int): String {
        val len = readUShort(pos)
        val byteLen = len * 2
        if (pos + 2 + byteLen > data.size) return ""
        val bytes = ByteArray(byteLen)
        System.arraycopy(data, pos + 2, bytes, 0, byteLen)
        return String(bytes, Charsets.UTF_16LE)
    }

    private fun readUShort(offset: Int): Int {
        if (offset + 1 >= data.size) return 0
        return (data[offset].toInt() and 0xFF) or ((data[offset + 1].toInt() and 0xFF) shl 8)
    }

    private fun readUInt(offset: Int): Int {
        if (offset + 3 >= data.size) return 0
        return (data[offset].toInt() and 0xFF) or
            ((data[offset + 1].toInt() and 0xFF) shl 8) or
            ((data[offset + 2].toInt() and 0xFF) shl 16) or
            ((data[offset + 3].toInt() and 0xFF) shl 24)
    }

    private fun readInt(offset: Int): Int = readUInt(offset)
}
