# Vardiya

Single-module Android app (`:app`, package `com.vardiya.app`) for tracking shifts and earnings
under Turkish labour law. Jetpack Compose + Material 3 Expressive, no DI framework, no navigation
library.

## Commands

```bash
./gradlew testDebugUnitTest      # JVM unit tests (domain logic) and screenshot tests
./gradlew recordRoborazziDebug   # re-record screenshots after an intended UI change
./gradlew verifyRoborazziDebug   # compare screens with app/src/test/screenshots/ (CI runs this)
./gradlew detekt                 # static analysis, incl. Compose rules
./gradlew lintDebug              # Android lint
./gradlew assembleDebug assembleRelease
```

CI (`.github/workflows/android.yml`) runs detekt, lint, unit tests and both APK builds on every
push. Cloud sessions can build locally if the Android SDK (platform 37) is installed and
`ANDROID_HOME` points at it; Maven Central rate-limits the session proxy (HTTP 429), so pass
`-I .claude/gradle/maven-central-mirror.gradle` to resolve it through Google's mirror. Still
read the GitHub Actions result before calling a change done.

## Layout

- `domain/` — pure Kotlin, unit-tested. `Models.kt` (`Shift`, `PaySettings`, `ShiftTemplate`),
  `EarningsCalculator.kt` (month summary and pay rules), `TurkishHolidays.kt` (public and
  religious holidays, half-day eves from 13:00).
- `domain/Reports.kt` — weekly and monthly totals. `ActiveShift` (in `Models.kt`) — the running
  shift with its breaks; pay stops during a break.
- `data/ShiftRepository.kt` — shifts, settings and the running shift (start time and breaks) in
  Preferences DataStore.
- `ui/VardiyaViewModel.kt` — single `AndroidViewModel`; exposes `StateFlow`s.
- `ui/VardiyaApp.kt` — scaffold, tab switching. `ui/Destination.kt` — tabs and the bottom bar.
- `ui/screens/` — Summary (wavy ring, start/break/finish), Calendar (tabs: month grid, working
  hours, reports in `ReportsTab.kt`), Shifts, Settings, shift editor sheet.
- `ui/components/` — `WavyRing` (Material's `CircularWavyProgressIndicator`, set up exactly like
  F-Droid's first-start indicator: 6dp round stroke and 24dp wavelength on a 128dp ring, scaled up
  to the 300dp ring, defaults otherwise),
  `MonthSwitcher`, `Format.kt` (Turkish number/money/date formatting).
- `ui/theme/Theme.kt` — teal brand scheme; dynamic color is off on purpose.

## Domain rules

- Hourly rate = monthly salary ÷ monthly hours (default 28.000 ₺ ÷ 225 h), or the user's own
  hourly wage when `PaySettings.useHourlyRate` is set.
- Holiday minutes ×2 (İş Kanunu md. 47); eves and 28 Ekim count from 13:00.
- Minutes beyond the monthly hours are overtime ×1,5 (md. 41).
- Breaks are unpaid. Times are minutes from midnight; `endMinute <= startMinute` means the shift
  ends the next day.
- Any change here needs a test in `app/src/test/.../EarningsCalculatorTest.kt`.

## Screenshot tests

`app/src/test/.../ui/ScreenshotTest.kt` renders every tab (Roborazzi + Robolectric, SDK 34) with
fixed data, a fixed clock (`SummaryScreen(clock = …)`) and a fixed day (`CalendarScreen(today = …)`)
into `app/src/test/screenshots/`. A UI change that alters a screen fails CI until the images are
re-recorded and committed; look at the new PNGs before committing them.

## Conventions

- Code, comments, commit messages: English. User-facing text (UI strings, `README.md`,
  `.github/release-notes.md`): Turkish. UI strings are inline in Kotlin, not in `strings.xml`.
- Format numbers, money and dates through `ui/components/Format.kt` (`TurkishLocale`,
  `formatMoney`, `formatMonth`), never with the default locale.
- Performance: the month `summary` changes at most once a minute; per-second values
  (`partialPay`, the ring's progress) are read only by the Summary screen, and fast-changing
  values are passed to drawing code as lambdas (`progress: () -> Float`) so they don't recompose.
  Keep Calendar and Shifts off per-second state.
- Accessibility: group related texts with `semantics(mergeDescendants = true)`, mark titles as
  headings, give icon-only buttons a `contentDescription`.
- Build: AGP 9 (built-in Kotlin, no `kotlin-android` plugin), Gradle 9, compileSdk 37 (material3
  1.5 and Compose 1.12 require it), targetSdk 36. Material 3 is `1.5.0-beta01`, ahead of the
  Compose BOM; the app uses `MaterialExpressiveTheme`. The Expressive opt-ins are set globally in
  `app/build.gradle.kts`, don't add `@OptIn` per call site.
- Detekt config is in `config/detekt/detekt.yml`; `config/detekt/baseline.xml` holds findings
  that predate detekt. Fix new findings rather than adding them to the baseline.

## Releasing

For a user-visible change: bump `versionCode` and `versionName` in `app/build.gradle.kts` and add
a "Yenilikler (x.y.z)" section at the top of `.github/release-notes.md` (move the previous one
down under its own version heading). Pushing a `v*` tag, or running the workflow with
`release_tag`, publishes the release APK as a GitHub release.

## Skills

`.claude/skills/` holds Compose UI, performance audit, accessibility and testing guidance, plus
`material3-expressive`: the full Material 3 Expressive reference (every material3 1.5.0-beta01
API with KDoc, design tokens, official samples, release notes, and notes on the upgrade from
1.4.0-alpha14). Load the matching skill before working in that area.
