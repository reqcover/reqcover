package dev.reqcover.testng

import dev.reqcover.api.ForRequirement
import dev.reqcover.engine.CoveragePolicy
import dev.reqcover.engine.DEFAULT_REPORTS_DIRECTORY
import dev.reqcover.engine.ReqCoverConfiguration
import dev.reqcover.engine.RequirementsCoverageTracker
import dev.reqcover.engine.CoverageRunLog
import dev.reqcover.engine.finishCoverageRun
import dev.reqcover.engine.loadAllRequirements
import dev.reqcover.engine.report
import org.testng.IExecutionListener
import org.testng.ISuite
import org.testng.ISuiteListener
import org.testng.ITestListener
import org.testng.ITestResult
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.system.exitProcess

class RequirementsCoverageListener @JvmOverloads constructor(
    private val tracker: RequirementsCoverageTracker = RequirementsCoverageTracker(),
    private val requirementsCoverageReporter: (RequirementsCoverageTracker, String) -> Unit = ::report,
    private val failBuild: () -> Unit = { exitProcess(1) },
    private val systemProperty: (String) -> String? = System::getProperty,
) : IExecutionListener, ISuiteListener, ITestListener {
    private val logger = Logger.getLogger(RequirementsCoverageListener::class.java.name)
    private val loadedRequirementsUris = ConcurrentHashMap.newKeySet<String>()
    private val suiteConfigurations = ConcurrentLinkedQueue<ReqCoverConfiguration>()

    private fun configuration(suite: ISuite): ReqCoverConfiguration =
        ReqCoverConfiguration(listOf(systemProperty, suite::getParameter))

    override fun onStart(suite: ISuite) {
        val configuration = configuration(suite)
        suiteConfigurations.add(configuration)
        val newRequirementsUris = configuration.requirementsUris().filter(loadedRequirementsUris::add)
        val loaded = loadAllRequirements(newRequirementsUris)
        tracker.expectAll(loaded.requirements)
        loaded.failures.forEach { failure ->
            logger.log(Level.SEVERE, "${failure.message}. $CHECK_REQUIREMENTS_URIS", failure.cause)
        }
        loaded.urisWithoutRequirements.forEach { uri ->
            logger.warning("Requirements file at URI: $uri contains no requirements. $CHECK_REQUIREMENTS_URIS")
        }
        if (loaded.failures.isNotEmpty()) failBuild()
    }

    override fun onTestSuccess(result: ITestResult) {
        result.method.constructorOrMethod.method
            ?.getAnnotationsByType(ForRequirement::class.java)
            ?.forEach { tracker.verified(it.id) }
    }

    override fun onExecutionFinish() {
        if (loadedRequirementsUris.isEmpty()) {
            logger.warning(
                """ReqCover is on the classpath, but no requirements specification has been provided.
The recommended solution is to pass the following system property to the JVM running your TestNG tests:
-DreqCover.requirementsUris=path/to/your/requirements
For example, systemProperty("reqCover.requirementsUris", "path/to/your/requirements") in Gradle,
or <systemPropertyVariables> in the Maven Surefire configuration.
Alternatively, add <parameter name="reqCover.requirementsUris" value="path/to/your/requirements"/> to your testng.xml suite.
This will enable ReqCover to check the requirements coverage against your requirements specification.
"""
            )
        }
        finishCoverageRun(
            tracker = tracker,
            policy = CoveragePolicy.strictest(suiteConfigurations.map { it.coveragePolicy() }),
            report = { requirementsCoverageReporter(it, reportsDirectory()) },
            failBuild = failBuild,
            log = object : CoverageRunLog {
                override fun info(message: String) = logger.info(message)
                override fun warning(message: String) = logger.warning(message)
                override fun error(message: String) = logger.severe(message)
            },
        )
    }

    private fun reportsDirectory(): String =
        suiteConfigurations.firstNotNullOfOrNull { it.value(ReqCoverConfiguration.REPORTS_DIR) } ?: DEFAULT_REPORTS_DIRECTORY

    private companion object {
        const val CHECK_REQUIREMENTS_URIS =
            "Check the reqCover.requirementsUris system property or testng.xml suite parameter."
    }
}
