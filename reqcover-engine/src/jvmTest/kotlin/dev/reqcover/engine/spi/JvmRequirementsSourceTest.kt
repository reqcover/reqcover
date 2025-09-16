package dev.reqcover.engine.spi

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class JvmRequirementsSourceTest {
    object TestSource : JvmRequirementsSource {
        override fun load(inputStream: java.io.InputStream): Set<String> {
            return inputStream.bufferedReader().readLines().toSet()
        }
    }

    @Test
    fun `throws NotFound when the requested resource is missing`() {
        assertThrows<dev.reqcover.engine.NotFound> {
            TestSource.load("nonexistent_resource.txt")
        }
    }

    @Test
    fun `returns the strings from all sources when there are two sources on the classpath`() {
        assertEquals(setOf("REQ-1", "REQ-2"), TestSource.load("sample-requirements.txt"))
    }
}
