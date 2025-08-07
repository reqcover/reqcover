package dev.reqcover.api

import kotlin.test.Test

class ForRequirementTest {
    @ForRequirement("req-1")
    @Test
    fun `a test case can be annotated ForRequirement`() {
        // This test is intentionally empty.
        // That the test compiles with the @ForRequirement annotation is the point.
    }

    @ForRequirement("req-1")
    @ForRequirement("req-2")
    @Test
    fun `the ForRequirement annotation is repeatable`() {
        // This test is intentionally empty.
        // That the test compiles with two @ForRequirement annotations is the point.
    }
}
