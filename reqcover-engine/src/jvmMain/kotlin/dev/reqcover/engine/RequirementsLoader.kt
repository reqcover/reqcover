package dev.reqcover.engine

import dev.reqcover.engine.spi.RequirementsSource
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

data class RequirementsLoadFailure(val uri: String, val cause: Exception) {
    val message: String =
        when (cause) {
            is NotFound, is UnsupportedFormat -> cause.message ?: uri
            else -> "Could not load requirements file at URI: $uri ($cause)"
        }
}

data class RequirementsLoadResult(
    val requirements: Set<String>,
    val failures: List<RequirementsLoadFailure>,
)

fun loadAllRequirements(
    uris: List<String>,
    load: (String) -> Set<String> = ::loadRequirements,
): RequirementsLoadResult =
    uris.fold(RequirementsLoadResult(emptySet(), emptyList())) { result, uri ->
        try {
            result.copy(requirements = result.requirements + load(uri))
        } catch (e: Exception) {
            result.copy(failures = result.failures + RequirementsLoadFailure(uri, e))
        }
    }
