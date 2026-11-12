package dev.reqcover.engine

import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.update

@OptIn(ExperimentalAtomicApi::class)
class RequirementsCoverageTracker {
    private val expectedRequirements = AtomicReference(emptySet<String>())
    private val verifiedRequirements = AtomicReference(emptySet<String>())

    fun expect(requirementId: String) {
        expectedRequirements.update { it + requirementId }
    }

    fun expectAll(requirementIds: Collection<String>) {
        expectedRequirements.update { it + requirementIds }
    }

    fun isExpected(requirementId: String): Boolean = requirementId in expectedRequirements.load()

    fun verified(requirementId: String) {
        verifiedRequirements.update { it + requirementId }
    }

    fun isVerified(requirementId: String): Boolean = requirementId in verifiedRequirements.load()

    fun expectedRequirements(): Set<String> = expectedRequirements.load()

    fun verifiedRequirements(): Set<String> = verifiedRequirements.load()

    fun unverifiedRequirements(): Set<String> = expectedRequirements.load() - verifiedRequirements.load()

    fun unexpectedRequirements(): Set<String> = verifiedRequirements.load() - expectedRequirements.load()

    fun coveredRequirements(): Set<String> = verifiedRequirements.load() intersect expectedRequirements.load()
}
