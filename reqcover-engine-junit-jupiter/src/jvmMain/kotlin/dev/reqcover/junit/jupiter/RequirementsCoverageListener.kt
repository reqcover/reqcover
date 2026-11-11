package dev.reqcover.junit.jupiter

import dev.reqcover.api.ForRequirement
import dev.reqcover.engine.CoveragePolicy
import dev.reqcover.engine.RequirementsCoverageTracker
import dev.reqcover.engine.evaluateCoverage
import dev.reqcover.engine.loadAllRequirements
import dev.reqcover.engine.parseRequirementsUris
import dev.reqcover.engine.report
import org.junit.platform.commons.logging.LoggerFactory
import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.engine.support.descriptor.MethodSource
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestIdentifier
import org.junit.platform.launcher.TestPlan
import kotlin.system.exitProcess

class RequirementsCoverageListener @JvmOverloads constructor(
    private val tracker: RequirementsCoverageTracker = RequirementsCoverageTracker(),
    private val requirementsCoverageReporter: (RequirementsCoverageTracker) -> Unit = ::report,
    private val failBuild: () -> Unit = { exitProcess(1) },
) : TestExecutionListener {
    private val logger = LoggerFactory.getLogger(RequirementsCoverageListener::class.java)

    override fun testPlanExecutionStarted(testPlan: TestPlan) {
        val config = testPlan.configurationParameters
        val removedRequirementsUri = config.get("reqCover.requirementsUri").orElse(null)
        if (removedRequirementsUri != null) {
            logger.error {
                "The reqCover.requirementsUri configuration parameter has been removed. " +
                    "Use reqCover.requirementsUris instead."
            }
            failBuild()
            return
        }
        val requirementsUris = config.get("reqCover.requirementsUris").orElse(null)
        if (requirementsUris == null) {
            logger.warn {
                """ReqCover is on the classpath, but no requirements specification has been provided.
The recommended solution is to add the following line to your junit-platform.properties file:
reqCover.requirementsUris=path/to/your/requirements
This will enable ReqCover to check the requirements coverage against your requirements specification.
"""
            }
            return
        }
        val loaded = loadAllRequirements(parseRequirementsUris(requirementsUris))
        tracker.expectAll(loaded.requirements)
        loaded.failures.forEach { failure ->
            logger.error(failure.cause) { "${failure.message}. Check the reqCover.requirementsUris configuration parameter." }
        }
        if (loaded.failures.isNotEmpty()) failBuild()
    }

    override fun executionFinished(testIdentifier: TestIdentifier, testExecutionResult: TestExecutionResult) {
        if (testExecutionResult.status == TestExecutionResult.Status.SUCCESSFUL) {
            val source = testIdentifier.source.orElse(null)
            if (source is MethodSource) {
                val annotations = source.javaMethod.getAnnotationsByType(ForRequirement::class.java)
                for (annotation in annotations) {
                    tracker.verified(annotation.id)
                }
            }
        }
    }

    override fun testPlanExecutionFinished(testPlan: TestPlan) {
        if (tracker.unverifiedRequirements().isNotEmpty()) {
            logger.warn { "The following requirements were not covered by any test: ${tracker.unverifiedRequirements()}" }
        } else {
            logger.info { "All requirements are covered by tests." }
        }

        if (tracker.unexpectedRequirements().isNotEmpty()) {
            logger.warn { "The following covered requirements are not defined in the requirements specification: ${tracker.unexpectedRequirements()}" }
        }

        val config = testPlan.configurationParameters
        val policy = CoveragePolicy.parse(
            minimumRequiredCoveragePercent = config.get("reqCover.minimumRequiredCoveragePercent").orElse(null),
            failIfBelowMinimum = config.get("reqCover.failIfBelowMinimum").orElse(null),
        )
        val outcome = evaluateCoverage(tracker, policy)
        val coverage = outcome.coveragePercent
        val minimumRequiredCoveragePercent = policy.minimumRequiredCoveragePercent
        logger.info { "Requirements coverage: ${outcome.coveredRequirements} out of ${outcome.expectedRequirements} = ${"%.1f".format(coverage)}%" }
        requirementsCoverageReporter(tracker)

        if (outcome.isBelowMinimum) {
            logger.error { "Requirements coverage is below the minimum required coverage of $minimumRequiredCoveragePercent%. Actual coverage: ${"%.1f".format(coverage)}%" }
            if (outcome.shouldFailBuild) {
                logger.error { "Exiting with error code 1 due to insufficient requirements coverage." }
                failBuild()
            }
        } else {
            logger.info { "Requirements coverage meets the minimum required coverage of $minimumRequiredCoveragePercent%. Actual coverage: ${"%.1f".format(coverage)}%" }
        }
    }
}
