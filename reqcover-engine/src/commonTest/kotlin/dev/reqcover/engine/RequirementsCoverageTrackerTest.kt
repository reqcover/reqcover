package dev.reqcover.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RequirementsCoverageTrackerTest {
    val requirementsCoverageTracker = RequirementsCoverageTracker()

    @Test
    fun `RequirementsCoverageTracker can track expected requirements`() {
        requirementsCoverageTracker.expect("req-1")
        assertTrue(requirementsCoverageTracker.isExpected("req-1"), "Requirement 'req-1' should be tracked as expected")
    }

    @Test
    fun `RequirementsCoverageTracker can track verified requirements`() {
        requirementsCoverageTracker.verified("req-1")
        assertTrue(requirementsCoverageTracker.isVerified("req-1"), "Requirement 'req-1' should be tracked as verified")
    }

    @Test
    fun `RequirementsCoverageTracker can report expected requirements that are not verified`() {
        requirementsCoverageTracker.expect("req-1")
        assertEquals(setOf("req-1"), requirementsCoverageTracker.unverifiedRequirements())
    }

    @Test
    fun `RequirementsCoverageTracker can report verified requirements that are not expected`() {
        requirementsCoverageTracker.verified("req-1") // Cover a requirement without expecting it
        assertEquals(setOf("req-1"), requirementsCoverageTracker.unexpectedRequirements())
    }
}
