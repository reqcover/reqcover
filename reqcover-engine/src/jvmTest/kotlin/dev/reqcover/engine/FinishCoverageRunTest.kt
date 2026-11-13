package dev.reqcover.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FinishCoverageRunTest {
    private class RecordingLog : CoverageRunLog {
        val messages = mutableListOf<Pair<String, String>>()
        override fun info(message: String) { messages.add("info" to message) }
        override fun warning(message: String) { messages.add("warning" to message) }
        override fun error(message: String) { messages.add("error" to message) }
        fun at(level: String): List<String> = messages.filter { it.first == level }.map { it.second }
    }

    private class Run(val log: RecordingLog, val events: List<String>)

    private fun tracker(expected: Set<String>, verified: Set<String>): RequirementsCoverageTracker =
        RequirementsCoverageTracker().apply {
            expectAll(expected)
            verified.forEach(::verified)
        }

    private fun finish(
        tracker: RequirementsCoverageTracker,
        policy: CoveragePolicy = CoveragePolicy(),
    ): Run {
        val log = RecordingLog()
        val events = mutableListOf<String>()
        finishCoverageRun(
            tracker = tracker,
            policy = policy,
            report = { events.add("report") },
            failBuild = { events.add("failBuild") },
            log = log,
        )
        return Run(log, events)
    }

    @Test
    fun `warns about requirements that no test covered`() {
        val run = finish(tracker(expected = setOf("#a", "#b"), verified = setOf("#a")))

        assertTrue(run.log.at("warning").contains("The following requirements were not covered by any test: [#b]"), "${run.log.messages}")
    }

    @Test
    fun `says so when all requirements are covered`() {
        val run = finish(tracker(expected = setOf("#a"), verified = setOf("#a")))

        assertTrue(run.log.at("info").contains("All requirements are covered by tests."), "${run.log.messages}")
        assertEquals(emptyList(), run.log.at("warning"))
    }

    @Test
    fun `warns about covered requirements that are not in the specification`() {
        val run = finish(tracker(expected = setOf("#a"), verified = setOf("#a", "#x")))

        assertTrue(
            run.log.at("warning").contains("The following covered requirements are not defined in the requirements specification: [#x]"),
            "${run.log.messages}",
        )
    }

    @Test
    fun `logs the coverage`() {
        val run = finish(tracker(expected = setOf("#a", "#b", "#c"), verified = setOf("#a")))

        assertTrue(run.log.at("info").contains("Requirements coverage: 1 out of 3 = 33.3%"), "${run.log.messages}")
    }

    @Test
    fun `reports and then fails the build when coverage is below the minimum`() {
        val run = finish(
            tracker(expected = setOf("#a", "#b"), verified = setOf("#a")),
            CoveragePolicy(minimumRequiredCoveragePercent = 100.0),
        )

        assertEquals(listOf("report", "failBuild"), run.events)
        assertEquals(
            listOf(
                "Requirements coverage is below the minimum required coverage of 100.0%. Actual coverage: 50.0%",
                "Exiting with error code 1 due to insufficient requirements coverage.",
            ),
            run.log.at("error"),
        )
    }

    @Test
    fun `reports but does not fail the build below the minimum when failIfBelowMinimum is false`() {
        val run = finish(
            tracker(expected = setOf("#a", "#b"), verified = setOf("#a")),
            CoveragePolicy(minimumRequiredCoveragePercent = 100.0, failIfBelowMinimum = false),
        )

        assertEquals(listOf("report"), run.events)
        assertEquals(
            listOf("Requirements coverage is below the minimum required coverage of 100.0%. Actual coverage: 50.0%"),
            run.log.at("error"),
        )
    }

    @Test
    fun `reports and confirms when coverage meets the minimum`() {
        val run = finish(
            tracker(expected = setOf("#a", "#b"), verified = setOf("#a")),
            CoveragePolicy(minimumRequiredCoveragePercent = 50.0),
        )

        assertEquals(listOf("report"), run.events)
        assertTrue(
            run.log.at("info").contains("Requirements coverage meets the minimum required coverage of 50.0%. Actual coverage: 50.0%"),
            "${run.log.messages}",
        )
        assertEquals(emptyList(), run.log.at("error"))
    }
}
