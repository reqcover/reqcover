package dev.reqcover.junit.jupiter

import dev.reqcover.api.ForRequirement
import dev.reqcover.engine.RequirementsCoverageTracker
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.platform.engine.discovery.DiscoverySelectors.selectClass
import org.junit.platform.launcher.core.LauncherConfig
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
import org.junit.platform.launcher.core.LauncherFactory
import java.util.concurrent.atomic.AtomicBoolean

class RequirementsCoverageListenerTest {
    @Test
    fun `loads requirements from reqCover requirementsUris`() {
        val tracker = RequirementsCoverageTracker()

        executeTests(
            listener = RequirementsCoverageListener(
                tracker = tracker,
                requirementsCoverageReporter = {},
                failBuild = {},
            ),
            testClasses = arrayOf(PassingRequirementCase::class.java),
            configurationParameters = mapOf(
                "reqCover.requirementsUris" to "requirements-a.yaml, requirements-b.yaml",
            ),
        )

        assertEquals(
            setOf("#reqOne", "#reqTwo"),
            tracker.expectedRequirements(),
        )
        assertEquals(setOf("#reqOne"), tracker.coveredRequirements())
    }

    @Test
    fun `marks only successful annotated tests as verified`() {
        val tracker = RequirementsCoverageTracker()

        executeTests(
            listener = RequirementsCoverageListener(
                tracker = tracker,
                requirementsCoverageReporter = {},
                failBuild = {},
            ),
            testClasses = arrayOf(PassingRequirementCase::class.java, FailingRequirementCase::class.java),
            configurationParameters = mapOf(
                "reqCover.requirementsUris" to "requirements-a.yaml, requirements-b.yaml",
            ),
        )

        assertTrue(tracker.isVerified("#reqOne"))
        assertFalse(tracker.isVerified("#reqTwo"))
        assertEquals(setOf("#reqTwo"), tracker.unverifiedRequirements())
    }

    @Test
    fun `does not fail when no requirements are configured but minimum coverage is 100 percent`() {
        val failBuildCalled = AtomicBoolean(false)

        executeTests(
            listener = RequirementsCoverageListener(
                requirementsCoverageReporter = {},
                failBuild = { failBuildCalled.set(true) },
            ),
            testClasses = arrayOf(UnannotatedPassingCase::class.java),
            configurationParameters = mapOf(
                "reqCover.minimumRequiredCoveragePercent" to "100.0",
            ),
        )

        assertFalse(failBuildCalled.get())
    }

    @Test
    fun `fails when coverage is below the minimum required threshold`() {
        val failBuildCalled = AtomicBoolean(false)

        executeTests(
            listener = RequirementsCoverageListener(
                requirementsCoverageReporter = {},
                failBuild = { failBuildCalled.set(true) },
            ),
            testClasses = arrayOf(UnannotatedPassingCase::class.java),
            configurationParameters = mapOf(
                "reqCover.requirementsUris" to "requirements-a.yaml",
                "reqCover.minimumRequiredCoveragePercent" to "100.0",
            ),
        )

        assertTrue(failBuildCalled.get())
    }

    @Test
    fun `fails when removed reqCover requirementsUri property is configured`() {
        val failBuildCalled = AtomicBoolean(false)

        executeTests(
            listener = RequirementsCoverageListener(
                requirementsCoverageReporter = {},
                failBuild = { failBuildCalled.set(true) },
            ),
            testClasses = arrayOf(UnannotatedPassingCase::class.java),
            configurationParameters = mapOf(
                "reqCover.requirementsUri" to "requirements-a.yaml",
            ),
        )

        assertTrue(failBuildCalled.get())
    }

    private fun executeTests(
        listener: RequirementsCoverageListener,
        testClasses: Array<Class<*>>,
        configurationParameters: Map<String, String>,
    ) {
        val requestBuilder = LauncherDiscoveryRequestBuilder.request()
            .configurationParameter("junit.jupiter.conditions.deactivate", "org.junit.*DisabledCondition")
        configurationParameters.forEach { (key, value) ->
            requestBuilder.configurationParameter(key, value)
        }
        testClasses.forEach { testClass ->
            requestBuilder.selectors(selectClass(testClass))
        }

        LauncherFactory.create(
            LauncherConfig.builder()
                .enableTestExecutionListenerAutoRegistration(false)
                .build(),
        ).execute(requestBuilder.build(), listener)
    }

    @Disabled("Executed via the embedded JUnit launcher in RequirementsCoverageListenerTest")
    class PassingRequirementCase {
        @Test
        @ForRequirement("#reqOne")
        fun passes() {
        }
    }

    @Disabled("Executed via the embedded JUnit launcher in RequirementsCoverageListenerTest")
    class FailingRequirementCase {
        @Test
        @ForRequirement("#reqTwo")
        fun fails() {
            throw AssertionError("boom")
        }
    }

    @Disabled("Executed via the embedded JUnit launcher in RequirementsCoverageListenerTest")
    class UnannotatedPassingCase {
        @Test
        fun passes() {
        }
    }
}
