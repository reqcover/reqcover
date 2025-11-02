package dev.reqcover.testng

import dev.reqcover.api.ForRequirement
import dev.reqcover.engine.RequirementsCoverageTracker
import dev.reqcover.engine.loadRequirements
import dev.reqcover.engine.report
import org.testng.IExecutionListener
import org.testng.IInvokedMethod
import org.testng.IInvokedMethodListener
import org.testng.ITestResult
import org.testng.internal.TestResult
import org.testng.log4testng.Logger
import java.util.Properties
import kotlin.system.exitProcess

class RequirementsCoverageListener : IExecutionListener, IInvokedMethodListener {
    private val tracker = RequirementsCoverageTracker()
    private val logger = Logger.getLogger(RequirementsCoverageListener::class.java)
    private val config = RequirementsCoverageListener::class.java.getClassLoader().getResourceAsStream("testng.properties").use {
        Properties().apply {
            load(it)
        }
    }

    override fun onExecutionStart() {
        val requirementsUri: String? = config.getProperty("reqCover.requirementsUri")
        val requirementsUris: String? = config.getProperty("reqCover.requirementsUris")
        if (requirementsUri == null && requirementsUris == null) {
            logger.warn {
                """ReqCover is on the classpath, but no requirements specification has been provided.
The recommended solution is to add the following line to your junit-platform.properties file:
reqCover.requirementsUris=path/to/your/requirements
This will enable ReqCover to check the requirements coverage against your requirements specification.
"""
            }
            return
        }
        if (requirementsUri != null)
            tracker.expectAll(loadRequirements(requirementsUri))
        if (requirementsUris != null)
            for (uri in requirementsUris.split(",").map { it.trim() })
                tracker.expectAll(loadRequirements(uri))
    }

    override fun afterInvocation(method: IInvokedMethod, testResult: ITestResult) {
        if (testResult.status == TestResult.SUCCESS) {
            if (method.isTestMethod) {
                val source = method.testMethod.constructorOrMethod.method
                val annotations = source.getAnnotationsByType(ForRequirement::class.java)
                for (annotation in annotations) {
                    tracker.verified(annotation.id)
                }
            }
        }
    }

    override fun onExecutionFinish() {
        if (tracker.unverifiedRequirements().isNotEmpty()) {
            logger.warn { "The following requirements were not covered by any test: ${tracker.unverifiedRequirements()}" }
        } else {
            logger.info { "All requirements are covered by tests." }
        }

        if (tracker.unexpectedRequirements().isNotEmpty()) {
            logger.warn { "The following covered requirements are not defined in the requirements specification: ${tracker.unexpectedRequirements()}" }
        }

        val coverage = (tracker.verifiedRequirements().size.toDouble() / tracker.expectedRequirements().size) * 100
        logger.info { "Requirements coverage: ${tracker.verifiedRequirements().size} out of ${tracker.expectedRequirements().size} = ${"%.1f".format(coverage)}%" }

        val minimumRequiredCoveragePercent = config.getProperty("reqCover.minimumRequiredCoveragePercent")?.toDoubleOrNull() ?: 0.0
        if (coverage < minimumRequiredCoveragePercent) {
            logger.error { "Requirements coverage is below the minimum required coverage of $minimumRequiredCoveragePercent%. Actual coverage: ${"%.1f".format(coverage)}%" }
            val failIfBelowMinimum = config.getProperty("reqCover.failIfBelowMinimum").toBooleanStrictOrNull() ?: true
            if (failIfBelowMinimum) {
                logger.error { "Exiting with error code 1 due to insufficient requirements coverage." }
                exitProcess(1)
            }
        } else {
            logger.info { "Requirements coverage meets the minimum required coverage of $minimumRequiredCoveragePercent%. Actual coverage: ${"%.1f".format(coverage)}%" }
        }
        report(tracker)
    }
}
