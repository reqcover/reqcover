package dev.reqcover.junit.jupiter

import dev.reqcover.api.ForRequirement
import dev.reqcover.engine.RequirementsCoverageTracker
import dev.reqcover.engine.loadRequirements
import org.junit.platform.commons.logging.LoggerFactory
import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.engine.support.descriptor.MethodSource
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestIdentifier
import org.junit.platform.launcher.TestPlan
import kotlin.system.exitProcess

class RequirementsCoverageListener : TestExecutionListener {
    private val tracker = RequirementsCoverageTracker()
    private val logger = LoggerFactory.getLogger(RequirementsCoverageListener::class.java)

    override fun executionFinished(testIdentifier: TestIdentifier, testExecutionResult: TestExecutionResult) {
        if (testExecutionResult.status == TestExecutionResult.Status.SUCCESSFUL) {
            val source = testIdentifier.source.orElse(null)
            if (source is MethodSource) {
                val clazz = Class.forName(source.className)
                val method = clazz.getMethod(source.methodName)

                val annotations = method.getAnnotationsByType(ForRequirement::class.java)
                for (annotation in annotations) {
                    tracker.verified(annotation.id)
                }
            }
        }
    }

    override fun testPlanExecutionFinished(testPlan: TestPlan) {
        val config = testPlan.configurationParameters
        val requirementsUri = config.get("reqCover.requirementsUri").orElse(null)
        if (requirementsUri == null) {
            logger.warn {
                """ReqCover is on the classpath, but no requirements specification has been provided.
The recommended solution is to add the following line to your junit-platform.properties file:
reqCover.requirementsUri=path/to/your/requirements
This will enable ReqCover to check the requirements coverage against your requirements specification.
"""
            }
            return
        }
        loadRequirements(requirementsUri).forEach { tracker.expect(it) }

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
        val minimumRequiredCoveragePercent = config.get("reqCover.minimumRequiredCoveragePercent").map { it.toDoubleOrNull() ?: 0.0 }.orElse(0.0)
        if (coverage < minimumRequiredCoveragePercent) {
            logger.error { "Requirements coverage is below the minimum required coverage of $minimumRequiredCoveragePercent%. Actual coverage: ${"%.1f".format(coverage)}%" }
            val failIfBelowMinimum = config.get("reqCover.failIfBelowMinimum").map { it.toBoolean() }.orElse(true)
            if (failIfBelowMinimum) {
                logger.error { "Exiting with error code 1 due to insufficient requirements coverage." }
                exitProcess(1)
            }
        } else {
            logger.info { "Requirements coverage meets the minimum required coverage of $minimumRequiredCoveragePercent%. Actual coverage: ${"%.1f".format(coverage)}%" }
        }
    }
}
