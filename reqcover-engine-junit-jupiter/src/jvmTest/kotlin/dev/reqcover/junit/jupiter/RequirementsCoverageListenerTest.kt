package dev.reqcover.junit.jupiter

import dev.reqcover.api.ForRequirement
import dev.reqcover.engine.RequirementsCoverageTracker
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.RepetitionInfo
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.junit.jupiter.api.Test
import org.junit.platform.engine.discovery.DiscoverySelectors.selectClass
import org.junit.platform.launcher.core.LauncherConfig
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
import org.junit.platform.launcher.core.LauncherFactory
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
    fun `writes the coverage report before failing the build`() {
        val events = mutableListOf<String>()

        executeTests(
            listener = RequirementsCoverageListener(
                requirementsCoverageReporter = { events.add("report") },
                failBuild = { events.add("failBuild") },
            ),
            testClasses = arrayOf(UnannotatedPassingCase::class.java),
            configurationParameters = mapOf(
                "reqCover.requirementsUris" to "requirements-a.yaml",
                "reqCover.minimumRequiredCoveragePercent" to "100.0",
            ),
        )

        assertEquals(listOf("report", "failBuild"), events)
    }

    @Test
    fun `a parameterized, repeated or factory test whose runs all fail does not cover its requirement`() {
        val tracker = verifiedBy(FailingParameterizedCase::class.java, FailingRepeatedCase::class.java, FailingFactoryCase::class.java)

        assertEquals(emptySet<String>(), tracker.verifiedRequirements())
    }

    @Test
    fun `one passing run of a parameterized test covers its requirement`() {
        assertEquals(setOf("#paramReq"), verifiedBy(PartlyPassingParameterizedCase::class.java).verifiedRequirements())
    }

    @Test
    fun `one passing run of a repeated test covers its requirement`() {
        assertEquals(setOf("#repeatedReq"), verifiedBy(PartlyPassingRepeatedCase::class.java).verifiedRequirements())
    }

    @Test
    fun `one passing dynamic test of a test factory covers its requirement`() {
        assertEquals(setOf("#factoryReq"), verifiedBy(PartlyPassingFactoryCase::class.java).verifiedRequirements())
    }

    @Test
    fun `fails when a configured requirements URI cannot be found`() {
        val failBuildCalled = AtomicBoolean(false)

        executeTests(
            listener = RequirementsCoverageListener(
                requirementsCoverageReporter = {},
                failBuild = { failBuildCalled.set(true) },
            ),
            testClasses = arrayOf(UnannotatedPassingCase::class.java),
            configurationParameters = mapOf(
                "reqCover.requirementsUris" to "requirements-a.yaml, does-not-exist.yaml",
            ),
        )

        assertTrue(failBuildCalled.get())
    }

    @Test
    fun `fails when a configured requirements file is not a supported format`() {
        val failBuildCalled = AtomicBoolean(false)

        executeTests(
            listener = RequirementsCoverageListener(
                requirementsCoverageReporter = {},
                failBuild = { failBuildCalled.set(true) },
            ),
            testClasses = arrayOf(UnannotatedPassingCase::class.java),
            configurationParameters = mapOf(
                "reqCover.requirementsUris" to "not-openapi.yaml",
                "reqCover.minimumRequiredCoveragePercent" to "100.0",
            ),
        )

        assertTrue(failBuildCalled.get())
    }

    @Test
    fun `warns how to configure requirements when the requirements URIs value is empty`() {
        listOf("", " , ").forEach { emptyValue ->
            val warnings = capturedWarnings {
                executeTests(
                    listener = RequirementsCoverageListener(requirementsCoverageReporter = {}, failBuild = {}),
                    testClasses = arrayOf(PassingRequirementCase::class.java),
                    configurationParameters = mapOf("reqCover.requirementsUris" to emptyValue),
                )
            }

            assertTrue(
                warnings.any { it.contains("reqCover.requirementsUris=path/to/your/requirements") },
                "Expected a warning explaining how to configure reqCover.requirementsUris for '$emptyValue', but got: $warnings",
            )
        }
    }

    @Test
    fun `warns when a requirements file contains no requirements`() {
        val failBuildCalled = AtomicBoolean(false)

        val warnings = capturedWarnings {
            executeTests(
                listener = RequirementsCoverageListener(
                    requirementsCoverageReporter = {},
                    failBuild = { failBuildCalled.set(true) },
                ),
                testClasses = arrayOf(PassingRequirementCase::class.java),
                configurationParameters = mapOf("reqCover.requirementsUris" to "requirements-a.yaml, no-requirements.yaml"),
            )
        }

        assertFalse(failBuildCalled.get())
        assertTrue(
            warnings.any { it.contains("no-requirements.yaml") && it.contains("no requirements") },
            "Expected a warning naming the file without requirements, but got: $warnings",
        )
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

    private fun verifiedBy(vararg testClasses: Class<*>): RequirementsCoverageTracker {
        val tracker = RequirementsCoverageTracker()
        executeTests(
            listener = RequirementsCoverageListener(tracker = tracker, requirementsCoverageReporter = {}, failBuild = {}),
            testClasses = arrayOf(*testClasses),
            configurationParameters = mapOf("reqCover.requirementsUris" to "requirements-a.yaml"),
        )
        return tracker
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

    @Disabled("Executed via the embedded JUnit launcher in RequirementsCoverageListenerTest")
    class FailingParameterizedCase {
        @ParameterizedTest
        @ValueSource(ints = [1, 2])
        @ForRequirement("#paramReq")
        fun fails(i: Int) {
            throw AssertionError("boom $i")
        }
    }

    @Disabled("Executed via the embedded JUnit launcher in RequirementsCoverageListenerTest")
    class PartlyPassingParameterizedCase {
        @ParameterizedTest
        @ValueSource(ints = [1, 2])
        @ForRequirement("#paramReq")
        fun passesOnlyForTwo(i: Int) {
            if (i != 2) throw AssertionError("boom $i")
        }
    }

    @Disabled("Executed via the embedded JUnit launcher in RequirementsCoverageListenerTest")
    class FailingRepeatedCase {
        @RepeatedTest(2)
        @ForRequirement("#repeatedReq")
        fun fails() {
            throw AssertionError("boom")
        }
    }

    @Disabled("Executed via the embedded JUnit launcher in RequirementsCoverageListenerTest")
    class PartlyPassingRepeatedCase {
        @RepeatedTest(2)
        @ForRequirement("#repeatedReq")
        fun passesOnlyTheSecondTime(repetitionInfo: RepetitionInfo) {
            if (repetitionInfo.currentRepetition != 2) throw AssertionError("boom")
        }
    }

    @Disabled("Executed via the embedded JUnit launcher in RequirementsCoverageListenerTest")
    class FailingFactoryCase {
        @TestFactory
        @ForRequirement("#factoryReq")
        fun fails(): List<DynamicTest> =
            listOf(1, 2).map { i -> dynamicTest("fails $i") { throw AssertionError("boom $i") } }
    }

    @Disabled("Executed via the embedded JUnit launcher in RequirementsCoverageListenerTest")
    class PartlyPassingFactoryCase {
        @TestFactory
        @ForRequirement("#factoryReq")
        fun passesOnlyForTwo(): List<DynamicTest> =
            listOf(1, 2).map { i -> dynamicTest("run $i") { if (i != 2) throw AssertionError("boom $i") } }
    }
}
