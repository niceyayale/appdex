package com.appdex.elfviewer

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ELF 查看器 Repository:解析 ELF/ELF64 格式的 .so 共享库。
 *
 * 支持:
 * - ELF Header (magic, class, data, machine, entry, sections, segments)
 * - Section Headers (name, type, flags, addr, offset, size, link, info)
 * - Program Headers (type, offset, vaddr, paddr, filesz, memsz, flags, align)
 * - Dynamic Section
 * - Symbol Table
 */
@Suppress("MagicNumber")
@Singleton
class ElfViewerRepository @Inject constructor() {

    fun parse(filePath: String): ElfData {
        val file = File(filePath)
        val maxSize = 100 * 1024 * 1024 // 100MB limit
        if (file.length() > maxSize) {
            throw IllegalArgumentException("File too large (max 100MB): ${file.length()} bytes")
        }
        val bytes = file.readBytes()
        return parseBytes(bytes, filePath.substringAfterLast("/"))
    }

    fun parseBytes(bytes: ByteArray, fileName: String): ElfData {
        val parser = ElfParser(bytes)
        return parser.parse(fileName)
    }
}

// ── Data Models ──

data class ElfData(
    val fileName: String,
    val header: ElfHeader,
    val sections: List<ElfSection>,
    val segments: List<ElfSegment>,
    val symbols: List<ElfSymbol>,
    val dynamicEntries: List<ElfDynamicEntry>,
)

data class ElfHeader(
    val magic: String,
    val is64Bit: Boolean,
    val isLittleEndian: Boolean,
    val abi: String,
    val abiVersion: Int,
    val type: String,
    val machine: String,
    val entry: Long,
    val sectionCount: Int,
    val segmentCount: Int,
    val sectionNameIndex: Int,
)

data class ElfSection(
    val index: Int,
    val name: String,
    val type: String,
    val flags: String,
    val address: Long,
    val offset: Long,
    val size: Long,
    val link: Int,
    val info: Int,
    val align: Long,
    val entrySize: Long,
)

data class ElfSegment(
    val index: Int,
    val type: String,
    val offset: Long,
    val virtualAddress: Long,
    val physicalAddress: Long,
    val fileSize: Long,
    val memorySize: Long,
    val flags: String,
    val align: Long,
)

data class ElfSymbol(
    val index: Int,
    val name: String,
    val value: Long,
    val size: Long,
    val type: String,
    val bind: String,
    val visibility: String,
    val sectionIndex: String,
)

data class ElfDynamicEntry(
    val tag: String,
    val value: Long,
)
