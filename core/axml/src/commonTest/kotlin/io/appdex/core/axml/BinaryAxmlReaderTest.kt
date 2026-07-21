package io.appdex.core.axml

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class BinaryAxmlReaderTest {

    @Test
    fun `reads minimal manifest`() {
        val bytes = AxmlSample.buildMinimalManifest()
        val doc = BinaryAxmlReader().read(bytes)
        assertTrue(doc.xml.contains("<manifest"))
        assertTrue(doc.xml.contains("</manifest>"))
    }

    @Test
    fun `reads element with attribute`() {
        val bytes = AxmlSample.buildWithAttribute()
        val doc = BinaryAxmlReader().read(bytes)
        assertTrue(doc.xml.contains("<element"))
        assertTrue(doc.xml.contains("attr=\"value\""))
    }
}
