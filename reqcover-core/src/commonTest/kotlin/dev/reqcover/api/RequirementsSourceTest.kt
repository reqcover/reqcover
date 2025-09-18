package dev.reqcover.api

import kotlin.test.Test

@RequirementsSource("source1")
@RequirementsSource("source2")
class RequirementsSourceTest {
    @Test
    fun `a test class can be annotated RequirementsSource`() {
        // This test is intentionally empty.
        // That the test class compiles with the @RequirementsSource annotation is the point.
    }

    @RequirementsSource("source3")
    @Test
    fun `a test case can be annotated RequirementsSource`() {
        // This test is intentionally empty.
        // That the test compiles with the @RequirementsSource annotation is the point.
    }

    @RequirementsSource("source4")
    @RequirementsSource("source5")
    @Test
    fun `the RequirementsSource annotation is repeatable`() {
        // This test is intentionally empty.
        // That the test compiles with two @RequirementsSource annotations is the point.
    }
}
