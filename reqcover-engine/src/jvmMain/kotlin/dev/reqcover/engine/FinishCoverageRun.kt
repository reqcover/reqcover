package dev.reqcover.engine

interface CoverageRunLog {
    fun info(message: String)
    fun warning(message: String)
    fun error(message: String)
}

fun finishCoverageRun(
    tracker: RequirementsCoverageTracker,
    policy: CoveragePolicy,
    report: (RequirementsCoverageTracker) -> Unit,
    failBuild: () -> Unit,
    log: CoverageRunLog,
) {
    val unverified = tracker.unverifiedRequirements()
    if (unverified.isNotEmpty()) {
        log.warning("The following requirements were not covered by any test: $unverified")
    } else {
        log.info("All requirements are covered by tests.")
    }

    val unexpected = tracker.unexpectedRequirements()
    if (unexpected.isNotEmpty()) {
        log.warning("The following covered requirements are not defined in the requirements specification: $unexpected")
    }

    val outcome = evaluateCoverage(tracker, policy)
    val coverage = "%.1f".format(outcome.coveragePercent)
    val minimum = policy.minimumRequiredCoveragePercent
    log.info("Requirements coverage: ${outcome.coveredRequirements} out of ${outcome.expectedRequirements} = $coverage%")
    report(tracker)

    if (!outcome.isBelowMinimum) {
        log.info("Requirements coverage meets the minimum required coverage of $minimum%. Actual coverage: $coverage%")
        return
    }
    log.error("Requirements coverage is below the minimum required coverage of $minimum%. Actual coverage: $coverage%")
    if (outcome.shouldFailBuild) {
        log.error("Exiting with error code 1 due to insufficient requirements coverage.")
        failBuild()
    }
}
