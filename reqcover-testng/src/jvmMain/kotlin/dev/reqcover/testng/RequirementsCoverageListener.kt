package dev.reqcover.testng

import dev.reqcover.api.ForRequirement
import dev.reqcover.engine.RequirementsCoverageTracker
import dev.reqcover.engine.loadRequirements
import dev.reqcover.engine.parseRequirementsUris
import dev.reqcover.engine.report
import org.testng.IExecutionListener
import org.testng.ITestListener
import org.testng.ITestResult
import java.util.logging.Logger
import kotlin.system.exitProcess

class RequirementsCoverageListener @JvmOverloads constructor(
    private val tracker: RequirementsCoverageTracker = RequirementsCoverageTracker(),
    private val requirementsCoverageReporter: (RequirementsCoverageTracker) -> Unit = ::report,
    private val failBuild: () -> Unit = { exitProcess(1) },
    private val systemProperty: (String) -> String? = System::getProperty,
) : IExecutionListener, ITestListener {
    private val logger = Logger.getLogger(RequirementsCoverageListener::class.java.name)

    override fun onExecutionStart() {
        val requirementsUris = systemProperty("reqCover.requirementsUris")
        if (requirementsUris == null) {
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
        parseRequirementsUris(requirementsUris).forEach { uri -> tracker.expectAll(loadRequirements(uri)) }
    }

    override fun onTestSuccess(result: ITestResult) {
        result.method.constructorOrMethod.method
            ?.getAnnotationsByType(ForRequirement::class.java)
            ?.forEach { tracker.verified(it.id) }
    }
}
