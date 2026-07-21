package com.appdex.size

import java.io.File
import java.util.zip.ZipFile
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 文件类型分类。
 */
enum class FileCategory(val displayName: String, val extensions: Set<String>, val color: Long) {
    DEX("DEX 字节码", setOf(".dex"), 0xFF4CAF50),
    RESOURCES("资源文件", setOf(".arsc"), 0xFFFF9800),
    ASSETS("Assets", setOf(), 0xFF2196F3),
    NATIVE_LIBS("Native 库", setOf(".so"), 0xFFF44336),
    MANIFEST("清单文件", setOf(".xml"), 0xFF9C27B0),
    FONTS("字体", setOf(".ttf", ".otf", ".woff"), 0xFF795548),
    IMAGES("图片", setOf(".png", ".jpg", ".jpeg", ".gif", ".webp", ".bmp"), 0xFFE91E63),
    MEDIA("媒体", setOf(".mp3", ".mp4", ".wav", ".ogg", ".m4a"), 0xFF00BCD4),
    OTHER("其他", setOf(), 0xFF607D8B);

    companion object {
        fun fromPath(path: String): FileCategory {
            val ext = path.substringAfterLast(".", "").lowercase()
            if (ext.isNotEmpty()) {
                values().forEach { cat ->
                    if (cat.extensions.contains(".$ext")) return cat
                }
            }
            return when {
                path.startsWith("assets/") -> ASSETS
                path.startsWith("res/") -> IMAGES
                path.startsWith("lib/") -> NATIVE_LIBS
                path == "AndroidManifest.xml" -> MANIFEST
                else -> OTHER
            }
        }
    }
}

/**
 * 分类大小统计。
 */
data class CategorySize(
    val category: FileCategory,
    val totalSize: Long,
    val compressedSize: Long,
    val fileCount: Int,
    val percentage: Float,
)

/**
 * 大文件信息。
 */
data class LargeFile(
    val path: String,
    val size: Long,
    val compressedSize: Long,
    val category: FileCategory,
)

/**
 * 体积分析结果。
 */
data class SizeAnalysisResult(
    val apkName: String,
    val apkPath: String,
    val totalSize: Long,
    val compressedSize: Long,
    val fileCount: Int,
    val categories: List<CategorySize>,
    val largestFiles: List<LargeFile>,
    val duplicateFiles: List<LargeFile>,
)

/**
 * APK 体积分析仓库。
 *
 * 可视化分析 APK 空间占用，帮助优化体积。
 */
@Singleton
class SizeAnalyzerRepository @Inject constructor() {

    fun analyze(apkPath: String): SizeAnalysisResult {
        val apkFile = File(apkPath)
        val totalSize = apkFile.length()

        val entries = mutableListOf<ZipEntryInfo>()
        ZipFile(apkPath).use { zip ->
            zip.entries().asSequence().filter { !it.isDirectory }.forEach { entry ->
                entries.add(ZipEntryInfo(
                    path = entry.name,
                    size = entry.size,
                    compressedSize = entry.compressedSize
                ))
            }
        }

        val compressedTotal = entries.sumOf { it.compressedSize }

        // 按分类统计
        val categoryMap = mutableMapOf<FileCategory, MutableList<ZipEntryInfo>>()
        entries.forEach { entry ->
            val cat = FileCategory.fromPath(entry.path)
            categoryMap.getOrPut(cat) { mutableListOf() }.add(entry)
        }

        val categories = categoryMap.map { (cat, files) ->
            CategorySize(
                category = cat,
                totalSize = files.sumOf { it.size },
                compressedSize = files.sumOf { it.compressedSize },
                fileCount = files.size,
                percentage = if (totalSize > 0) files.sumOf { it.size }.toFloat() / totalSize * 100 else 0f
            )
        }.sortedByDescending { it.totalSize }

        // 最大文件
        val largestFiles = entries
            .sortedByDescending { it.size }
            .take(20)
            .map { LargeFile(it.path, it.size, it.compressedSize, FileCategory.fromPath(it.path)) }

        // 重复文件 (相同文件名的文件)
        val duplicateFiles = entries
            .groupBy { File(it.path).name to it.size }
            .filter { it.value.size > 1 }
            .values
            .flatten()
            .map { LargeFile(it.path, it.size, it.compressedSize, FileCategory.fromPath(it.path)) }
            .sortedByDescending { it.size }
            .take(10)

        return SizeAnalysisResult(
            apkName = apkFile.name,
            apkPath = apkPath,
            totalSize = totalSize,
            compressedSize = compressedTotal,
            fileCount = entries.size,
            categories = categories,
            largestFiles = largestFiles,
            duplicateFiles = duplicateFiles,
        )
    }

    private data class ZipEntryInfo(
        val path: String,
        val size: Long,
        val compressedSize: Long,
    )
}
