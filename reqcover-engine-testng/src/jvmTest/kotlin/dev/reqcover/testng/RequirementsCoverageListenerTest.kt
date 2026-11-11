package dev.reqcover.testng

import dev.reqcover.api.ForRequirement
import dev.reqcover.engine.RequirementsCoverageTracker
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.testng.SkipException
import org.testng.TestNG

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
