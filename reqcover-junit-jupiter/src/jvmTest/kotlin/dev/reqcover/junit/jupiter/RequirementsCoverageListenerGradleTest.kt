package dev.reqcover.junit.jupiter

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties
import kotlin.streams.toList

class RequirementsCoverageListenerGradleTest {
    private val fixtureDirectory = repositoryRoot()
        .resolve("reqcover-junit-jupiter/src/jvmTest/fixtures/legacy-config-check")

    @Test
    fun `fails the Gradle test task when reqCover requirementsUri is configured`() {
        val projectDir = Files.createTempDirectory("reqcover-gradle-test")
        copyFixtureProject(projectDir)

        val result = runGradleBuildAndFail(projectDir)

        assertTrue(result.output.contains("> Task :legacyConfigCheck FAILED"), result.output)
        assertTrue(
            result.output.contains(
                "The reqCover.requirementsUri configuration parameter has been removed. Use reqCover.requirementsUris instead."
            ),
            result.output,
        )
    }

    private fun runGradleBuildAndFail(projectDir: Path): BuildFailureResult =
        gradleTestKitClassLoader.let { classLoader ->
            val gradleRunnerClass = classLoader.loadClass("org.gradle.testkit.runner.GradleRunner")
            val buildResultClass = classLoader.loadClass("org.gradle.testkit.runner.BuildResult")
            val runner = gradleRunnerClass.getMethod("create").invoke(null)
            val configuredRunner = gradleRunnerClass
                .getMethod("withProjectDir", File::class.java)
                .invoke(runner, projectDir.toFile())
                .let {
                    gradleRunnerClass.getMethod("withArguments", Array<String>::class.java)
                        .invoke(it, arrayOf("legacyConfigCheck", "--stacktrace", "--console=plain") as Any)
                }
                .let {
                    gradleRunnerClass.getMethod("withGradleVersion", String::class.java)
                        .invoke(it, currentGradleVersion())
                }
            val buildResult = gradleRunnerClass.getMethod("buildAndFail").invoke(configuredRunner)
            val output = buildResultClass.getMethod("getOutput").invoke(buildResult) as String
            BuildFailureResult(output = output)
        }

    private fun copyFixtureProject(projectDir: Path) {
        Files.walk(fixtureDirectory).use { paths ->
            paths.forEach { source ->
                val relativePath = fixtureDirectory.relativize(source)
                val target = projectDir.resolve(relativePath.toString())
                if (Files.isDirectory(source)) {
                    Files.createDirectories(target)
                } else {
                    Files.createDirectories(target.parent)
                    Files.copy(source, target)
                }
            }
        }
        val buildFile = projectDir.resolve("build.gradle.kts")
        Files.writeString(
            buildFile,
            Files.readString(buildFile).replace(
                "__REQCOVER_TEST_CLASSPATH__",
                fixtureClasspathEntries().joinToString(",\n        ") { "\"$it\"" },
            ),
        )
    }

    private val gradleTestKitClassLoader: URLClassLoader by lazy {
        val gradleHomeDir = locateGradleHomeDir()
        val urls = buildList {
            Files.list(gradleHomeDir.resolve("lib")).use { paths ->
                addAll(paths.filter { it.toString().endsWith(".jar") }.map { it.toUri().toURL() }.toList())
            }
            Files.list(gradleHomeDir.resolve("lib/plugins")).use { paths ->
                addAll(paths.filter { it.toString().endsWith(".jar") }.map { it.toUri().toURL() }.toList())
            }
        }
        URLClassLoader(urls.toTypedArray(), javaClass.classLoader)
    }

    private fun locateGradleHomeDir(): Path {
        val version = currentGradleVersion()
        val gradleUserHome = Path.of(System.getProperty("user.home"), ".gradle", "wrapper", "dists")
        Files.walk(gradleUserHome).use { paths ->
            val gradleTestKitJar = paths
                .filter { it.fileName.toString() == "gradle-test-kit-$version.jar" }
                .findFirst()
                .orElseThrow()
            return gradleTestKitJar.parent.parent.parent
        }
    }

    private fun currentGradleVersion(): String {
        val wrapperProperties = findWrapperProperties()
        val properties = Properties()
        Files.newInputStream(wrapperProperties).use(properties::load)
        val distributionUrl = properties.getProperty("distributionUrl")
        return Regex("""gradle-([0-9.]+)-""").find(distributionUrl)?.groupValues?.get(1)
            ?: error("Could not determine Gradle version from $distributionUrl")
    }

    private fun findWrapperProperties(): Path {
        var current: Path? = Path.of(System.getProperty("user.dir")).toAbsolutePath()
        while (current != null) {
            val candidate = current.resolve("gradle/wrapper/gradle-wrapper.properties")
            if (Files.exists(candidate)) {
                return candidate
            }
            current = current.parent
        }
        error("Could not find gradle/wrapper/gradle-wrapper.properties from ${System.getProperty("user.dir")}")
    }

    private fun repositoryRoot(): Path =
        findWrapperProperties().parent.parent.parent

    private fun fixtureClasspathEntries(): List<String> {
        val repositoryRoot = repositoryRoot().toAbsolutePath().normalize().toString()
        return buildList {
            addAll(reqcoverModuleJarEntries())
            addAll(runtimeClasspathEntries().filterNot { it.startsWith(repositoryRoot) })
        }.distinct()
    }

    private fun reqcoverModuleJarEntries(): List<String> {
        val repositoryRoot = repositoryRoot()
        val jarDirectories = listOf(
            repositoryRoot.resolve("reqcover-core/build/libs"),
            repositoryRoot.resolve("reqcover-engine/build/libs"),
            repositoryRoot.resolve("reqcover-junit-jupiter/build/libs"),
        )
        return jarDirectories.map { jarDirectory ->
            Files.list(jarDirectory).use { paths ->
                paths
                    .filter { path -> path.fileName.toString().contains("-jvm-") && path.fileName.toString().endsWith(".jar") }
                    .findFirst()
                    .orElseThrow()
                    .toAbsolutePath()
                    .normalize()
                    .toString()
                    .replace("\\", "\\\\")
            }
        }
    }

    private fun runtimeClasspathEntries(): List<String> =
        System.getProperty("java.class.path")
            .split(File.pathSeparator)
            .map { Path.of(it) }
            .filter { Files.exists(it) }
            .map { it.toAbsolutePath().normalize().toString().replace("\\", "\\\\") }

    private data class BuildFailureResult(
        val output: String,
    )
}
