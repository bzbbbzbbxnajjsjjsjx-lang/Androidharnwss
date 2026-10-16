---
name: material3-expressive
description: Material 3 Expressive guidance for this app (Compose material3 1.4.0-alpha) — wavy progress, loading indicators, shape morphing, expressive motion, button and navigation components, and the hand-drawn WavyRing. Use when adding or changing visual components, shapes, motion or the Summary screen ring.
---

# Material 3 Expressive in Vardiya

The app pins `androidx.compose.material3:material3` to the `1.4.0-alpha` line
(`gradle/libs.versions.toml`) because the Expressive components live there. Both
`ExperimentalMaterial3Api` and `ExperimentalMaterial3ExpressiveApi` are opted in for the whole
module in `app/build.gradle.kts`; never add `@OptIn` at call sites.

## Before you write a component

1. Check whether Material already ships it (list below). Prefer the library component over a
   custom one; it gets theming, accessibility semantics and motion for free.
2. If the library version doesn't fit (as with the ring, see below), draw it yourself but take the
   proportions from the androidx tokens and say so in a KDoc comment.
3. Colors come from `MaterialTheme.colorScheme` (the teal scheme in `ui/theme/Theme.kt`), never
   hard-coded `Color(...)` in screens. Dynamic color stays off.
4. Alpha APIs move between releases. When bumping `material3`, read the release notes for renamed
   parameters and fix every call site in the same commit; CI is the only place the build is
   verified.

## Components available in the 1.4.0-alpha line

| Need | Component |
| --- | --- |
| Indeterminate wait (app opening) | `LoadingIndicator`, `ContainedLoadingIndicator` |
| Determinate progress, small sizes | `LinearWavyProgressIndicator`, `CircularWavyProgressIndicator` |
| Shape-morphing buttons | `Button(shapes = ButtonDefaults.shapes())`, `ToggleButton`, `IconButton` with `IconButtonDefaults.shapes()` |
| Connected actions | `ButtonGroup`, `SplitButtonLayout` |
| Floating actions | `FloatingActionButtonMenu`, `ToggleFloatingActionButton`, `HorizontalFloatingToolbar` |
| Navigation | `ShortNavigationBar` + `ShortNavigationBarItem`, `WideNavigationRail` |
| Expressive shapes | `MaterialShapes.*` (e.g. `Cookie9Sided`, `Sunny`, `Pill`) with `.toShape()` |
| Motion | `MotionScheme.expressive()`, via `MaterialTheme(motionScheme = …)` or `MaterialExpressiveTheme` |

Verify an API exists in the pinned version before using it (check the material3 release notes or
the sources jar); don't guess parameter names.

## The Summary ring (`ui/components/WavyRing.kt`)

`CircularWavyProgressIndicator` has a fixed wave height meant for ~48dp and looks flat at the
270dp hero size, so the ring is drawn on a `Canvas`. Keep it matching the Material tokens, scaled
to `strokeWidth`:

- wave amplitude = 0.4 × stroke (token: 1.6dp on a 4dp stroke)
- wavelength = 3.75 × stroke (token: 15dp), rounded to a whole number of waves around the ring
  so the wave closes seamlessly
- gap between the active wave and the track = 1 × stroke (token: 4dp), on both ends
- track is as thick as the wave, round caps on both
- the wave travels one wavelength per second (`phase` 0 → 2π over 1 000 ms, linear)
- amplitude animates 0 → 1 over 500 ms when a shift starts and back to 0 when it ends

`progress` and `amplitude` are lambdas read inside the draw scope, so per-frame updates
invalidate only the drawing, not composition. Keep it that way: don't turn them back into plain
`Float` parameters.

## Shape morphing and motion

- Pressed buttons morph from pill to a squarer corner (see `ExpressiveButton` in
  `SummaryScreen.kt`). For new buttons prefer `ButtonDefaults.shapes()`; if you animate a shape by
  hand, use a spring (`Spring.DampingRatioMediumBouncy`, `Spring.StiffnessMedium`) to match.
- Expressive motion uses springs for spatial changes (size, position, shape) and short tweens for
  effects (color, alpha). Don't use long linear tweens except for continuous loops like the wave.
- Pair emphasized actions with haptics the way the Summary screen does
  (`HapticFeedbackType.Confirm` on start, `LongPress` on finish, `ContextClick` for ticks).

## Accessibility checks for expressive components

- Decorative shapes, waves and loading indicators need no text; the information they show must
  also be in readable text (the ring's percentage is in the centre label).
- Morphing or bouncing must not move a touch target out from under the finger; keep the layout
  size fixed and animate only the drawn shape.
- Touch targets stay at least 48dp; the start/finish buttons are 64dp tall.
