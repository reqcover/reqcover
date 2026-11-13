package dev.reqcover.engine.spi

import dev.reqcover.engine.RequirementsCoverageTracker

interface RequirementsCoverageReporter {
    fun report(tracker: RequirementsCoverageTracker)

    fun report(tracker: RequirementsCoverageTracker, reportsDirectory: String) = report(tracker)
}
