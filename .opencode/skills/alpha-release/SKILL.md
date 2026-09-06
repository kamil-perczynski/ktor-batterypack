---
name: alpha-release
description: Use when the user wants to cut, prepare, or publish an alpha release of ktor-batterypack. Bumps the alpha version from git tags, updates the `ktor-batterypack` version in the version catalog (`gradle/libs.versions.toml`), creates and pushes a release/<version> branch, then optionally tags the commit to trigger artifact publishing to GitHub Packages. Keywords: alpha release, release, publish artifacts, bump version, tag release.
---

# Alpha Release

Cut an alpha release of the `ktor-batterypack` monorepo. Follow the steps below strictly, in order. Never skip a user confirmation step.

## Step 1 — Determine the next version

1. Run `git fetch --tags` to make sure local tags are up to date.
2. List existing tags sorted descending: `git tag --sort=-v:refname`.
3. Tags follow the pattern `X.Y.Z-alpha` (no `v` prefix, e.g. `0.0.8-alpha`).
4. The next version is the highest existing alpha with the patch segment incremented by one: `X.Y.(Z+1)-alpha`.
5. If no alpha tags exist, default to `0.0.1-alpha`.

## Step 2 — Confirm the version with the user

Use the Question tool to ask the user to confirm the proposed next version. Offer the computed version as the recommended option and allow a custom version. Do NOT proceed until the user confirms. If the user picks a different version, use that one for all subsequent steps.

## Step 3 — Update the version

Edit `gradle/libs.versions.toml` and change the `ktor-batterypack` entry in the `[versions]` section to the confirmed version, e.g.:

```toml
ktor-batterypack = "0.0.9-alpha"
```

This single catalog entry drives the version everywhere, so no other files need changing:

- Root `build.gradle.kts` reads it via `version = libs.versions.ktorBatterypack`.
- `ktor-batterypack-gradle-plugin/build.gradle.kts` reads it via `version = libs.versions.ktor.batterypack`.
- Subprojects inherit it via `allprojects { version = rootProject.version }`.
- All `ktor-batterypack-*` library/plugin coordinates reference it via `version.ref = "ktor-batterypack"`.

Verify with `git diff` that only this one line changed.


## Step 4 — Create the release branch and commit

1. Create and switch to a new branch: `git checkout -b release/<version>` (e.g. `release/0.0.9-alpha`).
2. Stage only `gradle/libs.versions.toml` and commit with the exact message: `release: <version>` (e.g. `release: 0.0.9-alpha`).

## Step 5 — Push the branch

Push the release branch to origin: `git push -u origin release/<version>`.

## Step 6 — Confirm tagging (publishing trigger)

Use the Question tool to ask the user whether to tag the current commit as `<version>` and push the tag. Explain that **pushing the tag triggers the `Publish packages` workflow** (`.github/workflows/publish.yaml`), which runs `./gradlew publish` and physically publishes all `ktor-batterypack-*` artifacts to GitHub Packages. This action cannot be undone easily — do NOT proceed without explicit confirmation.

If the user declines, stop here and report that the release branch is pushed but not tagged.

## Step 7 — Tag and push

Only after explicit confirmation:

1. `git tag <version>` on the current commit (the release commit on the release branch).
2. `git push origin <version>`.

Report the published version, the release branch name, and the tag. Remind the user to check the `Publish packages` workflow run on GitHub.

## Rules

- Never push tags without the Step 6 confirmation.
- Never force-push or reuse an existing tag. If the tag already exists, stop and ask the user.
- Never commit anything other than the version change in `gradle/libs.versions.toml` on the release branch.
- Tags and branch names use the bare version (`0.0.9-alpha`), never prefixed with `v`.
