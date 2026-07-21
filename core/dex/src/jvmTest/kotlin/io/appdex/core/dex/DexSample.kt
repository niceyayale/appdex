package io.appdex.core.dex

import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.immutable.ImmutableClassDef
import com.android.tools.smali.dexlib2.immutable.ImmutableDexFile
import com.android.tools.smali.dexlib2.immutable.ImmutableField
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.instruction.ImmutableInstruction10x
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore
import com.android.tools.smali.dexlib2.writer.pool.DexPool

/**
 * 测试用 DEX 样本构造器。沿用 Task 1 spike 验证过的 ImmutableClassDef + DexPool 序列化方式。
 */
object DexSample {

    private const val TEST_CLASS = "Lcom/example/Test;"
    private const val SUPERCLASS = "Ljava/lang/Object;"
    private const val SOURCE_FILE = "Test.java"

    /** accessFlags = PUBLIC = 1 */
    private const val PUBLIC = 1

    /**
     * 单空类 com.example.Test extends Object。无字段无方法。
     */
    fun buildMinimalDex(): ByteArray {
        val classDef = ImmutableClassDef(
            TEST_CLASS,
            PUBLIC,
            SUPERCLASS,
            null,
            SOURCE_FILE,
            null,
            null,
            null,
        )
        return serialize(classDef)
    }

    /**
     * 单类带一个字段:public int count。
     */
    fun buildDexWithField(): ByteArray {
        val field = ImmutableField(
            TEST_CLASS,
            "count",
            "I",
            PUBLIC,
            null,
            null,
            null,
        )
        val classDef = ImmutableClassDef(
            TEST_CLASS,
            PUBLIC,
            SUPERCLASS,
            null,
            SOURCE_FILE,
            null,
            setOf(field),
            null,
        )
        return serialize(classDef)
    }

    /**
     * 单类带一个方法:public void hello() {}(空实现,仅 return-void)。
     */
    fun buildDexWithMethod(): ByteArray {
        val impl = ImmutableMethodImplementation(
            0,
            listOf(ImmutableInstruction10x(Opcode.RETURN_VOID)),
            emptyList(),
            emptyList(),
        )
        val method = ImmutableMethod(
            TEST_CLASS,
            "hello",
            emptyList(),
            "V",
            PUBLIC,
            null,
            null,
            impl,
        )
        val classDef = ImmutableClassDef(
            TEST_CLASS,
            PUBLIC,
            SUPERCLASS,
            null,
            SOURCE_FILE,
            null,
            null,
            setOf(method),
        )
        return serialize(classDef)
    }

    private fun serialize(classDef: ImmutableClassDef): ByteArray {
        // ImmutableDexFile 验证 class 可被加载;DexPool 实际序列化为字节
        ImmutableDexFile(Opcodes.getDefault(), listOf(classDef))
        val store = MemoryDataStore()
        DexPool(Opcodes.getDefault()).apply {
            internClass(classDef)
            writeTo(store)
        }
        return store.getData()
    }
}
