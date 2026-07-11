package io.appdex.core.dex

import com.android.tools.smali.baksmali.Adaptors.ClassDefinition
import com.android.tools.smali.baksmali.BaksmaliOptions
import com.android.tools.smali.baksmali.formatter.BaksmaliWriter
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import java.io.StringWriter

/**
 * 基于 dexlib2 的 DEX 只读解析器实现。
 */
class BinaryDexReader : DexReader {

    override fun listClasses(dexBytes: ByteArray): List<DexClass> {
        val dexFile = DexBackedDexFile(Opcodes.getDefault(), dexBytes)
        return dexFile.classes.map { cls ->
            DexClass(
                type = cls.type,
                name = extractSimpleName(cls.type),
                accessFlags = cls.accessFlags,
                superclass = cls.superclass,
                interfaces = cls.interfaces.toList(),
                sourceFile = cls.sourceFile,
                fields = cls.fields.map { f ->
                    DexField(f.name, f.type, f.accessFlags)
                }.toList(),
                methods = cls.methods.map { m ->
                    DexMethod(
                        m.name,
                        m.parameterTypes.map { it.toString() },
                        m.returnType,
                        m.accessFlags,
                    )
                }.toList(),
            )
        }.toList()
    }

    override fun toSmali(dexBytes: ByteArray, classType: String): String {
        val dexFile = DexBackedDexFile(Opcodes.getDefault(), dexBytes)
        val targetClass = dexFile.classes.first { it.type == classType }
        val classDefinition = ClassDefinition(BaksmaliOptions(), targetClass)
        val stringWriter = StringWriter()
        BaksmaliWriter(stringWriter).use { writer ->
            classDefinition.writeTo(writer)
        }
        return stringWriter.toString()
    }

    private fun extractSimpleName(typeDescriptor: String): String {
        // "Lcom/example/Test;" -> "Test"
        val inner = typeDescriptor.removePrefix("L").removeSuffix(";")
        return inner.substringAfterLast('/')
    }
}
