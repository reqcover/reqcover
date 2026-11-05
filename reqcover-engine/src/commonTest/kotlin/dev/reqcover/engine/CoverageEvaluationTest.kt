package dev.reqcover.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CoverageEvaluationTest {
    private fun trackerWith(expected: Set<String>, verified: Set<String>): RequirementsCoverageTracker =
        RequirementsCoverageTracker().apply {
            expectAll(expected)
            verified.forEach { verified(it) }
        }

    private fun halfCoveredTracker(): RequirementsCoverageTracker =
        trackerWith(expected = setOf("#reqOne", "#reqTwo"), verified = setOf("#reqOne", "#unexpected"))

    @Test
    fun `coverage is 100 percent when no requirements are expected`() {
        val outcome = evaluateCoverage(trackerWith(expected = emptySet(), verified = emptySet()), CoveragePolicy())

        assertEquals(100.0, outcome.coveragePercent)
        assertFalse(outcome.isBelowMinimum)
    }

    @Test
    fun `coverage counts only verified requirements that are expected`() {
        val outcome = evaluateCoverage(halfCoveredTracker(), CoveragePolicy())

        assertEquals(1, outcome.coveredRequirements)
        assertEquals(2, outcome.expectedRequirements)
        assertEquals(50.0, outcome.coveragePercent)
    }

    @Test
    fun `fails the build when coverage is below the minimum`() {
        val outcome = evaluateCoverage(
            halfCoveredTracker(),
            CoveragePolicy(minimumRequiredCoveragePercent = 100.0, failIfBelowMinimum = true),
        )

        assertTrue(outcome.isBelowMinimum)
        assertTrue(outcome.shouldFailBuild)
    }

    @Test
    fun `does not fail the build below the minimum when failing is disabled`() {
        val outcome = evaluateCoverage(
            halfCoveredTracker(),
            CoveragePolicy(minimumRequiredCoveragePercent = 100.0, failIfBelowMinimum = false),
        )

        assertTrue(outcome.isBelowMinimum)
        assertFalse(outcome.shouldFailBuild)
    }

    @Test
    fun `coverage equal to the minimum is not below the minimum`() {
        val outcome = evaluateCoverage(halfCoveredTracker(), CoveragePolicy(minimumRequiredCoveragePercent = 50.0))

        assertFalse(outcome.isBelowMinimum)
        assertFalse(outcome.shouldFailBuild)
    }

    @Test
    fun `policy defaults to no minimum and failing the build when below it`() {
        assertEquals(
            CoveragePolicy(minimumRequiredCoveragePercent = 0.0, failIfBelowMinimum = true),
            CoveragePolicy.parse(minimumRequiredCoveragePercent = null, failIfBelowMinimum = null),
        )
    }

    @Test
    fun `default policy is the same as an unconfigured policy`() {
        assertEquals(
            CoveragePolicy.parse(minimumRequiredCoveragePercent = null, failIfBelowMinimum = null),
            CoveragePolicy(),
        )
    }

    @Test
    fun `policy parses configured values`() {
        assertEquals(
            CoveragePolicy(minimumRequiredCoveragePercent = 80.5, failIfBelowMinimum = false),
            CoveragePolicy.parse(minimumRequiredCoveragePercent = "80.5", failIfBelowMinimum = "false"),
        )
    }

    @Test
    fun `policy treats an unparseable minimum as zero`() {
        assertEquals(
            0.0,
            CoveragePolicy.parse(minimumRequiredCoveragePercent = "lots", failIfBelowMinimum = "true")
                .minimumRequiredCoveragePercent,
        )
    }
}
