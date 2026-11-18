<p align="center">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="https://reqcover.dev/brand/reqcover-lockup-dark.svg">
    <img src="https://reqcover.dev/brand/reqcover-lockup-light.svg" alt="ReqCover" height="56">
  </picture>
</p>

<p align="center"><b>Every requirement, proven by a passing test.</b></p>

<p align="center">
  <a href="https://github.com/reqcover/reqcover/releases/latest"><img alt="Latest release" src="https://img.shields.io/github/v/release/reqcover/reqcover"></a>
  <a href="https://github.com/reqcover/reqcover/actions/workflows/gradle.yml"><img alt="Build" src="https://github.com/reqcover/reqcover/actions/workflows/gradle.yml/badge.svg"></a>
  <a href="LICENSE"><img alt="License: BSD-3-Clause" src="https://img.shields.io/badge/license-BSD--3--Clause-blue"></a>
</p>

ReqCover links your tests to the requirements in your API specification, reports which requirements are covered by a passing test, and fails the build when coverage is below your minimum.
It works with **JUnit Jupiter** (JUnit 5 and 6), **TestNG** and **Cucumber**, with Gradle or Maven, for Java, Kotlin and other JVM languages.

**Documentation: [reqcover.dev](https://reqcover.dev/)** · [Getting started](https://reqcover.dev/getting-started/) · [Release notes](https://reqcover.dev/releases/) · [Upgrading](https://reqcover.dev/migration/)

## How it works

1. **Describe your requirements.** Point ReqCover at an OpenAPI document. Each operation and response becomes a requirement:

   ```yaml
   paths:
     /users:
       get:
         operationId: getUsers
         responses:
           '200':
             description: All users          # -> #getUsers/200
     /users/{id}:
       delete:
         operationId: deleteUser
         responses:
           '204':
             description: Deleted
             x-requirements:
               audit: The deletion is logged # -> #deleteUser/204/audit
   ```

   An operation without `responses` gives `#operationId`; a response gives `#operationId/status`; each key under `x-requirements` gives `#operationId/status/key`.

2. **Link your tests.** Annotate a test (or a whole test class) with `@ForRequirement`, or tag a Cucumber scenario with `@req:`:

   ```java
   @Test
   @ForRequirement(id = "#getUsers/200")   // Kotlin: @ForRequirement("#getUsers/200")
   void listsUsers() { }
   ```

   ```gherkin
   @req:#getUsers/200
   Scenario: list users
   ```

3. **Run your build.** Only passing tests count. ReqCover writes an HTML and an XML report and checks the coverage gate:

   ```text
   WARNING: The following requirements were not covered by any test: [#deleteUser/204/audit]
   INFO: Requirements coverage: 1 out of 2 = 50.0%
   SEVERE: Requirements coverage is below the minimum required coverage of 100.0%. Actual coverage: 50.0%
   SEVERE: Exiting with error code 1 due to insufficient requirements coverage.
   ```

## Quick start (Gradle + JUnit)

ReqCover is published to GitHub Packages, which requires a GitHub token with the `read:packages` scope even for public packages.
Put `gpr.user` and `gpr.key` in `~/.gradle/gradle.properties`, then in `build.gradle.kts`:

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
    testImplementation("dev.reqcover:reqcover-engine-junit-jupiter:0.0.12")
    testImplementation("dev.reqcover:reqcover-source-openapi:0.0.12")
    testImplementation("dev.reqcover:reqcover-reporter-html:0.0.12")
    testImplementation("dev.reqcover:reqcover-reporter-xml:0.0.12")
}
```

Put your OpenAPI file on the test classpath, for example `src/test/resources/requirements.yaml`, and configure ReqCover in `src/test/resources/junit-platform.properties`:

```properties
reqCover.requirementsUris=requirements.yaml
reqCover.minimumRequiredCoveragePercent=100
```

Run `./gradlew test`. The JUnit listener registers itself; no extension or plugin is needed.
The reports are written to `build/reports/tests/reqcover/`.
The full walkthrough is in [Getting started](https://reqcover.dev/getting-started/).

## Other setups

| Setup | What changes | Guide |
|---|---|---|
| TestNG | Use `reqcover-engine-testng`; configure with system properties or `testng.xml` suite parameters | [TestNG](https://reqcover.dev/guides/testng/) |
| Cucumber | Use `reqcover-engine-cucumber`; register `cucumber.plugin=dev.reqcover.cucumber.RequirementsCoverageListener`; tag scenarios with `@req:<id>` | [Cucumber](https://reqcover.dev/guides/cucumber/) |
| Maven | Use the `-jvm` artifacts (for example `reqcover-engine-junit-jupiter-jvm`) and set `reqCover.reportsDir` to `${project.build.directory}/reqcover` | [Maven](https://reqcover.dev/guides/maven/) |

## Settings

| Setting | Meaning | Default |
|---|---|---|
| `reqCover.requirementsUris` | Comma-separated classpath locations of the requirements files | none: a warning is logged and no requirements are expected |
| `reqCover.minimumRequiredCoveragePercent` | Minimum coverage in percent, a number from 0 to 100 | `0.0` |
| `reqCover.failIfBelowMinimum` | Fail the build below the minimum (`true` or `false`) | `true` |
| `reqCover.reportsDir` | Directory for the HTML and XML reports | `build/reports/tests/reqcover` |
| `reqCover.cucumber.tagPrefix` | Cucumber only: prefix of requirement tags | `@req:` |

A JVM system property always wins; otherwise the framework's own configuration is used (`junit-platform.properties`, `testng.xml` or `cucumber.properties`).
A missing or malformed requirements file, or an invalid setting value, fails the build with an error.
Details: [Configuration](https://reqcover.dev/configuration/) and [Troubleshooting](https://reqcover.dev/troubleshooting/).

## Modules

| Module | Purpose |
|---|---|
| `reqcover-engine-junit-jupiter` | JUnit Jupiter listener (JUnit 5 and 6), registers itself |
| `reqcover-engine-testng` | TestNG listener, registers itself |
| `reqcover-engine-cucumber` | Cucumber plugin (Cucumber 7 and 8) |
| `reqcover-source-openapi` | Reads requirements from OpenAPI documents |
| `reqcover-reporter-html`, `reqcover-reporter-xml` | Write the coverage report |
| `reqcover-core`, `reqcover-engine` | The `@ForRequirement` annotation and the shared engine with its extension points for [sources](https://reqcover.dev/sources/) and [reporters](https://reqcover.dev/reporters/) |

All modules have the group `dev.reqcover`. Maven users add the `-jvm` suffix to the artifact name.

## Contributing

Contributions are welcome; see [CONTRIBUTING.md](CONTRIBUTING.md) and the [Code of Conduct](CODE_OF_CONDUCT.md).
Build and test with `make`. To report a security problem, see [SECURITY.md](SECURITY.md).

## Sponsors

ReqCover is sponsored by [eSol GmbH](https://www.esol.de/) and [Nelkinda Software Craft Pvt Ltd](https://nelkinda.com/).

## License

ReqCover is licensed under the BSD 3-Clause License; see [LICENSE](LICENSE).
