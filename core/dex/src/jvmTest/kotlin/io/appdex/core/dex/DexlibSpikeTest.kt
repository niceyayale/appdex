package io.appdex.core.dex

import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.immutable.ImmutableClassDef
import com.android.tools.smali.dexlib2.immutable.ImmutableDexFile
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore
import com.android.tools.smali.dexlib2.writer.pool.DexPool
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * spike:验证 dexlib2 关键 API 可用,并找到"构造 DEX 字节"的可行方式。
 * 后续测试用同样的方式构造样本。
 */
class DexlibSpikeTest {

    @Test
    fun `dexlib2 loads a dex byte array`() {
        val dexBytes = buildMinimalDex()
        val dexFile = DexBackedDexFile(Opcodes.getDefault(), dexBytes)
        // 能遍历类即通过
        val classes = dexFile.classes.toList()
        assertTrue(classes.isNotEmpty())
        assertEquals("Lcom/example/Test;", classes[0].type)
    }

    /**
     * 构造最小 DEX:单个空类 com.example.Test extends Object。
     * 用 ImmutableClassDef + DexPool 序列化。
     */
    fun buildMinimalDex(): ByteArray {
        val classDef = ImmutableClassDef(
            "Lcom/example/Test;",                       // type
            1,                                           // accessFlags = PUBLIC
            "Ljava/lang/Object;",                        // superclass
            null,                                        // interfaces
            "Test.java",                                 // sourceFile
            null,                                        // annotations
            null,                                        // fields
            null,                                        // methods
        )
        val dexFile = ImmutableDexFile(Opcodes.getDefault(), listOf(classDef))
        val store = MemoryDataStore()
        DexPool(Opcodes.getDefault()).apply {
            internClass(classDef)
            writeTo(store)
        }
        return store.getData()
    }
}
