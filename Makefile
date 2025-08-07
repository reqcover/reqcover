export GIT_SHA?=$(shell git rev-parse HEAD)
export GIT_TIMESTAMP?=$(shell TZ=UTC0 git show --quiet --date='format-local:%Y-%m-%dT%H:%M:%SZ' --format=%cd HEAD)
export BUILD_NUMBER?=UnknownLocal
export BUILD_TIMESTAMP?=$(shell date -u +"%Y-%m-%dT%H:%M:%SZ")

GRADLE:=echo -n | ./gradlew </dev/null

.PHONY: all
## all:	Builds and tests the project.
all:
	$(GRADLE) build

.PHONY: pipeline
## pipeline:	Performs the same steps as the build pipeline.
pipeline: all

.PHONY: stop-gradle
## stop-gradle:	Stops the Gradle daemon
stop-gradle:
	$(GRADLE) --stop

.PHONY: version
## version:	Print the current version.
version:
	$(GRADLE) \
		currentVersion \

.PHONY: release-patch
## release-patch:	Creates a patch release.
release-patch:
	$(GRADLE) \
		release -Drelease.versionIncrememnter=incrementPatch \

.PHONY: release-minor
## release-minor:	Creates a minor release.
release-minor:
	$(GRADLE) \
		release -Drelease.versionIncrememnter=incrementMinor \

.PHONY: release-major
## release-major:	Creates a major release.
release-major:
	$(GRADLE) \
		release -Drelease.versionIncrememnter=incrementMajor \

.PHONY: publish
## publish:	Publishes a release.
publish:
	$(GRADLE) \
		publish \

.PHONY: clean
## clean:	Removes all generated files.
clean::
	$(GRADLE) clean

.PHONY: help
## help:	Prints this help text.
help:
	@sed -En 's/^## ?//p' $(MAKEFILE_LIST)

-include ~/.User.mk
-include .User.mk
