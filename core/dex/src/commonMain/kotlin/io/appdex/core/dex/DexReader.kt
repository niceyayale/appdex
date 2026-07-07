package io.appdex.core.dex

/**
 * DEX 内的类。
 */
data class DexClass(
    /** 类型描述符,如 "Lcom/example/Test;" */
    val type: String,
    /** 简单类名,如 "Test"(从 type 提取) */
    val name: String,
    val accessFlags: Int,
    /** 父类类型描述符,可空(如 java.lang.Object) */
    val superclass: String?,
    /** 实现的接口类型描述符列表 */
    val interfaces: List<String>,
    /** 源文件名,可空 */
    val sourceFile: String?,
    val fields: List<DexField>,
    val methods: List<DexMethod>,
)

data class DexField(
    val name: String,
    val type: String,
    val accessFlags: Int,
)

data class DexMethod(
    val name: String,
    val parameterTypes: List<String>,
    val returnType: String,
    val accessFlags: Int,
)

/**
 * DEX 读取器接口。只读。
 */
interface DexReader {
    /** 列出 DEX 内所有类。 */
    fun listClasses(dexBytes: ByteArray): List<DexClass>

    /** 将指定类反汇编为 smali 文本。 */
    fun toSmali(dexBytes: ByteArray, classType: String): String
}
