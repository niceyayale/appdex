package com.appdex.dex

import com.android.tools.smali.baksmali.Adaptors.ClassDefinition
import com.android.tools.smali.baksmali.BaksmaliOptions
import com.android.tools.smali.baksmali.formatter.BaksmaliWriter
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.iface.ClassDef
import java.io.File
import java.io.StringWriter
import java.util.zip.ZipFile
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DEX 条目信息。
 */
data class DexFileInfo(
    val name: String,
    val size: Long,
    val compressedSize: Long,
)

/**
 * DEX 中的类信息。
 */
data class DexClassInfo(
    val type: String,
    val simpleName: String,
    val packageName: String,
    val accessFlags: Int,
    val superclass: String?,
    val interfaces: List<String>,
    val sourceFile: String?,
    val fieldCount: Int,
    val methodCount: Int,
)

/**
 * DEX 仓库。负责从 APK 中提取 DEX 并解析。
 *
 * Library 层 — 不依赖 Android Framework，纯 JVM。
 */
@Singleton
class DexRepository @Inject constructor() {

    /**
     * 列出 APK 内所有 .dex 文件。
     */
    fun listDexFiles(apkPath: String): List<DexFileInfo> {
        val zipFile = ZipFile(File(apkPath))
        val result = mutableListOf<DexFileInfo>()
        zipFile.use { zf ->
            zf.entries().toList()
                .filter { it.name.endsWith(".dex") }
                .sortedBy { it.name }
                .forEach { entry ->
                    result.add(
                        DexFileInfo(
                            name = entry.name,
                            size = entry.size,
                            compressedSize = entry.compressedSize,
                        )
                    )
                }
        }
        return result
    }

    /**
     * 读取指定 DEX 文件中的所有类。
     */
    fun listClasses(apkPath: String, dexName: String): List<DexClassInfo> {
        val dexBytes = extractDexBytes(apkPath, dexName)
        val dexFile = DexBackedDexFile(Opcodes.getDefault(), dexBytes)
        return dexFile.classes.map { cls ->
            DexClassInfo(
                type = cls.type,
                simpleName = extractSimpleName(cls.type),
                packageName = extractPackageName(cls.type),
                accessFlags = cls.accessFlags,
                superclass = cls.superclass,
                interfaces = cls.interfaces.toList(),
                sourceFile = cls.sourceFile,
                fieldCount = cls.fields.count(),
                methodCount = cls.methods.count(),
            )
        }.sortedBy { it.type }.toList()
    }

    /**
     * 将指定类反汇编为 Smali 文本。
     */
    fun toSmali(apkPath: String, dexName: String, classType: String): String {
        val dexBytes = extractDexBytes(apkPath, dexName)
        val dexFile = DexBackedDexFile(Opcodes.getDefault(), dexBytes)
        val targetClass = dexFile.classes.firstOrNull { it.type == classType }
            ?: throw ClassNotFoundException("Class $classType not found in $dexName")

        val classDefinition = ClassDefinition(BaksmaliOptions(), targetClass)
        val stringWriter = StringWriter()
        BaksmaliWriter(stringWriter).use { writer ->
            classDefinition.writeTo(writer)
        }
        return stringWriter.toString()
    }

    /**
     * 从 APK 中提取指定 DEX 文件的字节数据。
     */
    private fun extractDexBytes(apkPath: String, dexName: String): ByteArray {
        val zipFile = ZipFile(File(apkPath))
        return zipFile.use { zf ->
            val entry = zf.getEntry(dexName)
                ?: throw IllegalArgumentException("DEX file $dexName not found in APK")
            zf.getInputStream(entry).use { it.readBytes() }
        }
    }

    private fun extractSimpleName(typeDescriptor: String): String {
        // "Lcom/example/Test;" -> "Test"
        val inner = typeDescriptor.removePrefix("L").removeSuffix(";")
        val lastSlash = inner.lastIndexOf('/')
        return if (lastSlash >= 0) inner.substring(lastSlash + 1) else inner
    }

    private fun extractPackageName(typeDescriptor: String): String {
        // "Lcom/example/Test;" -> "com.example"
        val inner = typeDescriptor.removePrefix("L").removeSuffix(";")
        val lastSlash = inner.lastIndexOf('/')
        return if (lastSlash > 0) {
            inner.substring(0, lastSlash).replace('/', '.')
        } else {
            ""
        }
    }
}
