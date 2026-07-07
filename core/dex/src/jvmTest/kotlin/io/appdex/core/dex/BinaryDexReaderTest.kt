package io.appdex.core.dex

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class BinaryDexReaderTest {

    @Test
    fun `lists classes of minimal dex`() {
        val bytes = DexSample.buildMinimalDex()
        val reader = BinaryDexReader()
        val classes = reader.listClasses(bytes)
        assertEquals(1, classes.size)
        val cls = classes[0]
        assertEquals("Lcom/example/Test;", cls.type)
        assertEquals("Test", cls.name)
        assertEquals("Ljava/lang/Object;", cls.superclass)
        assertEquals("Test.java", cls.sourceFile)
        assertTrue(cls.fields.isEmpty())
        assertTrue(cls.methods.isEmpty())
    }

    @Test
    fun `lists fields`() {
        val bytes = DexSample.buildDexWithField()
        val reader = BinaryDexReader()
        val classes = reader.listClasses(bytes)
        assertEquals(1, classes.size)
        val cls = classes[0]
        assertEquals(1, cls.fields.size)
        val field = cls.fields[0]
        assertEquals("count", field.name)
        assertEquals("I", field.type)
    }

    @Test
    fun `lists methods`() {
        val bytes = DexSample.buildDexWithMethod()
        val reader = BinaryDexReader()
        val classes = reader.listClasses(bytes)
        assertEquals(1, classes.size)
        val cls = classes[0]
        assertEquals(1, cls.methods.size)
        val method = cls.methods[0]
        assertEquals("hello", method.name)
        assertEquals("V", method.returnType)
        assertTrue(method.parameterTypes.isEmpty())
    }
}
