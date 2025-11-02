package dev.reqcover.cucumber

import dev.reqcover.engine.RequirementsCoverageTracker
import dev.reqcover.engine.loadRequirements
import io.cucumber.plugin.ConcurrentEventListener
import io.cucumber.plugin.event.EventPublisher
import io.cucumber.plugin.event.TestCaseFinished
import io.cucumber.plugin.event.TestRunFinished
import io.cucumber.plugin.event.TestRunStarted
import jdk.jfr.internal.LogLevel
import java.util.Properties
import kotlin.system.exitProcess

class RequirementsCoverageListener : ConcurrentEventListener {
    private val tracker = RequirementsCoverageTracker()

    private val logger = System.getLogger("reqCover")

    private val config = RequirementsCoverageListener::class.java.getClassLoader().getResourceAsStream("cucumber.properties").use {
        Properties().apply {
            load(it)
        }
    }

    override fun setEventPublisher(publisher: EventPublisher) {
        with (publisher) {
            registerHandlerFor(TestRunStarted::class.java, ::onRunStarted)
            registerHandlerFor(TestCaseFinished::class.java, ::onCaseFinished)
            registerHandlerFor(TestRunFinished::class.java,  ::onRunFinished)
        }
    }

    fun onRunStarted(event: TestRunStarted) {
        val requirementsUri: String? = config.getProperty("reqCover.requirementsUri")
        val requirementsUris: String? = config.getProperty("reqCover.requirementsUris")
        if (requirementsUri == null && requirementsUris == null) {
            logger.warn(
                """ReqCover is on the classpath, but no requirements specification has been provided.
The recommended solution is to add the following line to your junit-platform.properties file:
reqCover.requirementsUris=path/to/your/requirements
This will enable ReqCover to check the requirements coverage against your requirements specification.
"""
            )
            return
        }
        if (requirementsUri != null)
            tracker.expectAll(loadRequirements(requirementsUri))
        if (requirementsUris != null)
            for (uri in requirementsUris.split(",").map { it.trim() })
                tracker.expectAll(loadRequirements(uri))
    }

    fun onCaseFinished(event: TestCaseFinished) {
        if (event.result.status.isOk) {
            val tags = event.testCase.tags
            for (tag in tags)
                logger.trace(tag)
        }
    }

    fun onRunFinished(event: TestRunFinished) {
        if (tracker.unverifiedRequirements().isNotEmpty()) {
            logger.warn("The following requirements were not covered by any test: ${tracker.unverifiedRequirements()}")
        } else {
            logger.info("All requirements are covered by tests.")
        }

        if (tracker.unexpectedRequirements().isNotEmpty()) {
            logger.warn("The following covered requirements are not defined in the requirements specification: ${tracker.unexpectedRequirements()}")
        }

        val coverage = (tracker.verifiedRequirements().size.toDouble() / tracker.expectedRequirements().size) * 100
        logger.info("Requirements coverage: ${tracker.verifiedRequirements().size} out of ${tracker.expectedRequirements().size} = ${"%.1f".format(coverage)}%")

        val minimumRequiredCoveragePercent = config.getProperty("reqCover.minimumRequiredCoveragePercent")?.toDoubleOrNull() ?: 0.0
        if (coverage < minimumRequiredCoveragePercent) {
            logger.error("Requirements coverage is below the minimum required coverage of $minimumRequiredCoveragePercent%. Actual coverage: ${"%.1f".format(coverage)}%")
            val failIfBelowMinimum = config.getProperty("reqCover.failIfBelowMinimum").toBooleanStrictOrNull() ?: true
            if (failIfBelowMinimum) {
                logger.error("Exiting with error code 1 due to insufficient requirements coverage.")
                exitProcess(1)
            }
        } else {
            logger.info("Requirements coverage meets the minimum required coverage of $minimumRequiredCoveragePercent%. Actual coverage: ${"%.1f".format(coverage)}%")
        }
    }
}

fun System.Logger.error(msg: String): Unit = this.log(System.Logger.Level.ERROR, msg)
fun System.Logger.info(msg: String): Unit = this.log(System.Logger.Level.INFO, msg)
fun System.Logger.warn(msg: String): Unit = this.log(System.Logger.Level.WARNING, msg)
fun System.Logger.trace(msg: String): Unit = this.log(System.Logger.Level.TRACE, msg)
