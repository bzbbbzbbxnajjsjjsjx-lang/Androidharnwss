# Vardiya

Single-module Android app (`:app`, package `com.vardiya.app`) for tracking shifts and earnings
under Turkish labour law. Jetpack Compose + Material 3 Expressive, no DI framework, no navigation
library.

## Commands

```bash
./gradlew testDebugUnitTest      # JVM unit tests (domain logic)
./gradlew detekt                 # static analysis, incl. Compose rules
./gradlew lintDebug              # Android lint
./gradlew assembleDebug assembleRelease
```

CI (`.github/workflows/android.yml`) runs detekt, lint, unit tests and both APK builds on every
push. Cloud sessions usually cannot reach Google's Maven repo, so Gradle cannot configure the
project there: push and read the GitHub Actions result instead of claiming a local build passed.

## Layout

- `domain/` — pure Kotlin, unit-tested. `Models.kt` (`Shift`, `PaySettings`, `ShiftTemplate`),
  `EarningsCalculator.kt` (month summary and pay rules), `TurkishHolidays.kt` (public and
  religious holidays, half-day eves from 13:00).
- `data/ShiftRepository.kt` — shifts, settings and the running shift's start time as JSON in
  Preferences DataStore.
- `ui/VardiyaViewModel.kt` — single `AndroidViewModel`; exposes `StateFlow`s.
- `ui/VardiyaApp.kt` — scaffold, bottom bar, tab switching.
- `ui/screens/` — Summary (wavy ring, start/finish), Calendar, Shifts, Settings, shift editor sheet.
- `ui/components/` — `WavyRing` (hand-drawn M3 Expressive circular wavy indicator),
  `MonthSwitcher`, `Format.kt` (Turkish number/money/date formatting).
- `ui/theme/Theme.kt` — teal brand scheme; dynamic color is off on purpose.

## Domain rules

- Hourly rate = monthly salary ÷ monthly hours (default 28.000 ₺ ÷ 225 h).
- Holiday minutes ×2 (İş Kanunu md. 47); eves and 28 Ekim count from 13:00.
- Minutes beyond the monthly hours are overtime ×1,5 (md. 41).
- Breaks are unpaid. Times are minutes from midnight; `endMinute <= startMinute` means the shift
  ends the next day.
- Any change here needs a test in `app/src/test/.../EarningsCalculatorTest.kt`.

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
- Material 3 is pinned to the `1.4.0-alpha` line for Expressive APIs; the opt-ins are set
  globally in `app/build.gradle.kts`, don't add `@OptIn` per call site.
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
API with KDoc, design tokens, official samples, release notes, and the upgrade path from the
pinned 1.4.0-alpha14). Load the matching skill before working in that area.
