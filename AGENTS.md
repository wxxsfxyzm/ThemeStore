# AGENTS.md

## Purpose

This file defines repository-specific instructions for coding agents working on
**ThemeStoreX / ThemeStore**.

Use it to decide:

* where a change belongs,
* which project constraints must be preserved,
* what to verify before claiming a task is complete.

Task-specific maintainer instructions take precedence over this file. When the
request is narrow, make the smallest coherent change that satisfies it.

For substantial features, invasive refactors, or behavior changes spanning
several files, sketch a short implementation plan before editing and keep it
aligned with the actual work.

---

## Read these first when relevant

* `README.md` - project scope, supported Android version, permissions, feature
  list, and user-facing boundaries.
* `settings.gradle.kts`, `build.gradle.kts`, `app/build.gradle.kts`, and
  `gradle/libs.versions.toml` - before touching Gradle, repositories, modules,
  versions, signing, or dependencies.
* `app/src/main/AndroidManifest.xml` - before changing permissions, services,
  receivers, exported components, Shizuku provider wiring, or file access.
* `hidden-api/` - before changing hidden API declarations or Android platform
  internals consumed by the app.

Do not duplicate or contradict those files casually. Update this file only for
stable, repository-wide rules that agents should repeatedly follow.

When this file and live project files disagree, trust the live project files
first and update this file as part of the same change if the rule is meant to
remain stable.

---

## Repository overview

ThemeStoreX is a fork of ThemeStore focused on simplifying and stabilizing theme
installation workflows for personal learning. The README currently defines
Android 16 as the supported platform.

The app namespace and application id are currently `com.merak.x`; most Kotlin
source remains under `com.merak`. Do not infer package names from directory
names alone when wiring Manifest components, DI, or generated references.

The app includes:

* local theme installation,
* network theme installation,
* theme mixing,
* Shizuku-backed privileged operations,
* accessibility/broadcast interception,
* foreground keep-alive notification behavior,
* quick settings tile control for keep-alive behavior,
* logging/statistics,
* direct file access workarounds using zero-width-space aliases for
  `Android/data`.

Treat device/ROM behavior as sensitive. This project targets a narrow Android
and Xiaomi/MIUI-style environment; do not generalize behavior to other Android
versions or ROMs unless the maintainer explicitly requests it and the change is
verified.

---

## Critical project constraints

* Preserve the **Android 16 only** product boundary unless the task explicitly
  expands compatibility.
* Preserve the current `applicationId`/namespace contract unless the task is
  specifically about package identity, install compatibility, or publishing.
* Preserve the theme file access workaround based on zero-width-space aliases.
  Do not replace it with shell access, root access, or broader storage behavior
  without maintainer direction.
* Treat Shizuku, hidden APIs, accessibility services, foreground services, and
  broadcast interception as privileged/sensitive areas. Keep permission
  boundaries explicit and review Manifest wiring with the code change.
* Do not silently widen background, notification, network, storage, or
  accessibility behavior.
* The foreground keep-alive flow includes service, settings, notification, and
  QS tile state. Keep these paths consistent when changing that behavior.
* When changing behavior described in `README.md`, update it or call out the
  documentation impact in the handoff.

---

## Project layout

### Top-level areas

* `app/` - main Android application.
* `hidden-api/` - hidden API declarations/helpers consumed by the app.
* `gradle/` - Gradle wrapper and version catalog.
* `keystore/` and `keystore.properties` - local signing material/configuration.
* root-level crash dumps/logs and local binaries, when present, are local
  artifacts. Do not clean or commit them as part of unrelated work.

Active Gradle modules are declared in `settings.gradle.kts`:

* `:app`
* `:hidden-api`

Do not assume every top-level directory is an included Gradle module. Confirm
active modules in `settings.gradle.kts` before making module-level assumptions.

### Main Kotlin package map

Under `app/src/main/java/com/merak/`:

* `core/` - low-level installer, Shizuku, reflection, and platform integration
  infrastructure.
* `data/` - persisted settings, data models, repositories, and storage access.
* `di/` - Koin modules and initialization wiring.
* `receiver/` - Android broadcast receivers.
* `service/` - Android services, including keep-alive and accessibility flows.
* `ui/` - activities, Compose UI, navigation, themes, components, icons, and
  pages.
* `util/` - general helpers.

Preserve this separation. Do not move behavior into a convenient but wrong layer
just to finish faster.

The project also contains compatibility shims under
`app/src/main/java/androidx/navigationevent/compose/`. Treat these as local
copies/workarounds and avoid editing them unless the task is specifically about
NavigationEvent compatibility.

The accessibility service may be declared with compatibility-oriented package
names under `app/src/main/java/com/google/` while delegating to project service
code. Treat Manifest component names as externally visible contracts; do not
rename them casually.

---

## Build prerequisites

### Toolchain

* Use the repository Gradle Wrapper: `./gradlew ...` on Unix-like shells or
  `.\gradlew.bat ...` on Windows/PowerShell.
* The project is configured for **JDK 25**.
* Android compile and target settings currently use SDK 37 with minor version
  0; `minSdk` is 35.
* The Gradle wrapper and wrapper JAR are part of the build surface. Do not
  regenerate or upgrade them during unrelated dependency or source changes.
* Kotlin/JVM toolchains and Java compatibility are configured in Gradle. Do not
  downgrade or loosen them unless the task explicitly requires it.

### GitHub Packages authentication

The project resolves `miuix` snapshot artifacts from GitHub Packages through
`settings.gradle.kts`.

For local builds, credentials are expected outside committed source, typically
through Gradle properties or environment variables:

```properties
gpr.user=YOUR_GITHUB_USERNAME
gpr.key=YOUR_PERSONAL_ACCESS_TOKEN
```

or:

```text
GITHUB_ACTOR=YOUR_GITHUB_USERNAME
GITHUB_TOKEN=YOUR_TOKEN
```

The token needs `read:packages` access. Never commit credentials, inline them
into tracked files, or weaken the existing credential handling.

### Signing

`app/build.gradle.kts` reads `keystore.properties` and uses it for both debug and
release signing configs. Treat signing setup as sensitive local configuration.
Do not modify signing, keystore paths, passwords, or release minification
behavior unless the maintainer explicitly asks for that.

---

## Default verification

### Standard smoke build

For changes that can affect app compilation, resources, dependencies, manifest
wiring, DI, or Kotlin source, run:

```bash
./gradlew assembleDebug
```

On Windows/PowerShell:

```powershell
.\gradlew.bat assembleDebug
```

For hidden API or module-level Gradle changes, include the relevant module task
or run the root smoke build above.

### Narrow verification

Use narrower tasks when the change is isolated and they give faster feedback,
for example:

```bash
./gradlew :app:compileDebugKotlin
./gradlew :hidden-api:compileDebugJavaWithJavac
```

If tests are relevant, run the closest available unit or instrumentation task.
The repository currently contains only template test files, so do not imply
meaningful business coverage from them.

### Report verification honestly

When summarizing work:

* state which commands were run,
* state whether they passed,
* say explicitly when verification was not run or could not be completed.

Do not imply a build or test passed unless it actually did.

---

## Gradle and dependency rules

* Prefer `gradle/libs.versions.toml` for dependency and plugin version changes.
* Follow the existing version catalog naming style.
* Do not scatter raw dependency coordinates or versions across module build
  files without a strong reason.
* Respect centralized repository setup and
  `RepositoriesMode.FAIL_ON_PROJECT_REPOS`.
* The GitHub Packages `miuix` repository is intentionally configured in
  `settings.gradle.kts`; do not duplicate repositories in subprojects.
* Keep the explicit `navigationevent-compose` exclusion unless the dependency
  issue it works around has been understood and resolved.
* Room plugin/schema configuration currently exists even though Room runtime and
  compiler dependencies are commented out. Do not enable, remove, or repurpose
  this setup without checking storage, codegen, DI, and build impact.
* Do not enable commented-out dependencies such as Room runtime/compiler or
  app-process helpers without checking the full storage, codegen, DI, and build
  impact.

---

## Architecture conventions

### Dependency injection

Use the existing Koin structure in `app/src/main/java/com/merak/di/`.

When introducing a new injectable dependency:

* place it in the most relevant existing module,
* avoid ad-hoc global singletons,
* keep initialization wiring explicit,
* update `di/init/` wiring when process-specific module loading is affected.

### Application initialization

`ThemeStoreApplication.kt` performs global startup work. Treat initialization
order as sensitive, especially where it touches logging, hidden API access,
Shizuku, settings, or DI.

Do not reorder or remove startup logic as a cleanup unless the task requires it
and the consequences are understood.

### Data and settings

Settings are currently stored through `data/settings/` and DataStore.

When adding or changing a persisted setting, verify whether the change needs:

1. a settings model update,
2. DataStore key/default handling,
3. repository contract or implementation changes,
4. DI wiring updates,
5. UI state/view-model/action updates,
6. string/resource updates.

Do not implement only the visible switch while leaving persistence, mapping, or
downstream behavior inconsistent.

For state that affects services or privileged flows, verify the service
lifecycle and process module wiring as well as the settings screen.

---

## UI conventions

The app uses Jetpack Compose with Material 3, Miuix, Haze, and MaterialKolor.

When changing UI:

* follow nearby Compose and Miuix patterns before inventing a new style,
* keep reusable components narrow and caller-driven,
* avoid hardcoding screen-specific behavior into generic components,
* preserve dark/light and localized resource behavior,
* check both phone layout behavior and permission/error states when the screen
  touches storage, Shizuku, accessibility, or network flows.

For user-visible strings, update all maintained locales that contain the same
string. Current resource directories include:

* `values/`
* `values-zh-rCN/`
* `values-zh-rTW/`

`values-night/` exists for theme resources; do not treat it as a string locale.

Preserve established product terminology unless the task is explicitly a wording
cleanup.

---

## Privileged and theme-install behavior

This repository interacts with sensitive Android behavior, including:

* theme installation and theme mixing,
* Android/data access workarounds,
* Shizuku user services,
* hidden APIs and reflection,
* accessibility service automation,
* broadcast receivers,
* foreground keep-alive notifications,
* quick settings tile service state,
* network download/installation flows.

Before changing these areas:

1. identify the exact flow being modified,
2. check whether the capability requires Shizuku, accessibility, storage, or
   notification permissions,
3. preserve explicit permission checks and failure states,
4. avoid widening behavior from one privileged path to another without evidence,
5. keep ROM/version assumptions local and readable.

If a change affects data safety, theme install success, background execution, or
system compatibility, explain the tradeoff clearly in the final handoff.

---

## Source and API discipline

* Prefer native Android APIs and the repository's existing abstractions over
  ad-hoc shell-command workflows.
* Reuse existing platform wrappers, providers, and repositories before adding
  parallel paths.
* Avoid introducing reflection, hidden API access, or privileged shortcuts where
  an existing maintained path already exists.
* Keep version checks, permission checks, and backend checks close to the code
  they protect.
* Do not add broad exception swallowing around privileged operations. Surface
  actionable failure states through existing logging or UI patterns.

---

## Recommended agent workflow

For implementation tasks, follow this order:

1. Restate the concrete behavior being changed.
2. Locate the smallest relevant area of the repository.
3. Find the nearest existing pattern and extend it.
4. Update all affected layers, not only the most visible file.
5. Run the narrowest meaningful verification, defaulting to `assembleDebug`
   when compilation impact is plausible.
6. Summarize what changed, what was verified, and what remains unverified.

Prefer targeted, reviewable edits over sweeping refactors.

---

## Common mistakes to avoid

* Broadening Android version or ROM support without explicit maintainer intent.
* Replacing the zero-width-space file access workaround with a shell or root
  shortcut.
* Adding a setting toggle without persistence or state propagation.
* Adding repositories to module Gradle files despite centralized repository
  management.
* Hardcoding dependency versions outside the version catalog.
* Modifying signing/keystore behavior during unrelated work.
* Reordering application initialization as a cleanup.
* Weakening permission, accessibility, storage, or compatibility warnings
  without understanding why they exist.
* Claiming a build passed when no verification was run.

---

## Maintainer-facing handoff format

When finishing a task, give a compact handoff that includes:

* **Changed:** files or areas updated.
* **Behavior:** what users or maintainers should expect now.
* **Verification:** commands run and result.
* **Notes:** migration concerns, unverified scenarios, or follow-up risks only
  when genuinely relevant.

Keep the report factual and specific.
