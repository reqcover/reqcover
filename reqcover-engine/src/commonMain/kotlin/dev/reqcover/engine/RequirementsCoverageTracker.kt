package dev.reqcover.engine

class RequirementsCoverageTracker {
    private val expectedRequirements = mutableSetOf<String>()
    private val verifiedRequirements = mutableSetOf<String>()

    fun expect(requirementId: String) {
        expectedRequirements.add(requirementId)
    }

    fun expectAll(requirementIds: Collection<String>) {
        expectedRequirements.addAll(requirementIds)
    }

    fun isExpected(requirementId: String): Boolean {
        return expectedRequirements.contains(requirementId)
    }

    fun verified(requirementId: String) {
        verifiedRequirements.add(requirementId)
    }

    fun isVerified(requirementId: String): Boolean {
        return verifiedRequirements.contains(requirementId)
    }

    fun expectedRequirements(): Set<String> {
        return expectedRequirements
    }

    fun verifiedRequirements(): Set<String> {
        return verifiedRequirements
    }

    fun unverifiedRequirements(): Set<String> {
        return expectedRequirements - verifiedRequirements
    }

    fun unexpectedRequirements(): Set<String> {
        return verifiedRequirements - expectedRequirements
    }

    fun coveredRequirements() : Set<String> {
        return verifiedRequirements intersect expectedRequirements
    }
}
