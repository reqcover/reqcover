package dev.reqcover.engine.spi

import dev.reqcover.engine.RequirementsCoverageTracker
import kotlin.test.Test
import kotlin.test.assertEquals

class RequirementsCoverageReporterTest {
    @Test
    fun `a reporter that does not support a reports directory still reports`() {
        val reported = mutableListOf<RequirementsCoverageTracker>()
        val reporter = object : RequirementsCoverageReporter {
            override fun report(tracker: RequirementsCoverageTracker) {
                reported.add(tracker)
            }
        }
        val tracker = RequirementsCoverageTracker()

        reporter.report(tracker, "target/reqcover")

        assertEquals(listOf(tracker), reported)
    }
}
