# Brand typefaces

MapMe is set in two SIL OFL 1.1 variable typefaces:

| File               | Family        | Role                                     |
| ------------------ | ------------- | ---------------------------------------- |
| `SpaceGrotesk.ttf` | Space Grotesk | Titles, the wordmark, and every number   |
| `Inter.ttf`        | Inter         | Body copy, labels, anything read at length |

They are **not committed to the repository**. `./gradlew fetchBrandFonts`
downloads them here (CI runs it before every build), together with each
family's `OFL.txt`.

## If they are missing

Nothing breaks. `BrandFonts.resolve()` checks this folder at runtime and falls
back to the platform grotesque, keeping the whole MapMe type system — the size
ramp, the tracking, the weights, the tabular figures. The app looks less like
itself and no less finished. `MapMeTheme.fonts.isBranded` reports which state
you are in, and the design system gallery says so on screen.

## Bundling them instead

For fully offline, reproducible builds: run the fetch task once, then delete
the two `core/design/src/main/assets/fonts/` lines from `.gitignore` and commit
the `.ttf` files **together with their `OFL-*.txt`** — the licence requires the
copyright notice to travel with the font.
