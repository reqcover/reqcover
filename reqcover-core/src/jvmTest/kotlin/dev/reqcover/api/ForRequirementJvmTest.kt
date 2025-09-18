package dev.reqcover.api

import dev.reqcover.api.java.lang.reflect.hasAnnotation
import kotlin.test.Test
import kotlin.test.assertTrue

class ForRequirementJvmTest {
    @Test
    fun `the @ForRequirement annotation has runtime retention`() {
        assertTrue(ForRequirementTest::class.java.methods.any { it.hasAnnotation<ForRequirement>() })
    }
}
