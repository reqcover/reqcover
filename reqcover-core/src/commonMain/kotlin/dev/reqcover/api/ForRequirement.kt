package dev.reqcover.api

/**
 * Annotates a test case to indicate that it is associated with a specific requirement.
 * On a test class, the requirement is covered when at least one test in the class passes.
 * @param id The identifier of the requirement this test case covers.
 */
@Repeatable
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
annotation class ForRequirement(val id: String)
