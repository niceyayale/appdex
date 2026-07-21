package com.appdex.diff

import com.appdex.apk.ApkFile
import java.io.File
import java.util.zip.ZipFile
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 文件差异类型。
 */
enum class DiffType {
    ADDED,      // 新增
    REMOVED,    // 删除
    MODIFIED,   // 修改
    SAME,       // 相同
}

/**
 * 单个文件的差异。
 */
data class FileDiff(
    val path: String,
    val type: DiffType,
    val oldSize: Long = 0,
    val newSize: Long = 0,
    val sizeDelta: Long = 0,
)

/**
 * 清单差异。
 */
data class ManifestDiff(
    val addedPermissions: List<String> = emptyList(),
    val removedPermissions: List<String> = emptyList(),
    val addedActivities: List<String> = emptyList(),
    val removedActivities: List<String> = emptyList(),
    val addedServices: List<String> = emptyList(),
    val removedServices: List<String> = emptyList(),
    val addedReceivers: List<String> = emptyList(),
    val removedReceivers: List<String> = emptyList(),
    val addedProviders: List<String> = emptyList(),
    val removedProviders: List<String> = emptyList(),
)

/**
 * APK 对比摘要。
 */
data class ApkDiffSummary(
    val oldApkName: String,
    val newApkName: String,
    val oldApkSize: Long,
    val newApkSize: Long,
    val totalFiles: Int,
    val addedCount: Int,
    val removedCount: Int,
    val modifiedCount: Int,
    val sameCount: Int,
    val fileDiffs: List<FileDiff>,
    val manifestDiff: ManifestDiff,
    val oldSignatureInfo: String,
    val newSignatureInfo: String,
)

/**
 * APK Diff 仓库。
 *
 * 对比两个 APK 的文件结构、清单、签名差异。
 */
@Singleton
class ApkDiffRepository @Inject constructor() {

    /**
     * 对比两个 APK。
     */
    fun diff(oldApkPath: String, newApkPath: String): ApkDiffSummary {
        val oldApk = ApkFile(oldApkPath)
        val newApk = ApkFile(newApkPath)
        val oldInfo = oldApk.use { it.parse() }
        val newInfo = newApk.use { it.parse() }

        // 1. 文件结构对比
        val fileDiffs = diffFileStructure(oldApkPath, newApkPath)

        // 2. 清单对比
        val manifestDiff = diffManifest(oldInfo.manifest, newInfo.manifest)

        // 3. 签名信息
        val oldSig = oldInfo.signatures.joinToString(", ") { "v${it.version}" }.ifEmpty { "无签名" }
        val newSig = newInfo.signatures.joinToString(", ") { "v${it.version}" }.ifEmpty { "无签名" }

        val added = fileDiffs.count { it.type == DiffType.ADDED }
        val removed = fileDiffs.count { it.type == DiffType.REMOVED }
        val modified = fileDiffs.count { it.type == DiffType.MODIFIED }
        val same = fileDiffs.count { it.type == DiffType.SAME }

        return ApkDiffSummary(
            oldApkName = File(oldApkPath).name,
            newApkName = File(newApkPath).name,
            oldApkSize = File(oldApkPath).length(),
            newApkSize = File(newApkPath).length(),
            totalFiles = fileDiffs.size,
            addedCount = added,
            removedCount = removed,
            modifiedCount = modified,
            sameCount = same,
            fileDiffs = fileDiffs,
            manifestDiff = manifestDiff,
            oldSignatureInfo = oldSig,
            newSignatureInfo = newSig,
        )
    }

    /**
     * 对比文件结构。
     */
    private fun diffFileStructure(oldApkPath: String, newApkPath: String): List<FileDiff> {
        val oldEntries = mutableMapOf<String, Long>()
        val newEntries = mutableMapOf<String, Long>()

        ZipFile(oldApkPath).use { zip ->
            zip.entries().asSequence().forEach { entry ->
                if (!entry.isDirectory) {
                    oldEntries[entry.name] = entry.size
                }
            }
        }

        ZipFile(newApkPath).use { zip ->
            zip.entries().asSequence().forEach { entry ->
                if (!entry.isDirectory) {
                    newEntries[entry.name] = entry.size
                }
            }
        }

        val allPaths = (oldEntries.keys + newEntries.keys).sorted()
        val diffs = mutableListOf<FileDiff>()

        for (path in allPaths) {
            val oldSize = oldEntries[path]
            val newSize = newEntries[path]

            when {
                oldSize == null && newSize != null -> {
                    diffs.add(FileDiff(path, DiffType.ADDED, 0, newSize, newSize))
                }
                oldSize != null && newSize == null -> {
                    diffs.add(FileDiff(path, DiffType.REMOVED, oldSize, 0, -oldSize))
                }
                oldSize != null && newSize != null -> {
                    if (oldSize == newSize) {
                        diffs.add(FileDiff(path, DiffType.SAME, oldSize, newSize, 0))
                    } else {
                        diffs.add(FileDiff(path, DiffType.MODIFIED, oldSize, newSize, newSize - oldSize))
                    }
                }
            }
        }

        return diffs
    }

    /**
     * 对比清单。
     */
    private fun diffManifest(oldManifest: com.appdex.apk.ApkManifest, newManifest: com.appdex.apk.ApkManifest): ManifestDiff {
        return ManifestDiff(
            addedPermissions = (newManifest.permissions - oldManifest.permissions.toSet()).toList(),
            removedPermissions = (oldManifest.permissions - newManifest.permissions.toSet()).toList(),
            addedActivities = (newManifest.activities - oldManifest.activities.toSet()).toList(),
            removedActivities = (oldManifest.activities - newManifest.activities.toSet()).toList(),
            addedServices = (newManifest.services - oldManifest.services.toSet()).toList(),
            removedServices = (oldManifest.services - newManifest.services.toSet()).toList(),
            addedReceivers = (newManifest.receivers - oldManifest.receivers.toSet()).toList(),
            removedReceivers = (oldManifest.receivers - newManifest.receivers.toSet()).toList(),
            addedProviders = (newManifest.providers - oldManifest.providers.toSet()).toList(),
            removedProviders = (oldManifest.providers - newManifest.providers.toSet()).toList(),
        )
    }
}
