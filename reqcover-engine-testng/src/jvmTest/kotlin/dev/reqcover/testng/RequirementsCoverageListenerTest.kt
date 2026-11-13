package dev.reqcover.testng

import dev.reqcover.api.ForRequirement
import dev.reqcover.engine.RequirementsCoverageTracker
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.testng.SkipException
import org.testng.TestNG
import org.testng.xml.XmlClass
import org.testng.xml.XmlSuite
import org.testng.xml.XmlTest
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
    fun `warns how to configure requirements when the requirements URIs value is empty`() {
        listOf("", " , ").forEach { emptyValue ->
            val warnings = capturedWarnings {
                executeTests(
                    listener = listener(properties = mapOf("reqCover.requirementsUris" to emptyValue)),
                    testClasses = arrayOf(PassingRequirementCase::class.java),
                )
            }

            assertTrue(
                warnings.any { it.contains("-DreqCover.requirementsUris=") },
                "Expected a warning explaining how to configure reqCover.requirementsUris for '$emptyValue', but got: $warnings",
            )
        }
    }

    @Test
    fun `warns when a requirements file contains no requirements`() {
        val failBuildCalled = AtomicBoolean(false)

        val warnings = capturedWarnings {
            executeTests(
                listener = listener(
                    failBuild = { failBuildCalled.set(true) },
                    properties = mapOf("reqCover.requirementsUris" to "requirements-a.yaml, no-requirements.yaml"),
                ),
                testClasses = arrayOf(PassingRequirementCase::class.java),
            )
        }

        assertFalse(failBuildCalled.get())
        assertTrue(
            warnings.any { it.contains("no-requirements.yaml") && it.contains("no requirements") },
            "Expected a warning naming the file without requirements, but got: $warnings",
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
    fun `fails when a configured requirements file is not a supported format`() {
        val failBuildCalled = AtomicBoolean(false)

        val errors = capturedMessages(Level.SEVERE) {
            executeTests(
                listener = listener(
                    failBuild = { failBuildCalled.set(true) },
                    properties = mapOf(
                        "reqCover.requirementsUris" to "not-openapi.yaml",
                        "reqCover.minimumRequiredCoveragePercent" to "100.0",
                    ),
                ),
                testClasses = arrayOf(UnannotatedPassingCase::class.java),
            )
        }

        assertTrue(failBuildCalled.get())
        assertTrue(
            errors.any { it.contains("not-openapi.yaml") && it.contains("reqCover.requirementsUris") },
            "Expected an error naming the unsupported URI and the property to check, but got: $errors",
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

    @Test
    fun `reads requirements URIs from a testng xml suite parameter`() {
        val tracker = RequirementsCoverageTracker()

        val warnings = capturedWarnings {
            executeSuites(
                listener = listener(tracker = tracker),
                suites = listOf(suite("a", mapOf("reqCover.requirementsUris" to "requirements-a.yaml"))),
            )
        }

        assertEquals(setOf("#reqOne"), tracker.expectedRequirements())
        assertEquals(emptyList<String>(), warnings)
    }

    @Test
    fun `a system property takes precedence over a testng xml suite parameter`() {
        val tracker = RequirementsCoverageTracker()

        executeSuites(
            listener = listener(tracker = tracker, properties = mapOf("reqCover.requirementsUris" to "requirements-b.yaml")),
            suites = listOf(suite("a", mapOf("reqCover.requirementsUris" to "requirements-a.yaml"))),
        )

        assertEquals(setOf("#reqTwo"), tracker.expectedRequirements())
    }

    @Test
    fun `expects the requirements of every suite`() {
        val tracker = RequirementsCoverageTracker()

        executeSuites(
            listener = listener(tracker = tracker),
            suites = listOf(
                suite("a", mapOf("reqCover.requirementsUris" to "requirements-a.yaml")),
                suite("b", mapOf("reqCover.requirementsUris" to "requirements-b.yaml")),
            ),
        )

        assertEquals(setOf("#reqOne", "#reqTwo"), tracker.expectedRequirements())
    }

    @Test
    fun `fails when a requirements file from a suite parameter cannot be found`() {
        val failBuildCalled = AtomicBoolean(false)

        executeSuites(
            listener = listener(failBuild = { failBuildCalled.set(true) }),
            suites = listOf(suite("a", mapOf("reqCover.requirementsUris" to "does-not-exist.yaml"))),
        )

        assertTrue(failBuildCalled.get())
    }

    @Test
    fun `reports a requirements file shared by several suites only once`() {
        val errors = capturedMessages(Level.SEVERE) {
            executeSuites(
                listener = listener(properties = mapOf("reqCover.requirementsUris" to "does-not-exist.yaml")),
                suites = listOf(suite("a", emptyMap()), suite("b", emptyMap())),
            )
        }

        assertEquals(1, errors.count { it.contains("does-not-exist.yaml") }, "Expected one error for the missing file, but got: $errors")
    }

    @Test
    fun `reads the minimum coverage from a testng xml suite parameter`() {
        val failBuildCalled = AtomicBoolean(false)

        executeSuites(
            listener = listener(failBuild = { failBuildCalled.set(true) }),
            suites = listOf(
                suite(
                    "a",
                    mapOf(
                        "reqCover.requirementsUris" to "requirements-a.yaml",
                        "reqCover.minimumRequiredCoveragePercent" to "100.0",
                    ),
                    testClass = UnannotatedPassingCase::class.java,
                ),
            ),
        )

        assertTrue(failBuildCalled.get())
    }

    @Test
    fun `uses the strictest coverage policy of all suites`() {
        val failBuildCalled = AtomicBoolean(false)

        executeSuites(
            listener = listener(failBuild = { failBuildCalled.set(true) }),
            suites = listOf(
                suite(
                    "a",
                    mapOf(
                        "reqCover.requirementsUris" to "requirements-a.yaml, requirements-b.yaml",
                        "reqCover.minimumRequiredCoveragePercent" to "100.0",
                        "reqCover.failIfBelowMinimum" to "false",
                    ),
                ),
                suite("b", mapOf("reqCover.minimumRequiredCoveragePercent" to "50.0")),
            ),
        )

        assertTrue(failBuildCalled.get())
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

    private fun suite(
        name: String,
        parameters: Map<String, String>,
        testClass: Class<*> = PassingRequirementCase::class.java,
    ): XmlSuite =
        XmlSuite().apply {
            this.name = name
            this.parameters = parameters
            XmlTest(this).apply {
                this.name = "$name-test"
                xmlClasses = listOf(XmlClass(testClass))
            }
        }

    private fun executeSuites(listener: RequirementsCoverageListener, suites: List<XmlSuite>) {
        TestNG().apply {
            setUseDefaultListeners(false)
            setVerbose(0)
            setXmlSuites(suites)
            addListener(listener)
        }.run()
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
