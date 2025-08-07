package dev.reqcover.api

/**
 * Annotates a test case to indicate that it is associated with a specific requirement.
 * @param id The identifier of the requirement this test case covers.
 */
@Repeatable
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class ForRequirement(val id: String)
