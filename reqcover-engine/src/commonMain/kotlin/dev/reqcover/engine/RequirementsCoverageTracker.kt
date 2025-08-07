package dev.reqcover.engine

class RequirementsCoverageTracker {
    private val expectedRequirements = mutableSetOf<String>()
    private val verifiedRequirements = mutableSetOf<String>()

    fun expect(string: String) {
        expectedRequirements.add(string)
    }

    fun isExpected(string: String): Boolean {
        return expectedRequirements.contains(string)
    }

    fun verified(string: String) {
        verifiedRequirements.add(string)
    }

    fun isVerified(string: String): Boolean {
        return verifiedRequirements.contains(string)
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
}
