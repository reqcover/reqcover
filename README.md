# ReqCover

https://reqcover.dev/
https://github.com/reqcover/reqcover

ReqCover is a collection of tools and libraries to track requirements coverage from test cases.
Currently, it supports JVM languages (Java, Kotlin, Scala, Groovy) and tests running in JUnit 5 Jupiter, TestNG or Cucumber.

## Architecture
ReqCover consists of the following components:
* API for linking test cases with requirements
* Framework for managing requirements coverage with
  * SPI for loading requirements from external sources
* JUnit 5 Jupiter listener for tracking and reporting requirements coverage
* TestNG listener for tracking and reporting requirements coverage
* Cucumber plugin for tracking and reporting requirements coverage

## Installation
ReqCover is published to GitHub Packages at `https://maven.pkg.github.com/reqcover/reqcover`.
GitHub Packages requires authentication even for public packages: use your GitHub username and a personal access token with the `read:packages` scope.

Add to your test classpath:
* the engine for your test framework: `reqcover-engine-junit-jupiter`, `reqcover-engine-testng` or `reqcover-engine-cucumber`
* a requirements source: `reqcover-source-openapi`
* one or more reporters: `reqcover-reporter-html`, `reqcover-reporter-xml`

Gradle (Kotlin DSL):
```kotlin
repositories {
    mavenCentral()
    maven("https://maven.pkg.github.com/reqcover/reqcover") {
        credentials {
            username = providers.gradleProperty("gpr.user").orNull ?: System.getenv("GITHUB_ACTOR")
            password = providers.gradleProperty("gpr.key").orNull ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    testImplementation("dev.reqcover:reqcover-engine-junit-jupiter:0.0.10") // or reqcover-engine-testng
    testImplementation("dev.reqcover:reqcover-source-openapi:0.0.10")
    testImplementation("dev.reqcover:reqcover-reporter-html:0.0.10")
    testImplementation("dev.reqcover:reqcover-reporter-xml:0.0.10")
}
```

Maven uses the JVM artifacts, which have a `-jvm` suffix.
Configure the repository credentials in `~/.m2/settings.xml` under the server id `github`.
```xml
<repositories>
    <repository>
        <id>github</id>
        <url>https://maven.pkg.github.com/reqcover/reqcover</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>dev.reqcover</groupId>
        <artifactId>reqcover-engine-junit-jupiter-jvm</artifactId> <!-- or reqcover-engine-testng-jvm -->
        <version>0.0.10</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>dev.reqcover</groupId>
        <artifactId>reqcover-source-openapi-jvm</artifactId>
        <version>0.0.10</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>dev.reqcover</groupId>
        <artifactId>reqcover-reporter-html-jvm</artifactId>
        <version>0.0.10</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

With Maven, set `reqCover.reportsDir` so the reports go to `target/` instead of `build/`, for example in the Surefire configuration:
```xml
<systemPropertyVariables>
    <reqCover.reportsDir>${project.build.directory}/reqcover</reqCover.reportsDir>
</systemPropertyVariables>
```

Annotate tests with `@ForRequirement`; only passing tests count as verifying their requirements.
A test can carry several `@ForRequirement` annotations.

Java:
```java
@Test
@ForRequirement(id = "#getUsers/200")
void listsUsers() { }
```

Kotlin:
```kotlin
@Test
@ForRequirement("#getUsers/200")
fun `lists users`() { }
```

## JUnit 5 Jupiter
The listener registers itself automatically through the JUnit Platform.
Configure ReqCover in `src/test/resources/junit-platform.properties` (or as JVM system properties, which take precedence):
```properties
reqCover.requirementsUris=requirements.yaml
reqCover.minimumRequiredCoveragePercent=100
```

| Setting | Description | Default |
|---|---|---|
| `reqCover.requirementsUris` | Comma-separated classpath locations of the requirements specifications | none (a warning is logged) |
| `reqCover.minimumRequiredCoveragePercent` | Minimum required requirements coverage in percent | `0.0` |
| `reqCover.failIfBelowMinimum` | Whether to fail the build if the coverage is below the minimum | `true` |
| `reqCover.reportsDir` | Directory for the HTML and XML reports, relative to the working directory | `build/reports/tests/reqcover` |

The former `reqCover.requirementsUri` (singular) setting has been removed; using it fails the build.

## TestNG
Add `reqcover-engine-testng` and a requirements source (for example `reqcover-source-openapi`) to the test classpath.
The listener registers itself automatically; no `@Listeners` annotation or `testng.xml` entry is needed.

ReqCover is configured through JVM system properties or `testng.xml` suite parameters:

| Setting | Description | Default |
|---|---|---|
| `reqCover.requirementsUris` | Comma-separated classpath locations of the requirements specifications | none (a warning is logged) |
| `reqCover.minimumRequiredCoveragePercent` | Minimum required requirements coverage in percent | `0.0` |
| `reqCover.failIfBelowMinimum` | Whether to fail the build if the coverage is below the minimum | `true` |
| `reqCover.reportsDir` | Directory for the HTML and XML reports, relative to the working directory | `build/reports/tests/reqcover` |

Gradle:
```kotlin
tasks.test {
    useTestNG()
    systemProperty("reqCover.requirementsUris", "requirements.yaml")
    systemProperty("reqCover.minimumRequiredCoveragePercent", "100")
}
```

Maven Surefire:
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <systemPropertyVariables>
            <reqCover.requirementsUris>requirements.yaml</reqCover.requirementsUris>
            <reqCover.minimumRequiredCoveragePercent>100</reqCover.minimumRequiredCoveragePercent>
            <reqCover.reportsDir>${project.build.directory}/reqcover</reqCover.reportsDir>
        </systemPropertyVariables>
    </configuration>
</plugin>
```

`testng.xml`:
```xml
<suite name="My suite">
    <parameter name="reqCover.requirementsUris" value="requirements.yaml"/>
    <parameter name="reqCover.minimumRequiredCoveragePercent" value="100"/>
    ...
</suite>
```

With several suites, `reqCover.reportsDir` is taken from the first suite that sets it, the requirements URIs of all suites are combined, and the strictest coverage settings apply: the highest minimum, and the build fails if any suite asks for that (a suite that does not set `reqCover.failIfBelowMinimum` counts as `true`).
This rule is provisional and may change based on user feedback.

Annotate TestNG tests with `@ForRequirement` exactly as with JUnit; only passing tests count as verifying their requirements.

## Cucumber
Add `reqcover-engine-cucumber` and a requirements source to the test classpath.
ReqCover works with Cucumber 7 and 8 and uses your project's Cucumber version.

Register the plugin and configure ReqCover in `junit-platform.properties` (when running Cucumber on the JUnit Platform) or `cucumber.properties`:
```properties
cucumber.plugin=dev.reqcover.cucumber.RequirementsCoverageListener
reqCover.requirementsUris=requirements.yaml
reqCover.minimumRequiredCoveragePercent=100
```

Link a scenario to a requirement with a tag. A passing scenario covers every requirement it is tagged with; tags on the feature or rule apply to all its scenarios.
```gherkin
Feature: Users

  @req:#getUsers/200
  Scenario: list users
    ...
```

| Setting | Description | Default |
|---|---|---|
| `reqCover.cucumber.tagPrefix` | Prefix of the tags that name a requirement; the rest of the tag is the requirement ID | `@req:` |
| `reqCover.requirementsUris` | Comma-separated classpath locations of the requirements specifications | none (a warning is logged) |
| `reqCover.minimumRequiredCoveragePercent` | Minimum required requirements coverage in percent | `0.0` |
| `reqCover.failIfBelowMinimum` | Whether to fail the build if the coverage is below the minimum | `true` |
| `reqCover.reportsDir` | Directory for the HTML and XML reports, relative to the working directory | `build/reports/tests/reqcover` |

## When a requirement counts as covered
A requirement is covered when at least one test annotated with `@ForRequirement` for it passes.
For Cucumber, a passing scenario covers the requirements of its tags.
For parameterized, repeated (`@RepeatedTest`) and data-provider tests, and for the dynamic tests of a `@TestFactory`, one passing run is enough; failing, skipped and aborted runs do not count.
This rule is provisional and may change based on user feedback.

## Configuration precedence
Every ReqCover setting (`reqCover.requirementsUris`, `reqCover.minimumRequiredCoveragePercent`, `reqCover.failIfBelowMinimum`, `reqCover.reportsDir`, and for Cucumber `reqCover.cucumber.tagPrefix`) is looked up in this order; the first place that sets it wins:

| Order | JUnit 5 Jupiter | TestNG | Cucumber |
|---|---|---|---|
| 1 | JVM system property (`-DreqCover.…`) | JVM system property (`-DreqCover.…`) | JVM system property (`-DreqCover.…`) |
| 2 | JUnit configuration parameter (`junit-platform.properties`, launcher or build tool configuration) | `testng.xml` suite `<parameter>` | `cucumber.properties` |
| 3 | Built-in default | Built-in default | `junit-platform.properties` |
| 4 | | | Built-in default |

## Requirements file problems
These rules apply to JUnit, TestNG and Cucumber alike:

| Situation | Behaviour |
|---|---|
| A requirements file is not found | Error, the build fails |
| A requirements file is malformed or not a supported format | Error, the build fails |
| `reqCover.requirementsUris` is not set or is empty | Warning, coverage is not checked |
| A requirements file contains no requirements (e.g. OpenAPI `paths: {}`) | Warning, the file counts as fully covered |

The two warning cases are provisional and may change based on user feedback.
If you have an opinion, please comment on https://github.com/reqcover/reqcover/issues.

## Glossary
* **Requirement** - a piece of functionality that should be implemented in the system
* **Test case** - a piece of code that tests a requirement
* **Coverage** - a set of test cases that cover a requirement
* **expectedRequirements** - a set of requirements that should be verified by a test case
* **verifiedRequirements** - a set of requirements that are verified by a test case

## Sponsors
This project is sponsored by:
* eSol GmbH - https://www.esol.de
* Nelkinda Software Craft Pvt Ltd - https://nelkinda.com
