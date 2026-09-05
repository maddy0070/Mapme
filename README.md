# MapMe

> Your life, drawn as a line.

Google Maps answers *"where do you want to go?"*
MapMe answers *"where have you been?"*

MapMe is a premium personal journey journal for Android. It quietly records
where you go and turns that movement into a visual history of your life. Not a
navigation app, not a fitness tracker, not a GPS logger — a personal map of
someone's life.

**Status: foundation.** The project, the design system and the brand exist.
Journey recording does not yet. See [`docs/SPRINT_PLAN.md`](docs/SPRINT_PLAN.md).

---

## Documentation

| Document | What it is |
| --- | --- |
| [Product constitution](docs/PRODUCT_CONSTITUTION.md) | The source of truth. Read before any sprint. |
| [Design system](docs/DESIGN_SYSTEM.md) | Colour, type, glass, motion, haptics, icons — and why each is what it is. |
| [Architecture](docs/ARCHITECTURE.md) | Modules, principles, build setup, open decisions. |
| [Sprint plan](docs/SPRINT_PLAN.md) | What each sprint delivered, and what is next. |
| [Device QA checklist](docs/QA_CHECKLIST.md) | The phone is the source of truth. |
| [ADR 0001 — Map technology](docs/decisions/0001-map-technology.md) | Deliberately undecided, with the cost of each option written down. |

## Build

Requires JDK 17 and an Android SDK with API 35.

```bash
./gradlew test            # unit tests, including the palette contrast checks
./gradlew assembleDebug   # installable APK -> app/build/outputs/apk/debug/
./gradlew fetchBrandFonts # pull the two OFL brand typefaces into assets
./gradlew lint
```

CI runs all of the above on every push and uploads the debug APK as an
artifact. **That artifact is the build to install on a phone** — device testing
is the only testing that counts.

## Layout

```
app             Activity, navigation, screens
core/design     The MapMe design system
core/model      Domain vocabulary — pure Kotlin, no Android
docs            Constitution, design system, architecture, decisions
```

## What is in the app today

- **Foundation screen** — the ink ground, the aurora, and the trail drawing
  itself. Honest about what this build is.
- **The kit** — every colour, type style, glass tone, button, number, icon and
  haptic, on the device they have to work on. Reached from the foundation
  screen.

## Privacy

Your journey belongs to you. Location history is intended to stay on the
device: local-first storage, `allowBackup` off, no analytics, no crash
reporting, no AI services. The only permission requested so far is `VIBRATE`.
Location permissions arrive with the sprint that records a journey, alongside
the screen that explains why.

## Licences

The brand typefaces — [Inter](https://github.com/rsms/inter) and
[Space Grotesk](https://github.com/floriankarsten/space-grotesk) — are SIL Open
Font License 1.1. They are fetched at build time rather than committed; see
`core/design/src/main/assets/fonts/README.md`.
