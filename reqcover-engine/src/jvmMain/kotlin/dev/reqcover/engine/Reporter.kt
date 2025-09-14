package dev.reqcover.engine

import dev.reqcover.engine.spi.RequirementsCoverageReporter
import java.util.ServiceLoader

fun report(tracker: RequirementsCoverageTracker) {
    val serviceLoader = ServiceLoader.load(RequirementsCoverageReporter::class.java)
    var any: Boolean = false
    serviceLoader.forEach { reporter ->
        any = true
        reporter.report(tracker)
    }
    if (!any) {
        System.err.println("No reporter configured. Please add a RequirementsCoverageReporter implementation to the classpath.")
    }
}
