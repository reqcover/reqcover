package dev.reqcover.engine

data class CoveragePolicy(
    val minimumRequiredCoveragePercent: Double = 0.0,
    val failIfBelowMinimum: Boolean = true,
) {
    companion object {
        fun parse(minimumRequiredCoveragePercent: String?, failIfBelowMinimum: String?): CoveragePolicy =
            CoveragePolicy(
                minimumRequiredCoveragePercent = minimumRequiredCoveragePercent?.toDoubleOrNull() ?: 0.0,
                failIfBelowMinimum = failIfBelowMinimum?.toBoolean() ?: true,
            )

        fun strictest(policies: List<CoveragePolicy>): CoveragePolicy =
            if (policies.isEmpty()) CoveragePolicy()
            else CoveragePolicy(
                minimumRequiredCoveragePercent = policies.maxOf { it.minimumRequiredCoveragePercent },
                failIfBelowMinimum = policies.any { it.failIfBelowMinimum },
            )
    }
}

data class CoverageOutcome(
    val coveredRequirements: Int,
    val expectedRequirements: Int,
    val coveragePercent: Double,
    val isBelowMinimum: Boolean,
    val shouldFailBuild: Boolean,
)

fun evaluateCoverage(tracker: RequirementsCoverageTracker, policy: CoveragePolicy): CoverageOutcome {
    val coveredRequirements = tracker.coveredRequirements().size
    val expectedRequirements = tracker.expectedRequirements().size
    val coveragePercent =
        if (expectedRequirements == 0) 100.0
        else (coveredRequirements.toDouble() / expectedRequirements) * 100
    val isBelowMinimum = coveragePercent < policy.minimumRequiredCoveragePercent
    return CoverageOutcome(
        coveredRequirements = coveredRequirements,
        expectedRequirements = expectedRequirements,
        coveragePercent = coveragePercent,
        isBelowMinimum = isBelowMinimum,
        shouldFailBuild = isBelowMinimum && policy.failIfBelowMinimum,
    )
}
