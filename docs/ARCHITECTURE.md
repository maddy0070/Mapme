# Architecture

## What exists

```
MapMe
├── app            Android application. Activity, navigation, screens.
├── core/design    The MapMe design system. Tokens, components, brand.
├── core/location  Where you are. Permission model, platform provider.
├── core/map       The basemap. Engine wrapper, generated style, camera.
└── core/model     Domain vocabulary. Pure Kotlin, no Android.
```

Five modules, and each one earns its boundary:

- **`:core:design`** is the product's identity. Every future feature depends on
  it, and it must be buildable and previewable without the app. Keeping it
  separate is what stops a "quick" hex value appearing in a screen.
- **`:core:model`** is a plain Kotlin/JVM module. The domain has no business
  knowing about Android, and the GPS-cleaning logic that will live here needs
  tests that run in milliseconds, not on a device.
- **`:core:map`** exists so the map engine can be replaced. Its public surface
  is `GeoPoint`, `MapPosition`, `MapCameraState` and `MapLoadState`; MapLibre
  is imported in exactly one file, and `EngineContainmentTest` fails the build
  if that stops being true. See [ADR 0002](decisions/0002-map-engine.md).
- **`:core:location`** is separate from `:core:map` because recording will want
  location without a map on screen, and because the permission state machine is
  worth testing on the JVM rather than through a screen.
- **`:app`** is thin on purpose. It wires things together and owns navigation.

There is deliberately **no** `:core:data`, `:core:location`, or
`:core:database` yet. Empty modules waiting for code are speculation, and the
constitution (§14) says not to abstract for its own sake. They get created in
the sprint that fills them.

### Where things will go

When journey recording lands, the expected shape is:

```
core/data        Journey repository. The one place features ask for journeys.
core/database    Room. Local, encrypted-at-rest if practical. Never leaves the phone.
feature/home     The map + live trail + glass information.
feature/history  Day / week / month / year.
feature/replay   Cinematic playback.
```

That is a sketch to keep decisions consistent, not a promise. A sprint creates
the modules it needs.

## Principles

**Local-first, always.** Journey history never leaves the device unless the
person explicitly asks it to. This is not a default to revisit later; it is the
one promise MapMe makes (constitution §16). `allowBackup` is already `false` in
the manifest for exactly this reason.

**Understandable over clever.** Prefer the obvious implementation. When
something must be subtle — the backdrop-refraction trick, the Catmull-Rom
spline — the comment explains *why*, and states the cost.

**One-way dependencies.** `app → core:design → core:model`. Nothing in `core`
knows about a feature. Nothing in the design system knows what a Journey is; it
draws points.

**No paid services.** No AI APIs, no analytics, no crash SDK, no paid map API
without an ADR that states the cost (constitution §§17–18).

## Build

- Kotlin `2.2.21`, AGP `8.7.3`, Gradle wrapper `8.14.3`, JDK 17. The Kotlin
  version is set by the map engine, not chosen for itself — MapLibre 13.2.0+ is
  built with Kotlin 2.2 and a 2.0.x project fails its metadata check. ADR 0002.
- `compileSdk`/`targetSdk` 35, `minSdk` **26** (~99% of active devices).
  Everything that needs a newer API degrades on purpose rather than being gated
  off — see `MapMeGlass` for the pattern.
- Compose BOM `2024.12.01`. Dependencies are pinned; no dynamic versions.
- `:core:design` depends on Compose **foundation**, not `material3`. See the
  design system doc for why.

### Verification

There is no Android SDK in the authoring environment, so **CI is the compiler**.
`.github/workflows/android.yml` runs unit tests, assembles a debug APK and
uploads it as an artifact, then runs Android Lint. The APK on that workflow run
is the build to install on a phone — device testing is the only testing that
counts (constitution §27).

## Open decisions

Recorded so a future sprint does not have to rediscover them:

- **Map engine.** Not chosen. See `decisions/0001-map-technology.md`.
- **Predictive back.** `enableOnBackInvokedCallback` is off until there is
  navigation worth testing it against.
- **Configuration changes.** The activity is left to recreate normally so that
  state restoration is genuinely exercised. Revisit when a map camera is worth
  holding across rotation.
- **Backdrop blur at scale.** The per-pane re-composition in `GlassBackdrop`
  must become a captured `GraphicsLayer` before it sits over a live map.
