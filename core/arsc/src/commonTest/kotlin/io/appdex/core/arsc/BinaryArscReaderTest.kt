package io.appdex.core.arsc

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class BinaryArscReaderTest {

    @Test
    fun `parses minimal arsc`() {
        val bytes = ArscSample.buildMinimal()
        val table = BinaryArscReader().read(bytes)
        assertEquals(1, table.packages.size)
        assertEquals(1, table.packages[0].id)
        assertEquals("io.appdex.test", table.packages[0].name)
    }
}
