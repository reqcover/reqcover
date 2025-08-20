package dev.reqcover.source.openapi

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import dev.reqcover.engine.spi.RequirementsSource
import kotlin.collections.component1
import kotlin.collections.component2

class OpenApiRequirementsSource : RequirementsSource {
    override fun load(uri: String): Set<String> {
        val resource = this::class.java.classLoader.getResource(uri)
            ?: throw dev.reqcover.engine.NotFound(uri)
        return loadOpenApiRequirements(YAMLMapper().readTree(resource.openStream()))
    }
}

fun loadOpenApiRequirementsFromString(yamlContent: String): Set<String> =
    loadOpenApiRequirements(YAMLMapper().readTree(yamlContent))

fun loadOpenApiRequirements(root: JsonNode): Set<String> {
    val setOfRequirements = mutableSetOf<String>()
    val pathsNode = root.get("paths")
    for ((rawPath, pathItem) in pathsNode.properties()) {
        for ((method, opNode) in pathItem.properties()) {
            val operationId = opNode.get("operationId")?.asText()
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

fun escapeJsonPointer(segment: String): String =
    segment.replace("~", "~0").replace("/", "~1")
