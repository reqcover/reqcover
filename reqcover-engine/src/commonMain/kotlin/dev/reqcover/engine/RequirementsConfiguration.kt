package dev.reqcover.engine

fun parseRequirementsUris(requirementsUris: String): List<String> =
    requirementsUris.split(",").map { it.trim() }.filter { it.isNotEmpty() }

class ReqCoverConfiguration(private val sources: List<(String) -> String?>) {
    fun value(key: String): String? = sources.firstNotNullOfOrNull { source -> source(key) }

    fun requirementsUris(): List<String> = parseRequirementsUris(value(REQUIREMENTS_URIS) ?: "")

    fun coveragePolicy(): CoveragePolicy =
        CoveragePolicy.parse(
            minimumRequiredCoveragePercent = value(MINIMUM_REQUIRED_COVERAGE_PERCENT),
            failIfBelowMinimum = value(FAIL_IF_BELOW_MINIMUM),
        )

    companion object {
        const val REQUIREMENTS_URIS = "reqCover.requirementsUris"
        const val MINIMUM_REQUIRED_COVERAGE_PERCENT = "reqCover.minimumRequiredCoveragePercent"
        const val FAIL_IF_BELOW_MINIMUM = "reqCover.failIfBelowMinimum"
    }
}
