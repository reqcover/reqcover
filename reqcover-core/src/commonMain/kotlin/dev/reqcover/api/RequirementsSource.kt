package dev.reqcover.api

/**
 * Provides a source of requirements.
 * @param uri URI to find the requirements.
 */
@Repeatable
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class RequirementsSource(val uri: String)
