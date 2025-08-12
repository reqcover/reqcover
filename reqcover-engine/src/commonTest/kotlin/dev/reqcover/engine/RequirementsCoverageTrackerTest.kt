package dev.reqcover.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RequirementsCoverageTrackerTest {
    val requirementsCoverageTracker = RequirementsCoverageTracker()

    @Test
    fun `Requirements are not implicitly tracked or verified`() {
        assertFalse(requirementsCoverageTracker.isExpected("req-1"), "Requirement 'req-1' should not be tracked as expected by default")
        assertFalse(requirementsCoverageTracker.isVerified("req-1"), "Requirement 'req-1' should not be tracked as verified by default")
    }

    @Test
    fun `RequirementsCoverageTracker can track expected requirements`() {
        requirementsCoverageTracker.expect("req-1")
        requirementsCoverageTracker.expect("req-2")
        assertTrue(requirementsCoverageTracker.isExpected("req-1"), "Requirement 'req-1' should be tracked as expected")
        assertTrue(requirementsCoverageTracker.isExpected("req-2"), "Requirement 'req-1' should be tracked as expected")
    }

    @Test
    fun `RequirementsCoverageTracker can track multiple expected requirements`() {
        requirementsCoverageTracker.expectAll(listOf("req-1", "req-2", "req-3"))
        assertTrue(requirementsCoverageTracker.isExpected("req-1"), "Requirement 'req-1' should be tracked as expected")
        assertTrue(requirementsCoverageTracker.isExpected("req-2"), "Requirement 'req-2' should be tracked as expected")
        assertTrue(requirementsCoverageTracker.isExpected("req-3"), "Requirement 'req-3' should be tracked as expected")
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
