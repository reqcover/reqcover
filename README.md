# ReqCover

https://github.com/reqcover/reqcover

ReqCover is a collection of tools and libraries to track requirements coverage from test cases.
Currently, it supports JVM languages (Java, Kotlin, Scala, Groovy) and tests running in JUnit 5 Jupiter.

## Architecture
ReqCover consists of the following components:
* API for linking test cases with requirements
* Framework for managing requirements coverage with
  * SPI for loading requirements from external sources
* JUnit 5 Jupiter extension for tracking and reporting requirements coverage

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
