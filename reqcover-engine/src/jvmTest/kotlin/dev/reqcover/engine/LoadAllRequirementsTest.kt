package dev.reqcover.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LoadAllRequirementsTest {
    private fun loader(vararg files: Pair<String, Set<String>>): (String) -> Set<String> = { uri ->
        files.toMap()[uri] ?: throw NotFound(uri)
    }

    @Test
    fun `combines the requirements of all URIs`() {
        val result = loadAllRequirements(
            uris = listOf("a.yaml", "b.yaml"),
            load = loader("a.yaml" to setOf("#one"), "b.yaml" to setOf("#two")),
        )

        assertEquals(setOf("#one", "#two"), result.requirements)
        assertEquals(emptyList(), result.failures)
    }

    @Test
    fun `reports a missing file as a failure naming the URI`() {
        val result = loadAllRequirements(
            uris = listOf("a.yaml", "missing.yaml"),
            load = loader("a.yaml" to setOf("#one")),
        )

        assertEquals(setOf("#one"), result.requirements)
        assertEquals(listOf("missing.yaml"), result.failures.map { it.uri })
        assertEquals("Requirements file not found at URI: missing.yaml", result.failures.single().message)
    }

    @Test
    fun `reports an unsupported format as a failure naming the URI`() {
        val result = loadAllRequirements(
            uris = listOf("bad.yaml"),
            load = { throw UnsupportedFormat(it) },
        )

        assertEquals(listOf("bad.yaml"), result.failures.map { it.uri })
        assertEquals("Unsupported format for requirements file at URI: bad.yaml", result.failures.single().message)
    }

    @Test
    fun `reports any other loading error as a failure naming the URI`() {
        val cause = IllegalStateException("boom")
        val result = loadAllRequirements(
            uris = listOf("broken.yaml"),
            load = { throw cause },
        )

        val failure = result.failures.single()
        assertEquals("broken.yaml", failure.uri)
        assertEquals(cause, failure.cause)
        assertTrue(failure.message.contains("broken.yaml"), failure.message)
        assertTrue(failure.message.contains("boom"), failure.message)
    }

    @Test
    fun `succeeds without any URIs`() {
        val result = loadAllRequirements(uris = emptyList(), load = { error("not called") })

        assertEquals(emptySet(), result.requirements)
        assertEquals(emptyList(), result.failures)
    }
}
