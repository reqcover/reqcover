package dev.reqcover.source.openapi

import dev.reqcover.engine.spi.RequirementsSource
import dev.reqcover.engine.loadRequirements
import java.util.ServiceLoader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OpenApiRequirementsSourceTest {
    @Test
    fun `ServiceLoader finds OpenApiRequirementsSource`() {
        val implementations = ServiceLoader.load(RequirementsSource::class.java).toList()
        assertTrue(implementations.any { it::class == OpenApiRequirementsSource::class })
    }

    @Test
    fun `escapeJsonPointer escapes special characters for JSON Pointers`() {
        assertEquals("https:~1~1example.com~1~0user", escapeJsonPointer("https://example.com/~user"))
    }

    @Test
    fun `an empty document has no requirements`() {
        val requirements = loadOpenApiRequirementsFromString(
            """
            openapi: 3.0.0
            info:
              title: Empty API
              version: 1.0.0
            paths: {}
            """.trimIndent()
        )
        assertEquals(emptySet(), requirements)
    }

    @Test
    fun `given no responses, the operations are the requirements`() {
        val requirements = loadOpenApiRequirementsFromString(
            """
            openapi: 3.0.0
            info:
              title: Test API
              version: 1.0.0
            paths:
              /users:
                get:
                  operationId: getUsers
              /users/{id}:
                get:
                  operationId: getUserById
            """.trimIndent()
        )
        assertEquals(setOf("#getUsers", "#getUserById"), requirements)
    }

    @Test
    fun `given multiple response codes, the response codes are the requirements`() {
        val requirements = loadOpenApiRequirementsFromString(
            """
            openapi: 3.0.0
            info:
              title: Test API
              version: 1.0.0
            paths:
              /users:
                get:
                  operationId: getUsers
                  responses:
                    '200':
                      description: OK
                    '404':
                      description: Not Found
              /users/{id}:
                get:
                  operationId: getUserById
                  responses:
                    '200':
                      description: OK
                    '400':
                      description: Bad Request
            """.trimIndent()
        )
        assertEquals(setOf("#getUsers/200", "#getUsers/404", "#getUserById/200", "#getUserById/400"), requirements)
    }

    @Test
    fun `given explicit requirements, those are the requirements`() {
        val requirements = loadOpenApiRequirementsFromString(
            """
            openapi: 3.0.0
            info:
              title: Test API
              version: 1.0.0
            paths:
              /actuator/health:
                get:
                  operationId: getHealth
                  responses:
                    '200':
                      description: OK
                    '500':
                      description: service outage
                      x-requirements:
                        database: If the database is down, the service is unavailable.
                        downstream: If the downstream service is down, the service is unavailable.
            """.trimIndent()
        )
        assertEquals(setOf("#getHealth/200", "#getHealth/500/database", "#getHealth/500/downstream"), requirements)
    }

    @Test
    fun `loads requirements from the classpath`() {
        val requirements = loadRequirements("openapi-requirements-example.yaml")
        assertEquals(setOf(
            "#getVersion/200",
            "#getHealth/200",
            "#getHealth/500/database",
            "#getHealth/500/downstream",
        ), requirements)
    }
}
