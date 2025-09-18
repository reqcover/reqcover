package dev.reqcover.api

import dev.reqcover.api.java.lang.reflect.hasAnnotation
import kotlin.test.Test
import kotlin.test.assertTrue

class RequirementsSourceJvmTest {
    @Test
    fun `the @RequirementsSource annotation has runtime retention`() {
        RequirementsSourceTest::class.java.annotations.forEach {
            println(it)
        }
        assertTrue(RequirementsSourceTest::class.java.hasAnnotation<RequirementsSource>())
    }

    @Test
    fun `the @ForRequirement annotation has runtime retention`() {
        assertTrue(RequirementsSourceTest::class.java.methods.any { it.hasAnnotation<RequirementsSource>() })
    }
}
