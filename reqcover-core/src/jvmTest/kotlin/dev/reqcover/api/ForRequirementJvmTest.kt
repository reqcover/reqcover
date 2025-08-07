package dev.reqcover.api

import kotlin.test.Test
import kotlin.test.assertTrue

class ForRequirementJvmTest {
    @Test
    fun `the @ForRequirement annotation has runtime retention`() {
        assertTrue(ForRequirementTest::class.java.methods.any { it.isAnnotationPresent(ForRequirement::class.java) })
    }
}
