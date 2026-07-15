package com.appdex.elfviewer

import java.nio.ByteOrder

/**
 * ELF 二进制解析器 — 支持 32 位和 64 位 ELF。
 */
@Suppress("MagicNumber")
class ElfParser(private val data: ByteArray) {

    private var is64Bit = true
    private var byteOrder = ByteOrder.LITTLE_ENDIAN

    fun parse(fileName: String): ElfData {
        require(data.size >= 64 && data[0] == 0x7f.toByte() && data[1] == 'E'.code.toByte() &&
            data[2] == 'L'.code.toByte() && data[3] == 'F'.code.toByte()) {
            "Not an ELF file"
        }

        is64Bit = data[4].toInt() == 2
        byteOrder = if (data[5].toInt() == 1) ByteOrder.LITTLE_ENDIAN else ByteOrder.BIG_ENDIAN

        val header = parseHeader()
        val sections = parseSections(header)
        val segments = parseSegments(header)
        val symbols = parseSymbols(sections)
        val dynamicEntries = parseDynamic(sections)

        return ElfData(fileName, header, sections, segments, symbols, dynamicEntries)
    }

    private fun parseHeader(): ElfHeader {
        val magic = "0x7F454C46"
        val abi = when (data[7].toInt() and 0xFF) {
            0 -> "System V"
            3 -> "Linux"
            9 -> "FreeBSD"
            12 -> "OpenBSD"
            else -> "Unknown(${data[7].toInt() and 0xFF})"
        }

        val typeVal = readUShort(16)
        val type = when (typeVal) {
            0 -> "NONE"
            1 -> "REL"
            2 -> "EXEC"
            3 -> "DYN (shared library)"
            4 -> "CORE"
            else -> "Unknown($typeVal)"
        }

        val machineVal = readUShort(18)
        val machine = when (machineVal) {
            0x00 -> "None"
            0x03 -> "x86"
            0x28 -> "ARM"
            0x32 -> "IA-64"
            0x3E -> "x86-64"
            0xB7 -> "AArch64"
            0xF3 -> "RISC-V"
            else -> "Unknown(0x${machineVal.toString(16)})"
        }

        val entry: Long
        val phOff: Long
        val shOff: Long
        val shNum: Int
        val shStrNdx: Int
        val phNum: Int

        if (is64Bit) {
            entry = readULong(24)
            phOff = readULong(32)
            shOff = readULong(40)
            shNum = readUShort(60)
            shStrNdx = readUShort(62)
            phNum = readUShort(56)
        } else {
            entry = readUInt(24).toLong() and 0xFFFFFFFFL
            phOff = readUInt(28).toLong() and 0xFFFFFFFFL
            shOff = readUInt(32).toLong() and 0xFFFFFFFFL
            shNum = readUShort(48)
            shStrNdx = readUShort(50)
            phNum = readUShort(44)
        }

        return ElfHeader(magic, is64Bit, byteOrder == ByteOrder.LITTLE_ENDIAN, abi,
            data[8].toInt() and 0xFF, type, machine, entry, shNum, phNum, shStrNdx)
    }

    private fun parseSections(header: ElfHeader): List<ElfSection> {
        val sections = mutableListOf<ElfSection>()
        if (header.sectionCount == 0) return sections

        val shOff = if (is64Bit) readULong(40) else readUInt(32).toLong() and 0xFFFFFFFFL
        val shEntSize = readUShort(if (is64Bit) 58 else 46)
        val shStrNdx = header.sectionNameIndex

        var strTableOffset = 0L
        var strTableSize = 0L
        if (shStrNdx != 0) {
            val strHeaderOff = shOff + shStrNdx * shEntSize
            strTableOffset = if (is64Bit) readULong(strHeaderOff + 24) else readUInt(strHeaderOff + 16).toLong() and 0xFFFFFFFFL
            strTableSize = if (is64Bit) readULong(strHeaderOff + 32) else readUInt(strHeaderOff + 20).toLong() and 0xFFFFFFFFL
        }

        for (i in 0 until header.sectionCount) {
            val off = shOff + i * shEntSize
            if (off >= data.size) break

            val nameIdx = readUInt(off)
            val typeVal = readUInt(off + 4)
            val name = readStringFromTable(strTableOffset, strTableSize, nameIdx)
            val type = sectionTypeToString(typeVal)

            if (is64Bit) {
                val flags = readULong(off + 8)
                val addr = readULong(off + 16)
                val sOff = readULong(off + 24)
                val size = readULong(off + 32)
                val link = readUInt(off + 40)
                val info = readUInt(off + 44)
                val align = readULong(off + 48)
                val entSize = readULong(off + 56)
                sections.add(ElfSection(i, name, type, flagsToString(flags), addr, sOff, size, link, info, align, entSize))
            } else {
                val flags = readUInt(off + 8).toLong() and 0xFFFFFFFFL
                val addr = readUInt(off + 12).toLong() and 0xFFFFFFFFL
                val sOff = readUInt(off + 16).toLong() and 0xFFFFFFFFL
                val size = readUInt(off + 20).toLong() and 0xFFFFFFFFL
                val link = readUInt(off + 24)
                val info = readUInt(off + 28)
                val align = readUInt(off + 32).toLong() and 0xFFFFFFFFL
                val entSize = readUInt(off + 36).toLong() and 0xFFFFFFFFL
                sections.add(ElfSection(i, name, type, flagsToString(flags), addr, sOff, size, link, info, align, entSize))
            }
        }

        return sections
    }

    private fun parseSegments(header: ElfHeader): List<ElfSegment> {
        val segments = mutableListOf<ElfSegment>()
        if (header.segmentCount == 0) return segments

        val phOff = if (is64Bit) readULong(32) else readUInt(28).toLong() and 0xFFFFFFFFL
        val phEntSize = readUShort(if (is64Bit) 54 else 42)

        for (i in 0 until header.segmentCount) {
            val off = phOff + i * phEntSize
            if (off >= data.size) break

            if (is64Bit) {
                val typeVal = readUInt(off)
                val flags = readUInt(off + 4)
                val offset = readULong(off + 8)
                val vaddr = readULong(off + 16)
                val paddr = readULong(off + 24)
                val filesz = readULong(off + 32)
                val memsz = readULong(off + 40)
                val align = readULong(off + 48)
                segments.add(ElfSegment(i, segmentTypeToString(typeVal), offset, vaddr, paddr, filesz, memsz, segFlagsToString(flags), align))
            } else {
                val typeVal = readUInt(off)
                val offset = readUInt(off + 4).toLong() and 0xFFFFFFFFL
                val vaddr = readUInt(off + 8).toLong() and 0xFFFFFFFFL
                val paddr = readUInt(off + 12).toLong() and 0xFFFFFFFFL
                val filesz = readUInt(off + 16).toLong() and 0xFFFFFFFFL
                val memsz = readUInt(off + 20).toLong() and 0xFFFFFFFFL
                val flags = readUInt(off + 24)
                val align = readUInt(off + 28).toLong() and 0xFFFFFFFFL
                segments.add(ElfSegment(i, segmentTypeToString(typeVal), offset, vaddr, paddr, filesz, memsz, segFlagsToString(flags), align))
            }
        }

        return segments
    }

    private fun parseSymbols(sections: List<ElfSection>): List<ElfSymbol> {
        val symbols = mutableListOf<ElfSymbol>()
        val symSections = sections.filter { it.type == "DYNSYM" || it.type == "SYMTAB" }

        for (symSection in symSections) {
            val strSection = sections.getOrNull(symSection.link)
            val strTableOffset = strSection?.offset ?: 0L
            val strTableSize = strSection?.size ?: 0L

            val entrySize = if (is64Bit) 24 else 16
            val count = (symSection.size / entrySize).toInt()

            for (i in 0 until count) {
                val off = symSection.offset + i * entrySize
                if (off >= data.size) break
                val offInt = off.toInt()

                val nameIdx = readUInt(off)
                val name = readStringFromTable(strTableOffset, strTableSize, nameIdx)

                if (is64Bit) {
                    val info = data[offInt + 4].toInt() and 0xFF
                    val other = data[offInt + 5].toInt() and 0xFF
                    val shndx = readUShort(off + 6)
                    val value = readULong(off + 8)
                    val size = readULong(off + 16)
                    val type = symTypeToString(info and 0x0F)
                    val bind = symBindToString(info shr 4)
                    val visibility = visToString(other and 0x03)
                    symbols.add(ElfSymbol(i, name, value, size, type, bind, visibility, shndxToString(shndx)))
                } else {
                    val value = readUInt(off + 4).toLong() and 0xFFFFFFFFL
                    val size = readUInt(off + 8).toLong() and 0xFFFFFFFFL
                    val info = data[offInt + 12].toInt() and 0xFF
                    val other = data[offInt + 13].toInt() and 0xFF
                    val shndx = readUShort(off + 14)
                    val type = symTypeToString(info and 0x0F)
                    val bind = symBindToString(info shr 4)
                    val visibility = visToString(other and 0x03)
                    symbols.add(ElfSymbol(i, name, value, size, type, bind, visibility, shndxToString(shndx)))
                }

                if (symbols.size >= 500) return symbols
            }
        }

        return symbols
    }

    private fun parseDynamic(sections: List<ElfSection>): List<ElfDynamicEntry> {
        val entries = mutableListOf<ElfDynamicEntry>()
        val dynSection = sections.find { it.type == "DYNAMIC" } ?: return entries

        val entrySize = if (is64Bit) 16 else 8
        val count = (dynSection.size / entrySize).toInt()

        for (i in 0 until count) {
            val off = dynSection.offset + i * entrySize
            if (off >= data.size) break

            val tag: Long
            val value: Long

            if (is64Bit) {
                tag = readULong(off)
                value = readULong(off + 8)
            } else {
                tag = readUInt(off).toLong() and 0xFFFFFFFFL
                value = readUInt(off + 4).toLong() and 0xFFFFFFFFL
            }

            if (tag == 0L) break
            entries.add(ElfDynamicEntry(dynamicTagToString(tag), value))
        }

        return entries
    }

    private fun readStringFromTable(tableOffset: Long, tableSize: Long, idx: Int): String {
        if (tableSize == 0L || idx < 0) return ""
        val pos = tableOffset + idx
        if (pos >= data.size) return ""
        val end = (pos + 255).coerceAtMost(tableOffset + tableSize).coerceAtMost(data.size.toLong())
        val sb = StringBuilder()
        var p = pos.toInt()
        val endInt = end.toInt()
        while (p < endInt && data[p] != 0.toByte()) {
            sb.append(data[p].toInt().toChar())
            p++
        }
        return sb.toString()
    }

    private fun sectionTypeToString(type: Int): String = when (type) {
        0 -> "NULL"; 1 -> "PROGBITS"; 2 -> "SYMTAB"; 3 -> "STRTAB"; 4 -> "RELA"
        5 -> "HASH"; 6 -> "DYNAMIC"; 7 -> "NOTE"; 8 -> "NOBITS"; 9 -> "REL"
        11 -> "DYNSYM"; 14 -> "INIT_ARRAY"; 15 -> "FINI_ARRAY"
        else -> "UNKNOWN(0x${type.toString(16)})"
    }

    private fun segmentTypeToString(type: Int): String = when (type) {
        0 -> "NULL"; 1 -> "LOAD"; 2 -> "DYNAMIC"; 3 -> "INTERP"; 4 -> "NOTE"
        6 -> "PHDR"; 7 -> "TLS"
        0x6474e551 -> "GNU_STACK"; 0x6474e552 -> "GNU_RELRO"
        else -> "UNKNOWN(0x${type.toString(16)})"
    }

    private fun flagsToString(flags: Long): String {
        val sb = StringBuilder()
        if (flags and 0x1L != 0L) sb.append("W")
        if (flags and 0x2L != 0L) sb.append("A")
        if (flags and 0x4L != 0L) sb.append("X")
        if (flags and 0x20L != 0L) sb.append("S")
        return sb.toString()
    }

    private fun segFlagsToString(flags: Int): String {
        val sb = StringBuilder()
        if (flags and 0x4 != 0) sb.append("R")
        if (flags and 0x2 != 0) sb.append("W")
        if (flags and 0x1 != 0) sb.append("X")
        return sb.toString()
    }

    private fun symTypeToString(type: Int): String = when (type) {
        0 -> "NOTYPE"; 1 -> "OBJECT"; 2 -> "FUNC"; 3 -> "SECTION"; 4 -> "FILE"
        else -> "UNKNOWN($type)"
    }

    private fun symBindToString(bind: Int): String = when (bind) {
        0 -> "LOCAL"; 1 -> "GLOBAL"; 2 -> "WEAK"
        else -> "UNKNOWN($bind)"
    }

    private fun visToString(vis: Int): String = when (vis) {
        0 -> "DEFAULT"; 1 -> "INTERNAL"; 2 -> "HIDDEN"; 3 -> "PROTECTED"
        else -> "UNKNOWN($vis)"
    }

    private fun shndxToString(shndx: Int): String = when (shndx) {
        0 -> "UNDEF"; 0xFFF1 -> "ABS"; 0xFFF2 -> "COMMON"
        else -> shndx.toString()
    }

    private fun dynamicTagToString(tag: Long): String = when (tag) {
        0L -> "NULL"; 1L -> "NEEDED"; 5L -> "STRTAB"; 6L -> "SYMTAB"
        7L -> "RELA"; 10L -> "STRSZ"; 11L -> "SYMENT"; 12L -> "INIT"; 13L -> "FINI"
        14L -> "SONAME"; 17L -> "REL"; 20L -> "PLTREL"; 23L -> "JMPREL"
        25L -> "INIT_ARRAY"; 26L -> "FINI_ARRAY"
        else -> "UNKNOWN(0x${tag.toString(16)})"
    }

    private fun readUShort(offset: Long): Int {
        val o = offset.toInt()
        if (o + 1 >= data.size) return 0
        return if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
            (data[o].toInt() and 0xFF) or ((data[o + 1].toInt() and 0xFF) shl 8)
        } else {
            ((data[o].toInt() and 0xFF) shl 8) or (data[o + 1].toInt() and 0xFF)
        }
    }

    private fun readUInt(offset: Long): Int {
        val o = offset.toInt()
        if (o + 3 >= data.size) return 0
        return if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
            (data[o].toInt() and 0xFF) or
                ((data[o + 1].toInt() and 0xFF) shl 8) or
                ((data[o + 2].toInt() and 0xFF) shl 16) or
                ((data[o + 3].toInt() and 0xFF) shl 24)
        } else {
            ((data[o].toInt() and 0xFF) shl 24) or
                ((data[o + 1].toInt() and 0xFF) shl 16) or
                ((data[o + 2].toInt() and 0xFF) shl 8) or
                (data[o + 3].toInt() and 0xFF)
        }
    }

    private fun readULong(offset: Long): Long {
        val o = offset.toInt()
        if (o + 7 >= data.size) return 0
        return if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
            (data[o].toLong() and 0xFF) or
                ((data[o + 1].toLong() and 0xFF) shl 8) or
                ((data[o + 2].toLong() and 0xFF) shl 16) or
                ((data[o + 3].toLong() and 0xFF) shl 24) or
                ((data[o + 4].toLong() and 0xFF) shl 32) or
                ((data[o + 5].toLong() and 0xFF) shl 40) or
                ((data[o + 6].toLong() and 0xFF) shl 48) or
                ((data[o + 7].toLong() and 0xFF) shl 56)
        } else {
            ((data[o].toLong() and 0xFF) shl 56) or
                ((data[o + 1].toLong() and 0xFF) shl 48) or
                ((data[o + 2].toLong() and 0xFF) shl 40) or
                ((data[o + 3].toLong() and 0xFF) shl 32) or
                ((data[o + 4].toLong() and 0xFF) shl 24) or
                ((data[o + 5].toLong() and 0xFF) shl 16) or
                ((data[o + 6].toLong() and 0xFF) shl 8) or
                (data[o + 7].toLong() and 0xFF)
        }
    }
}
