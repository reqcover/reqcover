# Contributing to ReqCover

## General
- Follow the [Code of Conduct](CODE_OF_CONDUCT.md).
- Make sure that you are allowed to contribute.
  If you're employed, and in doubt, check with your employer or a lawyer.
- Make sure you understand and accept the [License](LICENSE).

> If you are unsure about the general section of these guidelines, check with your employer or a lawyer.

## Quality
- Make sure there is no regression.
  There is an extensive suite of tests.
  Run `make` to build and run them before submitting a pull request.
- Work test first: write a failing test, make it pass, then refactor.
- If you fix a bug, prevent regression by reproducing it first with a test.
- If you add a new feature, make sure it's tested.
- Keep commits small and use [Conventional Commits](https://www.conventionalcommits.org/) messages, for example `fix(junit): ...` or `feat(cucumber): ...`.
- Keep the history linear: rebase onto `trunk`, don't merge.
- ReqCover is written in Kotlin with 4 spaces indentation, Unix format.
- For local-only Makefile overrides, use a `.User.mk` file in the repository root; it's git-ignored and automatically included.

> If you are unsure about the quality section of these guidelines, you can still submit a pull request.
> You will receive help to meet the quality guidelines.
