package dev.reqcover.cucumber

import dev.reqcover.engine.RequirementsCoverageTracker
import io.cucumber.core.options.CommandlineOptionsParser
import io.cucumber.core.runtime.Runtime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicBoolean

class RequirementsCoverageListenerTest {
    @Test
    fun `a passing scenario covers the requirements of its tags`() {
        val tracker = RequirementsCoverageTracker()

        run(listener(tracker = tracker), "coverage.feature")

        assertEquals(setOf("#reqOne"), tracker.verifiedRequirements())
    }

    @Test
    fun `a requirement tag on the feature applies to its scenarios`() {
        val tracker = RequirementsCoverageTracker()

        run(listener(tracker = tracker), "feature-tag.feature")

        assertEquals(setOf("#reqOne"), tracker.verifiedRequirements())
    }

    @Test
    fun `loads the expected requirements from reqCover requirementsUris`() {
        val tracker = RequirementsCoverageTracker()

        run(
            listener(tracker = tracker, systemProperties = mapOf("reqCover.requirementsUris" to "requirements-a.yaml, requirements-b.yaml")),
            "coverage.feature",
        )

        assertEquals(setOf("#reqOne", "#reqTwo"), tracker.expectedRequirements())
        assertEquals(setOf("#reqOne"), tracker.coveredRequirements())
    }

    @Test
    fun `the tag prefix is configurable, and then only that prefix counts`() {
        val tracker = RequirementsCoverageTracker()

        run(
            listener(tracker = tracker, systemProperties = mapOf("reqCover.cucumber.tagPrefix" to "@requirement=")),
            "custom-prefix.feature",
            "coverage.feature",
        )

        assertEquals(setOf("#reqThree"), tracker.verifiedRequirements())
    }

    @Test
    fun `a scenario without requirement tags covers nothing`() {
        val tracker = RequirementsCoverageTracker()

        run(listener(tracker = tracker), "untagged.feature")

        assertEquals(emptySet<String>(), tracker.verifiedRequirements())
    }

    @Test
    fun `reads settings from cucumber properties when no system property is set`() {
        val tracker = RequirementsCoverageTracker()

        run(
            listener(tracker = tracker, cucumberProperties = mapOf("reqCover.requirementsUris" to "requirements-a.yaml")),
            "coverage.feature",
        )

        assertEquals(setOf("#reqOne"), tracker.expectedRequirements())
    }

    @Test
    fun `a system property takes precedence over cucumber properties`() {
        val tracker = RequirementsCoverageTracker()

        run(
            listener(
                tracker = tracker,
                systemProperties = mapOf("reqCover.requirementsUris" to "requirements-b.yaml"),
                cucumberProperties = mapOf("reqCover.requirementsUris" to "requirements-a.yaml"),
            ),
            "coverage.feature",
        )

        assertEquals(setOf("#reqTwo"), tracker.expectedRequirements())
    }

    @Test
    fun `reads settings from junit-platform properties when nothing else sets them`() {
        val tracker = RequirementsCoverageTracker()

        run(
            listener(tracker = tracker, junitPlatformProperties = mapOf("reqCover.requirementsUris" to "requirements-a.yaml")),
            "coverage.feature",
        )

        assertEquals(setOf("#reqOne"), tracker.expectedRequirements())
    }

    @Test
    fun `cucumber properties take precedence over junit-platform properties`() {
        val tracker = RequirementsCoverageTracker()

        run(
            listener(
                tracker = tracker,
                cucumberProperties = mapOf("reqCover.requirementsUris" to "requirements-b.yaml"),
                junitPlatformProperties = mapOf("reqCover.requirementsUris" to "requirements-a.yaml"),
            ),
            "coverage.feature",
        )

        assertEquals(setOf("#reqTwo"), tracker.expectedRequirements())
    }

    @Test
    fun `fails when a configured requirements file cannot be found`() {
        val failBuildCalled = AtomicBoolean(false)

        run(
            listener(failBuild = { failBuildCalled.set(true) }, systemProperties = mapOf("reqCover.requirementsUris" to "does-not-exist.yaml")),
            "coverage.feature",
        )

        assertTrue(failBuildCalled.get())
    }

    @Test
    fun `reports to the configured directory and then fails the build below the minimum`() {
        val events = mutableListOf<String>()

        run(
            listener(
                requirementsCoverageReporter = { _, reportsDirectory -> events.add("report $reportsDirectory") },
                failBuild = { events.add("failBuild") },
                systemProperties = mapOf(
                    "reqCover.requirementsUris" to "requirements-a.yaml, requirements-b.yaml",
                    "reqCover.minimumRequiredCoveragePercent" to "100",
                    "reqCover.reportsDir" to "target/reqcover",
                ),
            ),
            "coverage.feature",
        )

        assertEquals(listOf("report target/reqcover", "failBuild"), events)
    }

    @Test
    fun `does not fail the build when coverage meets the minimum`() {
        val failBuildCalled = AtomicBoolean(false)

        run(
            listener(
                failBuild = { failBuildCalled.set(true) },
                systemProperties = mapOf(
                    "reqCover.requirementsUris" to "requirements-a.yaml",
                    "reqCover.minimumRequiredCoveragePercent" to "100",
                ),
            ),
            "coverage.feature",
        )

        assertFalse(failBuildCalled.get())
    }

    @Test
    fun `Cucumber can create the listener from its class name`() {
        val options = CommandlineOptionsParser(System.out)
            .parse(
                "--plugin", RequirementsCoverageListener::class.java.name,
                "--glue", "dev.reqcover.cucumber.glue",
                "classpath:features/untagged.feature",
            )
            .build()
        val runtime = Runtime.builder().withRuntimeOptions(options).build()

        runtime.run()

        assertEquals(0, runtime.exitStatus().toInt())
    }

    private fun listener(
        tracker: RequirementsCoverageTracker = RequirementsCoverageTracker(),
        requirementsCoverageReporter: (RequirementsCoverageTracker, String) -> Unit = { _, _ -> },
        failBuild: () -> Unit = {},
        systemProperties: Map<String, String> = emptyMap(),
        cucumberProperties: Map<String, String> = emptyMap(),
        junitPlatformProperties: Map<String, String> = emptyMap(),
    ): RequirementsCoverageListener =
        RequirementsCoverageListener(
            tracker = tracker,
            requirementsCoverageReporter = requirementsCoverageReporter,
            failBuild = failBuild,
            systemProperty = systemProperties::get,
            cucumberProperty = cucumberProperties::get,
            junitPlatformProperty = junitPlatformProperties::get,
        )

    private fun run(listener: RequirementsCoverageListener, vararg features: String) {
        val options = CommandlineOptionsParser(System.out)
            .parse("--glue", "dev.reqcover.cucumber.glue", *features.map { "classpath:features/$it" }.toTypedArray())
            .build()
        Runtime.builder()
            .withRuntimeOptions(options)
            .withAdditionalPlugins(listener)
            .build()
            .run()
    }
}
