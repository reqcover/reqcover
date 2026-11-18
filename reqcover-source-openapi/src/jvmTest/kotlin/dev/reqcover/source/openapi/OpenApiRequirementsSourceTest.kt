package dev.reqcover.source.openapi

import dev.reqcover.engine.UnsupportedFormat
import dev.reqcover.engine.spi.RequirementsSource
import kotlin.test.assertFailsWith
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
        assertEquals("https:~1~1example.com~1~0user", OpenApiRequirementsSource().escapeJsonPointer("https://example.com/~user"))
    }

    @Test
    fun `an empty document has no requirements`() {
        val requirements = OpenApiRequirementsSource().loadOpenApiRequirementsFromString(
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
        val requirements = OpenApiRequirementsSource().loadOpenApiRequirementsFromString(
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
        val requirements = OpenApiRequirementsSource().loadOpenApiRequirementsFromString(
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
        val requirements = OpenApiRequirementsSource().loadOpenApiRequirementsFromString(
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

    @Test
    fun `rejects a YAML document without paths as an unsupported format`() {
        val error = assertFailsWith<UnsupportedFormat> { OpenApiRequirementsSource().load("not-openapi.yaml") }
        assertTrue(error.message!!.contains("not-openapi.yaml"))
    }

    @Test
    fun `rejects a malformed YAML document as an unsupported format`() {
        val error = assertFailsWith<UnsupportedFormat> { OpenApiRequirementsSource().load("malformed.yaml") }
        assertTrue(error.message!!.contains("malformed.yaml"))
    }

    @Test
    fun `rejects an empty file as an unsupported format`() {
        assertFailsWith<UnsupportedFormat> { OpenApiRequirementsSource().load("empty.yaml") }
    }

    @Test
    fun `the engine reports a non-OpenAPI file as an unsupported format`() {
        assertFailsWith<UnsupportedFormat> { loadRequirements("not-openapi.yaml") }
    }

    private fun printedWhile(action: () -> Unit): String {
        val original = System.out
        val captured = java.io.ByteArrayOutputStream()
        System.setOut(java.io.PrintStream(captured, true))
        try {
            action()
        } finally {
            System.setOut(original)
        }
        return captured.toString()
    }

    @Test
    fun `path-level fields that are not operations are ignored without a warning`() {
        lateinit var requirements: Set<String>
        val output = printedWhile {
            requirements = OpenApiRequirementsSource().loadOpenApiRequirementsFromString(
                """
                openapi: 3.0.0
                info:
                  title: Test API
                  version: 1.0.0
                paths:
                  /users/{id}:
                    summary: A single user
                    description: Everything about one user
                    servers:
                      - url: https://example.com
                    parameters:
                      - name: id
                        in: path
                        required: true
                    x-internal: true
                    get:
                      operationId: getUser
                      responses:
                        '200':
                          description: OK
                """.trimIndent()
            )
        }

        assertEquals(setOf("#getUser/200"), requirements)
        assertEquals("", output)
    }

    @Test
    fun `every HTTP method is an operation`() {
        val methods = listOf("get", "put", "post", "delete", "options", "head", "patch", "trace")
        val requirements = OpenApiRequirementsSource().loadOpenApiRequirementsFromString(
            "openapi: 3.0.0\ninfo:\n  title: Test API\n  version: 1.0.0\npaths:\n  /x:\n" +
                methods.joinToString("") { "    $it:\n      operationId: ${it}Op\n" }
        )

        assertEquals(methods.map { "#${it}Op" }.toSet(), requirements)
    }

    @Test
    fun `an operation without operationId is skipped with a warning`() {
        lateinit var requirements: Set<String>
        val output = printedWhile {
            requirements = OpenApiRequirementsSource().loadOpenApiRequirementsFromString(
                """
                openapi: 3.0.0
                info:
                  title: Test API
                  version: 1.0.0
                paths:
                  /users:
                    get:
                      responses:
                        '200':
                          description: OK
                """.trimIndent()
            )
        }

        assertEquals(emptySet(), requirements)
        assertEquals("Warning: Operation ID not found for path '/users' and method 'get'.", output.trim())
    }
}
