package dev.reqcover.api.java.lang.reflect

import java.lang.reflect.AnnotatedElement

/** Checks whether an annotation is present on an annotated element (class, field, method, etc.).
 * This function works for both single and repeatable annotations.
 * @param T the annotation type to check for.
 * @return true if the annotation is present, false otherwise.
 */
inline fun <reified T : Annotation> AnnotatedElement.hasAnnotation() : Boolean {
    return isAnnotationPresent(T::class.java) || getAnnotationsByType(T::class.java).isNotEmpty()
}
