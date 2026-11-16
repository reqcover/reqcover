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
        configurationProblems: List<String> = emptyList(),
    ): Run {
        val log = RecordingLog()
        val events = mutableListOf<String>()
        finishCoverageRun(
            tracker = tracker,
            policy = policy,
            report = { events.add("report") },
            failBuild = { events.add("failBuild") },
            log = log,
            configurationProblems = configurationProblems,
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

    @Test
    fun `reports and then fails the build when the configuration has problems`() {
        val run = finish(
            tracker(expected = setOf("#a"), verified = setOf("#a")),
            configurationProblems = listOf("Invalid value '100%' for reqCover.minimumRequiredCoveragePercent: expected a number from 0 to 100."),
        )

        assertEquals(listOf("report", "failBuild"), run.events)
        assertEquals(
            listOf(
                "Invalid value '100%' for reqCover.minimumRequiredCoveragePercent: expected a number from 0 to 100.",
                "Exiting with error code 1 due to invalid ReqCover configuration.",
            ),
            run.log.at("error"),
        )
    }

    @Test
    fun `fails the build only once when the configuration has problems and coverage is low`() {
        val run = finish(
            tracker(expected = setOf("#a", "#b"), verified = setOf("#a")),
            CoveragePolicy(minimumRequiredCoveragePercent = 100.0),
            configurationProblems = listOf("Invalid value 'yes' for reqCover.failIfBelowMinimum: expected true or false."),
        )

        assertEquals(listOf("report", "failBuild"), run.events)
    }

    @Test
    fun `logs the coverage with a dot as decimal separator whatever the default locale`() {
        val defaultLocale = java.util.Locale.getDefault()
        java.util.Locale.setDefault(java.util.Locale.GERMANY)
        try {
            val run = finish(
                tracker(expected = setOf("#a", "#b"), verified = setOf("#a")),
                CoveragePolicy(minimumRequiredCoveragePercent = 100.0),
            )

            assertTrue(run.log.at("info").contains("Requirements coverage: 1 out of 2 = 50.0%"), "${run.log.messages}")
            assertTrue(
                run.log.at("error").contains("Requirements coverage is below the minimum required coverage of 100.0%. Actual coverage: 50.0%"),
                "${run.log.messages}",
            )
        } finally {
            java.util.Locale.setDefault(defaultLocale)
        }
    }
}
