package dev.reqcover.engine

import dev.reqcover.engine.spi.RequirementsCoverageReporter
import kotlin.test.Test
import kotlin.test.assertEquals

class RecordingReporter : RequirementsCoverageReporter {
    override fun report(tracker: RequirementsCoverageTracker) = report(tracker, DEFAULT_REPORTS_DIRECTORY)

    override fun report(tracker: RequirementsCoverageTracker, reportsDirectory: String) {
        reports.add(tracker to reportsDirectory)
    }

    companion object {
        val reports = java.util.concurrent.CopyOnWriteArrayList<Pair<RequirementsCoverageTracker, String>>()
    }
}

class ReporterTest {
    @Test
    fun `passes the reports directory to every reporter on the classpath`() {
        val tracker = RequirementsCoverageTracker()

        report(tracker, "target/reqcover")

        assertEquals(listOf(tracker to "target/reqcover"), RecordingReporter.reports.filter { it.first === tracker })
    }
}
