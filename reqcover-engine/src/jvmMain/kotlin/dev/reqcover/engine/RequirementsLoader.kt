package dev.reqcover.engine

import java.util.ServiceLoader

fun loadRequirements(uri: String): Set<String> {
    val serviceLoader = ServiceLoader.load(RequirementsSource::class.java)
    var notFound: NotFound? = null
    var any: Boolean = false
    serviceLoader.forEach { source ->
        any = true
        try {
            return source.load(uri)
        } catch (e: UnsupportedFormat) {
            // Ignore
        } catch (e: NotFound) {
            notFound = e
        }
    }
    if (!any) {
        throw UnsupportedFormat(uri, cause = IllegalStateException("No RequirementsSource implementations found."))
    }
    notFound?.let { throw it }
    throw UnsupportedFormat(uri)
}
