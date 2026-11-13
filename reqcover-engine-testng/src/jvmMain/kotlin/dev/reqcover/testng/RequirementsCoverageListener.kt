package dev.reqcover.testng

import dev.reqcover.api.ForRequirement
import dev.reqcover.engine.ReqCoverConfiguration
import dev.reqcover.engine.RequirementsCoverageTracker
import dev.reqcover.engine.CoverageRunLog
import dev.reqcover.engine.finishCoverageRun
import dev.reqcover.engine.loadAllRequirements
import dev.reqcover.engine.report
import org.testng.IExecutionListener
import org.testng.ITestListener
import org.testng.ITestResult
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.system.exitProcess

class RequirementsCoverageListener @JvmOverloads constructor(
    private val tracker: RequirementsCoverageTracker = RequirementsCoverageTracker(),
    private val requirementsCoverageReporter: (RequirementsCoverageTracker) -> Unit = ::report,
    private val failBuild: () -> Unit = { exitProcess(1) },
    private val systemProperty: (String) -> String? = System::getProperty,
) : IExecutionListener, ITestListener {
    private val logger = Logger.getLogger(RequirementsCoverageListener::class.java.name)
    private val configuration = ReqCoverConfiguration(listOf(systemProperty))

    override fun onExecutionStart() {
        val requirementsUris = configuration.requirementsUris()
        if (requirementsUris.isEmpty()) {
            logger.warning(
                """ReqCover is on the classpath, but no requirements specification has been provided.
The recommended solution is to pass the following system property to the JVM running your TestNG tests:
-DreqCover.requirementsUris=path/to/your/requirements
For example, systemProperty("reqCover.requirementsUris", "path/to/your/requirements") in Gradle,
or <systemPropertyVariables> in the Maven Surefire configuration.
This will enable ReqCover to check the requirements coverage against your requirements specification.
"""
            )
            return
        }
        val loaded = loadAllRequirements(requirementsUris)
        tracker.expectAll(loaded.requirements)
        loaded.failures.forEach { failure ->
            logger.log(Level.SEVERE, "${failure.message}. Check the reqCover.requirementsUris system property.", failure.cause)
        }
        loaded.urisWithoutRequirements.forEach { uri ->
            logger.warning("Requirements file at URI: $uri contains no requirements. Check the reqCover.requirementsUris system property.")
        }
        if (loaded.failures.isNotEmpty()) failBuild()
    }

    override fun onTestSuccess(result: ITestResult) {
        result.method.constructorOrMethod.method
            ?.getAnnotationsByType(ForRequirement::class.java)
            ?.forEach { tracker.verified(it.id) }
    }

    override fun onExecutionFinish() {
        finishCoverageRun(
            tracker = tracker,
            policy = configuration.coveragePolicy(),
            report = requirementsCoverageReporter,
            failBuild = failBuild,
            log = object : CoverageRunLog {
                override fun info(message: String) = logger.info(message)
                override fun warning(message: String) = logger.warning(message)
                override fun error(message: String) = logger.severe(message)
            },
        )
    }
}
