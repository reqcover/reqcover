package dev.reqcover.engine

import kotlin.test.Test
import kotlin.test.assertEquals

class RequirementsConfigurationTest {
    @Test
    fun `splits comma-separated requirements URIs and trims whitespace`() {
        assertEquals(listOf("a.yaml", "b.yaml"), parseRequirementsUris("a.yaml, b.yaml"))
    }

    @Test
    fun `ignores blank entries in requirements URIs`() {
        assertEquals(listOf("a.yaml", "b.yaml"), parseRequirementsUris(" a.yaml ,, b.yaml, \t"))
    }

    @Test
    fun `returns no requirements URIs for blank input`() {
        assertEquals(emptyList(), parseRequirementsUris("   "))
    }
}
