# ReqCover
ReqCover is a collection of tools and libraries to track requirements coverage from test cases.
Currently, it supports JVM languages (Java, Kotlin, Scala, Groovy) and tests running in JUnit 5 Jupiter.

## Architecture
ReqCover consists of the following components:
* API for linking test cases with requirements
* Framework for managing requirements coverage with
  * SPI for loading requirements from external sources
* JUnit 5 Jupiter extension for tracking and reporting requirements coverage
