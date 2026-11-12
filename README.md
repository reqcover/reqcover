# ReqCover

https://reqcover.dev/
https://github.com/reqcover/reqcover

ReqCover is a collection of tools and libraries to track requirements coverage from test cases.
Currently, it supports JVM languages (Java, Kotlin, Scala, Groovy) and tests running in JUnit 5 Jupiter or TestNG.

## Architecture
ReqCover consists of the following components:
* API for linking test cases with requirements
* Framework for managing requirements coverage with
  * SPI for loading requirements from external sources
* JUnit 5 Jupiter extension for tracking and reporting requirements coverage
* TestNG listener for tracking and reporting requirements coverage

## TestNG
Add `reqcover-engine-testng` and a requirements source (for example `reqcover-source-openapi`) to the test classpath.
The listener registers itself automatically; no `@Listeners` annotation or `testng.xml` entry is needed.

TestNG has no equivalent of `junit-platform.properties`, so ReqCover is configured through JVM system properties:

| System property | Description | Default |
|---|---|---|
| `reqCover.requirementsUris` | Comma-separated classpath locations of the requirements specifications | none (a warning is logged) |
| `reqCover.minimumRequiredCoveragePercent` | Minimum required requirements coverage in percent | `0.0` |
| `reqCover.failIfBelowMinimum` | Whether to fail the build if the coverage is below the minimum | `true` |

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
        </systemPropertyVariables>
    </configuration>
</plugin>
```

Annotate TestNG tests with `@ForRequirement` exactly as with JUnit; only passing tests count as verifying their requirements.

## When a requirement counts as covered
A requirement is covered when at least one test annotated with `@ForRequirement` for it passes.
For parameterized, repeated (`@RepeatedTest`) and data-provider tests, and for the dynamic tests of a `@TestFactory`, one passing run is enough; failing, skipped and aborted runs do not count.
This rule is provisional and may change based on user feedback.

## Configuration precedence
Every ReqCover setting (`reqCover.requirementsUris`, `reqCover.minimumRequiredCoveragePercent`, `reqCover.failIfBelowMinimum`) is looked up in this order; the first place that sets it wins:

| Order | JUnit 5 Jupiter | TestNG |
|---|---|---|
| 1 | JVM system property (`-DreqCover.…`) | JVM system property (`-DreqCover.…`) |
| 2 | JUnit configuration parameter (`junit-platform.properties`, launcher or build tool configuration) | — |
| 3 | Built-in default | Built-in default |

## Requirements file problems
These rules apply to JUnit and TestNG alike:

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
