package dev.reqcover.testng

import dev.reqcover.api.ForRequirement
import dev.reqcover.engine.RequirementsCoverageTracker
import dev.reqcover.engine.loadRequirements
import dev.reqcover.engine.parseRequirementsUris
import dev.reqcover.engine.report
import org.testng.IExecutionListener
import org.testng.ITestListener
import org.testng.ITestResult
import kotlin.system.exitProcess

class RequirementsCoverageListener @JvmOverloads constructor(
    private val tracker: RequirementsCoverageTracker = RequirementsCoverageTracker(),
    private val requirementsCoverageReporter: (RequirementsCoverageTracker) -> Unit = ::report,
    private val failBuild: () -> Unit = { exitProcess(1) },
    private val systemProperty: (String) -> String? = System::getProperty,
) : IExecutionListener, ITestListener {
    override fun onExecutionStart() {
        val requirementsUris = systemProperty("reqCover.requirementsUris") ?: return
        parseRequirementsUris(requirementsUris).forEach { uri -> tracker.expectAll(loadRequirements(uri)) }
    }

    override fun onTestSuccess(result: ITestResult) {
        result.method.constructorOrMethod.method
            ?.getAnnotationsByType(ForRequirement::class.java)
            ?.forEach { tracker.verified(it.id) }
    }
}
