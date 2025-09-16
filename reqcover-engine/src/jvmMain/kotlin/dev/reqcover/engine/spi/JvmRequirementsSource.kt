package dev.reqcover.engine.spi

import java.io.InputStream

interface JvmRequirementsSource : RequirementsSource {
    override fun load(uri: String): Set<String> {
        val resources = this::class.java.classLoader.getResources(uri)
        if (resources == null || !resources.hasMoreElements()) throw dev.reqcover.engine.NotFound(uri)
        return resources
            .asSequence()
            .flatMap { it.openStream().use { inputStream -> load(inputStream) } }
            .toSet()
    }

    fun load(inputStream: InputStream): Set<String>
}
