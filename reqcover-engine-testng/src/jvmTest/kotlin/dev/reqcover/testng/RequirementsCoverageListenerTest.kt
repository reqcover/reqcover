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
            listener = RequirementsCoverageListener(
                tracker = tracker,
                requirementsCoverageReporter = {},
                failBuild = {},
                systemProperty = mapOf(
                    "reqCover.requirementsUris" to "requirements-a.yaml, requirements-b.yaml",
                )::get,
            ),
            testClasses = arrayOf(PassingRequirementCase::class.java),
        )

        assertEquals(setOf("#reqOne", "#reqTwo"), tracker.expectedRequirements())
    }

    @Test
    fun `marks only successful annotated tests as verified`() {
        val tracker = RequirementsCoverageTracker()

        executeTests(
            listener = listenerWith(tracker),
            testClasses = arrayOf(PassingRequirementCase::class.java, FailingAndSkippedRequirementCase::class.java),
        )

        assertEquals(setOf("#reqOne"), tracker.verifiedRequirements())
    }

    @Test
    fun `marks every requirement of a test with multiple requirements as verified`() {
        val tracker = RequirementsCoverageTracker()

        executeTests(
            listener = listenerWith(tracker),
            testClasses = arrayOf(MultipleRequirementsCase::class.java),
        )

        assertEquals(setOf("#reqOne", "#reqTwo"), tracker.verifiedRequirements())
    }

    @Test
    fun `warns how to configure requirements when no requirements URIs are configured`() {
        val warnings = capturedWarnings {
            executeTests(
                listener = listenerWith(RequirementsCoverageTracker()),
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
                listener = RequirementsCoverageListener(
                    requirementsCoverageReporter = {},
                    failBuild = {},
                    systemProperty = mapOf("reqCover.requirementsUris" to "requirements-a.yaml")::get,
                ),
                testClasses = arrayOf(PassingRequirementCase::class.java),
            )
        }

        assertEquals(emptyList<String>(), warnings)
    }

    @Test
    fun `does not fail when no requirements are configured but minimum coverage is 100 percent`() {
        val failBuildCalled = AtomicBoolean(false)

        executeTests(
            listener = RequirementsCoverageListener(
                requirementsCoverageReporter = {},
                failBuild = { failBuildCalled.set(true) },
                systemProperty = mapOf("reqCover.minimumRequiredCoveragePercent" to "100.0")::get,
            ),
            testClasses = arrayOf(PassingRequirementCase::class.java),
        )

        assertFalse(failBuildCalled.get())
    }

    private fun capturedWarnings(action: () -> Unit): List<String> {
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
        return records.filter { it.level == Level.WARNING }.map { it.message }
    }

    private fun listenerWith(tracker: RequirementsCoverageTracker): RequirementsCoverageListener =
        RequirementsCoverageListener(
            tracker = tracker,
            requirementsCoverageReporter = {},
            failBuild = {},
            systemProperty = { null },
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
}
