package dev.reqcover.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReqCoverConfigurationTest {
    private fun configuration(vararg sources: Map<String, String>) =
        ReqCoverConfiguration(sources.map { source -> { key: String -> source[key] } })

    @Test
    fun `the first source that has a setting wins`() {
        val config = configuration(
            mapOf("reqCover.requirementsUris" to "from-system.yaml"),
            mapOf("reqCover.requirementsUris" to "from-framework.yaml"),
        )

        assertEquals("from-system.yaml", config.value("reqCover.requirementsUris"))
    }

    @Test
    fun `a later source is used when earlier sources do not have the setting`() {
        val config = configuration(
            emptyMap(),
            mapOf("reqCover.requirementsUris" to "from-framework.yaml"),
        )

        assertEquals("from-framework.yaml", config.value("reqCover.requirementsUris"))
    }

    @Test
    fun `a setting that no source has is absent`() {
        assertNull(configuration(emptyMap(), emptyMap()).value("reqCover.requirementsUris"))
    }

    @Test
    fun `requirements URIs are parsed from the winning source`() {
        val config = configuration(
            mapOf("reqCover.requirementsUris" to "a.yaml, b.yaml"),
            mapOf("reqCover.requirementsUris" to "c.yaml"),
        )

        assertEquals(listOf("a.yaml", "b.yaml"), config.requirementsUris())
    }

    @Test
    fun `requirements URIs are empty when not configured`() {
        assertEquals(emptyList(), configuration().requirementsUris())
    }

    @Test
    fun `each coverage policy setting comes from the first source that has it`() {
        val config = configuration(
            mapOf("reqCover.minimumRequiredCoveragePercent" to "80"),
            mapOf(
                "reqCover.minimumRequiredCoveragePercent" to "50",
                "reqCover.failIfBelowMinimum" to "false",
            ),
        )

        assertEquals(
            CoveragePolicy(minimumRequiredCoveragePercent = 80.0, failIfBelowMinimum = false),
            config.coveragePolicy(),
        )
    }

    @Test
    fun `the coverage policy uses the defaults when nothing is configured`() {
        assertEquals(CoveragePolicy(), configuration().coveragePolicy())
    }
}
