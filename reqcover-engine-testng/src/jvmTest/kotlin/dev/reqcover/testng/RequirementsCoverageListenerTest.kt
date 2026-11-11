package dev.reqcover.testng

import dev.reqcover.api.ForRequirement
import dev.reqcover.engine.RequirementsCoverageTracker
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.testng.SkipException
import org.testng.TestNG
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.LogRecord
import java.util.logging.Logger

class RequirementsCoverageListenerTest {
    @Test
    fun `loads requirements from reqCover requirementsUris`() {
        val tracker = RequirementsCoverageTracker()

        executeTests(
            listener = listener(
                tracker = tracker,
                properties = mapOf("reqCover.requirementsUris" to "requirements-a.yaml, requirements-b.yaml"),
            ),
            testClasses = arrayOf(PassingRequirementCase::class.java),
        )

        assertEquals(setOf("#reqOne", "#reqTwo"), tracker.expectedRequirements())
    }

    @Test
    fun `marks only successful annotated tests as verified`() {
        val tracker = RequirementsCoverageTracker()

        executeTests(
            listener = listener(tracker = tracker),
            testClasses = arrayOf(PassingRequirementCase::class.java, FailingAndSkippedRequirementCase::class.java),
        )

        assertEquals(setOf("#reqOne"), tracker.verifiedRequirements())
    }

    @Test
    fun `marks every requirement of a test with multiple requirements as verified`() {
        val tracker = RequirementsCoverageTracker()

        executeTests(
            listener = listener(tracker = tracker),
            testClasses = arrayOf(MultipleRequirementsCase::class.java),
        )

        assertEquals(setOf("#reqOne", "#reqTwo"), tracker.verifiedRequirements())
    }

    @Test
    fun `warns how to configure requirements when no requirements URIs are configured`() {
        val warnings = capturedWarnings {
            executeTests(
                listener = listener(),
                testClasses = arrayOf(PassingRequirementCase::class.java),
            )
        }

        assertTrue(
            warnings.any { it.contains("-DreqCover.requirementsUris=") },
            "Expected a warning explaining how to configure reqCover.requirementsUris, but got: $warnings",
        )
    }

    @Test
    fun `does not warn when requirements URIs are configured`() {
        val warnings = capturedWarnings {
            executeTests(
                listener = listener(properties = mapOf("reqCover.requirementsUris" to "requirements-a.yaml")),
                testClasses = arrayOf(PassingRequirementCase::class.java),
            )
        }

        assertEquals(emptyList<String>(), warnings)
    }

    @Test
    fun `does not fail when no requirements are configured but minimum coverage is 100 percent`() {
        val failBuildCalled = AtomicBoolean(false)

        executeTests(
            listener = listener(
                failBuild = { failBuildCalled.set(true) },
                properties = mapOf("reqCover.minimumRequiredCoveragePercent" to "100.0"),
            ),
            testClasses = arrayOf(PassingRequirementCase::class.java),
        )

        assertFalse(failBuildCalled.get())
    }

    @Test
    fun `fails when coverage is below the minimum required threshold`() {
        val failBuildCalled = AtomicBoolean(false)

        val errors = capturedMessages(Level.SEVERE) {
            executeTests(
                listener = listener(
                    failBuild = { failBuildCalled.set(true) },
                    properties = mapOf(
                        "reqCover.requirementsUris" to "requirements-a.yaml",
                        "reqCover.minimumRequiredCoveragePercent" to "100.0",
                    ),
                ),
                testClasses = arrayOf(UnannotatedPassingCase::class.java),
            )
        }

        assertTrue(failBuildCalled.get())
        assertTrue(
            errors.any { it.startsWith("Requirements coverage is below the minimum required coverage of 100.0%") },
            "Expected an error about insufficient coverage, but got: $errors",
        )
    }

    @Test
    fun `writes the coverage report before failing the build`() {
        val events = mutableListOf<String>()

        executeTests(
            listener = listener(
                requirementsCoverageReporter = { events.add("report") },
                failBuild = { events.add("failBuild") },
                properties = mapOf(
                    "reqCover.requirementsUris" to "requirements-a.yaml",
                    "reqCover.minimumRequiredCoveragePercent" to "100.0",
                ),
            ),
            testClasses = arrayOf(UnannotatedPassingCase::class.java),
        )

        assertEquals(listOf("report", "failBuild"), events)
    }

    @Test
    fun `fails when a configured requirements URI cannot be found`() {
        val failBuildCalled = AtomicBoolean(false)

        val errors = capturedMessages(Level.SEVERE) {
            executeTests(
                listener = listener(
                    failBuild = { failBuildCalled.set(true) },
                    properties = mapOf("reqCover.requirementsUris" to "requirements-a.yaml, does-not-exist.yaml"),
                ),
                testClasses = arrayOf(UnannotatedPassingCase::class.java),
            )
        }

        assertTrue(failBuildCalled.get())
        assertTrue(
            errors.any { it.contains("does-not-exist.yaml") && it.contains("reqCover.requirementsUris") },
            "Expected an error naming the missing URI and the property to check, but got: $errors",
        )
    }

    @Test
    fun `does not fail below the minimum when failIfBelowMinimum is false`() {
        val failBuildCalled = AtomicBoolean(false)

        executeTests(
            listener = listener(
                failBuild = { failBuildCalled.set(true) },
                properties = mapOf(
                    "reqCover.requirementsUris" to "requirements-a.yaml",
                    "reqCover.minimumRequiredCoveragePercent" to "100.0",
                    "reqCover.failIfBelowMinimum" to "false",
                ),
            ),
            testClasses = arrayOf(UnannotatedPassingCase::class.java),
        )

        assertFalse(failBuildCalled.get())
    }

    @Test
    fun `does not fail when coverage meets the minimum required threshold`() {
        val failBuildCalled = AtomicBoolean(false)

        executeTests(
            listener = listener(
                failBuild = { failBuildCalled.set(true) },
                properties = mapOf(
                    "reqCover.requirementsUris" to "requirements-a.yaml",
                    "reqCover.minimumRequiredCoveragePercent" to "100.0",
                ),
            ),
            testClasses = arrayOf(PassingRequirementCase::class.java),
        )

        assertFalse(failBuildCalled.get())
    }

    @Test
    fun `reports the coverage once at the end of the run`() {
        val tracker = RequirementsCoverageTracker()
        val reportedTrackers = mutableListOf<RequirementsCoverageTracker>()

        executeTests(
            listener = listener(
                tracker = tracker,
                requirementsCoverageReporter = { reportedTrackers.add(it) },
                properties = mapOf("reqCover.requirementsUris" to "requirements-a.yaml"),
            ),
            testClasses = arrayOf(PassingRequirementCase::class.java),
        )

        assertEquals(listOf(tracker), reportedTrackers)
        assertEquals(setOf("#reqOne"), reportedTrackers.single().coveredRequirements())
    }

    private fun capturedWarnings(action: () -> Unit): List<String> = capturedMessages(Level.WARNING, action)

    private fun capturedMessages(level: Level, action: () -> Unit): List<String> {
        val logger = Logger.getLogger(RequirementsCoverageListener::class.java.name)
        val records = mutableListOf<LogRecord>()
        val handler = object : Handler() {
            override fun publish(record: LogRecord) {
                records.add(record)
            }

            override fun flush() {}

            override fun close() {}
        }
        logger.addHandler(handler)
        try {
            action()
        } finally {
            logger.removeHandler(handler)
        }
        return records.filter { it.level == level }.map { it.message }
    }

    private fun listener(
        properties: Map<String, String> = emptyMap(),
        tracker: RequirementsCoverageTracker = RequirementsCoverageTracker(),
        failBuild: () -> Unit = {},
        requirementsCoverageReporter: (RequirementsCoverageTracker) -> Unit = {},
    ): RequirementsCoverageListener =
        RequirementsCoverageListener(
            tracker = tracker,
            requirementsCoverageReporter = requirementsCoverageReporter,
            failBuild = failBuild,
            systemProperty = properties::get,
        )

    private fun executeTests(listener: RequirementsCoverageListener, testClasses: Array<Class<*>>) {
        TestNG().apply {
            setUseDefaultListeners(false)
            setVerbose(0)
            setTestClasses(testClasses)
            addListener(listener)
        }.run()
    }

    class PassingRequirementCase {
        @org.testng.annotations.Test
        @ForRequirement("#reqOne")
        fun passes() {
        }
    }

    class FailingAndSkippedRequirementCase {
        @org.testng.annotations.Test
        @ForRequirement("#reqTwo")
        fun fails() {
            throw AssertionError("boom")
        }

        @org.testng.annotations.Test
        @ForRequirement("#reqThree")
        fun isSkipped() {
            throw SkipException("skipped")
        }
    }

    class MultipleRequirementsCase {
        @org.testng.annotations.Test
        @ForRequirement("#reqOne")
        @ForRequirement("#reqTwo")
        fun passes() {
        }
    }

    class UnannotatedPassingCase {
        @org.testng.annotations.Test
        fun passes() {
        }
    }
}
