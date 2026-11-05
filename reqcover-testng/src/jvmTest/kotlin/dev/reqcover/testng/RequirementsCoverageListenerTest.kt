package dev.reqcover.testng

import dev.reqcover.api.ForRequirement
import dev.reqcover.engine.RequirementsCoverageTracker
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
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
}
