package com.appdex.repack

import android.util.Log

import com.android.tools.smali.smali.Smali
import com.android.tools.smali.smali.SmaliOptions
import com.appdex.signing.SigningRepository
import com.appdex.signing.SigningSchemeConfig
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import java.util.zip.ZipEntry
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 回编译结果。
 */
data class RepackResult(
    val success: Boolean,
    val outputFilePath: String,
    val message: String,
    val dexCount: Int = 0,
    val signingResult: com.appdex.signing.SigningResult? = null,
)

/**
 * APK 回编译仓库。
 *
 * 全链路:
 * 1. Smali → DEX 编译 (使用 smali 库)
 * 2. APK 重新打包 (替换 DEX 文件)
 * 3. APK 签名 (使用 apksig)
 *
 * 架构优势: 全流程自动化，支持增量编译。
 */
@Singleton
class RepackagingRepository @Inject constructor(
    private val signingRepository: SigningRepository,
) {

    /**
     * 编译 Smali 文本为 DEX 字节数据。
     *
     * @param smaliContents Map: 类名 → Smali 文本
     * @return DEX 字节数据
     */
    fun compileSmaliToDex(smaliContents: Map<String, String>): Result<ByteArray> {
        require(smaliContents.isNotEmpty()) { "No Smali content to compile" }

        // 创建临时目录
        val tempDir = Files.createTempDirectory("AppX_smali_").toFile()
        val smaliDir = File(tempDir, "smali").apply { mkdirs() }
        val outputDex = File(tempDir, "output.dex")

        return try {
            // 写入 Smali 文件
            smaliContents.forEach { (className, smaliText) ->
                // 将类名转换为文件路径
                val fileName = className.removePrefix("L").removeSuffix(";").replace("/", File.separator) + ".smali"
                val smaliFile = File(smaliDir, fileName)
                smaliFile.parentFile?.mkdirs()
                smaliFile.writeText(smaliText)
            }

            // 配置编译选项
            val options = SmaliOptions().apply {
                apiLevel = 27
                outputDexFile = outputDex.absolutePath
            }

            // 收集所有 .smali 文件路径
            val smaliFiles = smaliDir.walkTopDown()
                .filter { it.extension == "smali" }
                .map { it.absolutePath }
                .toList()

            // 编译
            Smali.assemble(options, smaliFiles)

            if (!outputDex.exists()) {
                Result.failure(IllegalStateException("DEX compilation failed: output file not created"))
            } else {
                Result.success(outputDex.readBytes())
            }
        } catch (e: Exception) {
            Log.w("AppX", "Suppressed exception", e)
            Result.failure(e)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    /**
     * 编译单个 Smali 文本为 DEX。
     */
    fun compileSmaliToDex(smaliText: String): Result<ByteArray> {
        return compileSmaliToDex(mapOf("class" to smaliText))
    }

    /**
     * 重新打包 APK：替换指定 DEX 文件，保留其他文件不变。
     *
     * @param inputApkPath 原 APK 路径
     * @param outputApkPath 输出 APK 路径
     * @param dexReplacements Map: DEX 文件名 → 新 DEX 字节数据
     */
    fun repackApk(
        inputApkPath: String,
        outputApkPath: String,
        dexReplacements: Map<String, ByteArray>,
    ): RepackResult {
        // 1. 复制 APK，替换指定的 DEX 文件
        ZipFile(inputApkPath).use { inputZip ->
            FileOutputStream(outputApkPath).use { fos ->
                ZipOutputStream(fos).use { out ->
                    val entries = inputZip.entries()
                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()
                        val entryName = entry.name

                        // 跳过签名相关文件（V1 签名会被重新生成）
                        if (entryName.startsWith("META-INF/") &&
                            (entryName.endsWith(".SF") || entryName.endsWith(".RSA") ||
                             entryName.endsWith(".DSA") || entryName.endsWith(".MF"))) {
                            continue
                        }

                        // 检查是否需要替换此 DEX
                        val replacement = dexReplacements[entryName]
                        val newEntry = ZipEntry(entryName)
                        newEntry.method = ZipEntry.DEFLATED

                        if (replacement != null) {
                            // 替换 DEX 文件
                            newEntry.size = replacement.size.toLong()
                            newEntry.compressedSize = -1
                            out.putNextEntry(newEntry)
                            out.write(replacement)
                            out.closeEntry()
                        } else {
                            // 复制原始文件
                            out.putNextEntry(newEntry)
                            inputZip.getInputStream(entry).use { input ->
                                input.copyTo(out)
                            }
                            out.closeEntry()
                        }
                    }
                }
            }
        }

        return RepackResult(
            success = true,
            outputFilePath = outputApkPath,
            message = "回编译成功 (${dexReplacements.size} 个 DEX 已替换)",
            dexCount = dexReplacements.size,
        )
    }

    /**
     * 全链路回编译 + 签名。
     *
     * 1. 编译 Smali → DEX
     * 2. 重新打包 APK
     * 3. 签名 APK
     *
     * @param inputApkPath 原 APK 路径
     * @param outputApkPath 输出 APK 路径
     * @param smaliReplacements Map: DEX 文件名 → Map(类名 → Smali 文本)
     * @param keystorePath Keystore 路径
     * @param keystorePassword Keystore 密码
     * @param keyAlias Key 别名
     * @param keyPassword Key 密码
     * @param schemeConfig 签名方案配置
     */
    fun repackAndSign(
        inputApkPath: String,
        outputApkPath: String,
        smaliReplacements: Map<String, Map<String, String>>,
        keystorePath: String,
        keystorePassword: String,
        keyAlias: String,
        keyPassword: String,
        schemeConfig: SigningSchemeConfig = SigningSchemeConfig(),
    ): RepackResult {
        // 1. 编译所有 Smali → DEX
        val dexReplacements = mutableMapOf<String, ByteArray>()
        for ((dexName, smaliContents) in smaliReplacements) {
            val dexResult = compileSmaliToDex(smaliContents)
            if (dexResult.isFailure) {
                return RepackResult(
                    success = false,
                    outputFilePath = outputApkPath,
                    message = dexResult.exceptionOrNull()?.message ?: "DEX 编译失败",
                    dexCount = dexReplacements.size,
                )
            }
            dexReplacements[dexName] = dexResult.getOrThrow()
        }

        // 2. 重新打包
        val unsignedApkPath = outputApkPath.replace(".apk", "_unsigned.apk")
        val repackResult = repackApk(inputApkPath, unsignedApkPath, dexReplacements)
        if (!repackResult.success) {
            return repackResult
        }

        // 3. 签名
        val signingResult = signingRepository.signApk(
            inputApkPath = unsignedApkPath,
            outputApkPath = outputApkPath,
            keystorePath = keystorePath,
            keystorePassword = keystorePassword,
            keyAlias = keyAlias,
            keyPassword = keyPassword,
            schemeConfig = schemeConfig,
        )

        // 清理临时文件
        File(unsignedApkPath).delete()

        return RepackResult(
            success = signingResult.success,
            outputFilePath = outputApkPath,
            message = if (signingResult.success) "回编译+签名成功" else "回编译成功但签名失败",
            dexCount = dexReplacements.size,
            signingResult = signingResult,
        )
    }

    /**
     * 列出 APK 中的所有 DEX 文件。
     */
    fun listDexFiles(apkPath: String): List<String> {
        val dexFiles = mutableListOf<String>()
        ZipFile(apkPath).use { zip ->
            val entries = zip.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                if (entry.name.endsWith(".dex")) {
                    dexFiles.add(entry.name)
                }
            }
        }
        return dexFiles
    }
}
