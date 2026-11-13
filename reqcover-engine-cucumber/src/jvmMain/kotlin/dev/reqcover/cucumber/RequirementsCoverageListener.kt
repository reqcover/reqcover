package dev.reqcover.cucumber

import dev.reqcover.engine.CoverageRunLog
import dev.reqcover.engine.ReqCoverConfiguration
import dev.reqcover.engine.RequirementsCoverageTracker
import dev.reqcover.engine.finishCoverageRun
import dev.reqcover.engine.loadAllRequirements
import dev.reqcover.engine.report
import io.cucumber.plugin.ConcurrentEventListener
import io.cucumber.plugin.event.EventPublisher
import io.cucumber.plugin.event.Status
import io.cucumber.plugin.event.TestCaseFinished
import io.cucumber.plugin.event.TestRunFinished
import io.cucumber.plugin.event.TestRunStarted
import java.util.Properties
import kotlin.system.exitProcess

class RequirementsCoverageListener @JvmOverloads constructor(
    private val tracker: RequirementsCoverageTracker = RequirementsCoverageTracker(),
    private val requirementsCoverageReporter: (RequirementsCoverageTracker, String) -> Unit = ::report,
    private val failBuild: () -> Unit = { exitProcess(1) },
    private val systemProperty: (String) -> String? = System::getProperty,
    private val cucumberProperty: (String) -> String? = classpathProperties("cucumber.properties")::getProperty,
    private val junitPlatformProperty: (String) -> String? = classpathProperties("junit-platform.properties")::getProperty,
) : ConcurrentEventListener {
    private val logger = System.getLogger(RequirementsCoverageListener::class.java.name)
    private val configuration = ReqCoverConfiguration(listOf(systemProperty, cucumberProperty, junitPlatformProperty))
    private val tagPrefix = configuration.value(TAG_PREFIX) ?: DEFAULT_TAG_PREFIX

    private val log = object : CoverageRunLog {
        override fun info(message: String) = logger.log(System.Logger.Level.INFO, message)
        override fun warning(message: String) = logger.log(System.Logger.Level.WARNING, message)
        override fun error(message: String) = logger.log(System.Logger.Level.ERROR, message)
    }

    override fun setEventPublisher(publisher: EventPublisher) {
        publisher.registerHandlerFor(TestRunStarted::class.java) { onRunStarted() }
        publisher.registerHandlerFor(TestCaseFinished::class.java, ::onTestCaseFinished)
        publisher.registerHandlerFor(TestRunFinished::class.java) { onRunFinished() }
    }

    private fun onRunStarted() {
        val requirementsUris = configuration.requirementsUris()
        if (requirementsUris.isEmpty()) {
            log.warning(
                """ReqCover is on the classpath, but no requirements specification has been provided.
The recommended solution is to add the following line to your cucumber.properties or junit-platform.properties file:
reqCover.requirementsUris=path/to/your/requirements
or to pass -DreqCover.requirementsUris=path/to/your/requirements to the JVM running your Cucumber tests.
This will enable ReqCover to check the requirements coverage against your requirements specification.
"""
            )
            return
        }
        val loaded = loadAllRequirements(requirementsUris)
        tracker.expectAll(loaded.requirements)
        loaded.failures.forEach { failure -> log.error("${failure.message}. $CHECK_REQUIREMENTS_URIS") }
        loaded.urisWithoutRequirements.forEach { uri ->
            log.warning("Requirements file at URI: $uri contains no requirements. $CHECK_REQUIREMENTS_URIS")
        }
        if (loaded.failures.isNotEmpty()) failBuild()
    }

    private fun onTestCaseFinished(event: TestCaseFinished) {
        if (event.result.status != Status.PASSED) return
        event.testCase.tags
            .filter { it.startsWith(tagPrefix) }
            .forEach { tracker.verified(it.removePrefix(tagPrefix)) }
    }

    private fun onRunFinished() {
        finishCoverageRun(
            tracker = tracker,
            policy = configuration.coveragePolicy(),
            report = { requirementsCoverageReporter(it, configuration.reportsDirectory()) },
            failBuild = failBuild,
            log = log,
        )
    }

    private companion object {
        const val TAG_PREFIX = "reqCover.cucumber.tagPrefix"
        const val DEFAULT_TAG_PREFIX = "@req:"
        const val CHECK_REQUIREMENTS_URIS =
            "Check the reqCover.requirementsUris system property, cucumber.properties or junit-platform.properties setting."

        fun classpathProperties(name: String): Properties =
            Properties().apply {
                RequirementsCoverageListener::class.java.classLoader.getResourceAsStream(name)?.use(::load)
            }
    }
}
