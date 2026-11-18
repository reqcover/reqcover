package dev.reqcover.source.openapi

import dev.reqcover.engine.UnsupportedFormat
import tools.jackson.core.JacksonException
import tools.jackson.databind.JsonNode
import tools.jackson.dataformat.yaml.YAMLMapper
import dev.reqcover.engine.spi.JvmRequirementsSource
import java.io.InputStream
import kotlin.collections.component1
import kotlin.collections.component2

class OpenApiRequirementsSource : JvmRequirementsSource {
    override fun load(uri: String): Set<String> =
        try {
            super.load(uri)
        } catch (e: JacksonException) {
            throw UnsupportedFormat(uri, e)
        } catch (e: IllegalArgumentException) {
            throw UnsupportedFormat(uri, e)
        }

    override fun load(inputStream: InputStream): Set<String> =
        loadOpenApiRequirements(YAMLMapper().readTree(inputStream))

    fun loadOpenApiRequirementsFromString(yamlContent: String): Set<String> =
        loadOpenApiRequirements(YAMLMapper().readTree(yamlContent))

    fun loadOpenApiRequirements(root: JsonNode): Set<String> {
        val setOfRequirements = mutableSetOf<String>()
        val pathsNode = requireNotNull(root.get("paths")) { "Not an OpenAPI document: no 'paths' found." }
        for ((rawPath, pathItem) in pathsNode.properties()) {
            for ((method, opNode) in pathItem.properties().filter { (key, _) -> key in HTTP_METHODS }) {
                val operationId = opNode.get("operationId")?.asString()
                if (operationId == null) {
                    println("Warning: Operation ID not found for path '$rawPath' and method '$method'.")
                    continue
                }
                val responses = opNode.get("responses")?.properties()
                if (responses == null || responses.isEmpty())
                    setOfRequirements.add("#$operationId")
                else for ((responseCode, responseNode) in responses) {
                    val requirements = responseNode.get("x-requirements")?.properties()
                    if (requirements == null || requirements.isEmpty())
                        setOfRequirements.add("#$operationId/$responseCode")
                    else for ((requirementsId, requirementsNode) in requirements) {
                        setOfRequirements.add("#$operationId/$responseCode/$requirementsId")
                    }
                }
            }
        }
        return setOfRequirements
    }

    private companion object {
        val HTTP_METHODS = setOf("get", "put", "post", "delete", "options", "head", "patch", "trace")
    }

    fun escapeJsonPointer(segment: String): String =
        segment.replace("~", "~0").replace("/", "~1")
}
