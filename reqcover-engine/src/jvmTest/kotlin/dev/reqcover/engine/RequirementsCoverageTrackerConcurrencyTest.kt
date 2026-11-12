package dev.reqcover.engine

import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals

class RequirementsCoverageTrackerConcurrencyTest {
    private val threads = 16
    private val idsPerThread = 250

    private fun idsOf(thread: Int): List<String> = (0 until idsPerThread).map { "#req-$thread-$it" }

    private fun allIds(): Set<String> = (0 until threads).flatMap(::idsOf).toSet()

    private fun concurrently(action: (List<String>) -> Unit) {
        val barrier = CyclicBarrier(threads)
        val executor = Executors.newFixedThreadPool(threads)
        val futures = (0 until threads).map { thread ->
            executor.submit {
                barrier.await()
                action(idsOf(thread))
            }
        }
        futures.forEach { it.get(1, TimeUnit.MINUTES) }
        executor.shutdown()
    }

    @Test
    fun `no verified requirement is lost when many threads verify at once`() {
        val tracker = RequirementsCoverageTracker()

        concurrently { ids -> ids.forEach(tracker::verified) }

        assertEquals(allIds(), tracker.verifiedRequirements())
    }

    @Test
    fun `no expected requirement is lost when many threads expect at once`() {
        val tracker = RequirementsCoverageTracker()

        concurrently { ids -> ids.forEach(tracker::expect) }

        assertEquals(allIds(), tracker.expectedRequirements())
    }

    @Test
    fun `no expected requirement is lost when many threads expect batches at once`() {
        val tracker = RequirementsCoverageTracker()

        concurrently { ids -> ids.chunked(10).forEach(tracker::expectAll) }

        assertEquals(allIds(), tracker.expectedRequirements())
    }
}
