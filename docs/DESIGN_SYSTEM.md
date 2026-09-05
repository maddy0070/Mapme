# The MapMe design system

Everything here lives in `:core:design`. Nothing in a feature module should
contain a hex value, a raw `dp` for spacing, a font size, or a duration in
milliseconds. If a screen needs something the system does not have, the system
gets it — that is the only way two screens built months apart still look like
the same product.

Read tokens through `MapMeTheme`:

```kotlin
MapMeTheme.colors.accent
MapMeTheme.type.metric
MapMeTheme.space.cardPadding
MapMeTheme.radius.card
MapMeTheme.depth.floating
MapMeTheme.motion.cinematic
```

---

## 1. Colour

**Source:** `MapMePalette.kt` (pigments) → `MapMeColors.kt` (meanings).

### The idea

MapMe is looked at in the dark, over a map, for a long time. So the ground is a
cold ink with a slight cyan cast — the colour of the world seen from altitude at
night — rather than the neutral grey or navy-violet most dark themes settle on.
It recedes so the journey can come forward.

Four signal hues sit on it, each with exactly one job:

| Token       | Hue              | Means                                              |
| ----------- | ---------------- | -------------------------------------------------- |
| `accent`    | Aurora `#2BF5C0` | The hero. Primary actions, positive results, the freshest part of a trail. |
| `focus`     | Beacon `#4D7CFF` | Selection, focus, discovery — and the far end of a trail. |
| `live`      | Pulse `#FF2E93`  | *Now.* The live head, recording state, the dot that is you. |
| `milestone` | Ember `#FFB627`  | Celebration. Records, firsts, streaks. Rare on purpose. |
| `critical`  | `#FF4B4B`        | Destructive actions only. Never ordinary warnings. |

### The trail gradient

`trailFar` → `trailNear` is Beacon → Aurora: a **60° hue sweep**, deliberately
short. That is what keeps the line elegant at every zoom instead of turning into
a rainbow. The gradient carries time, so the line tells you which way the day
ran without a single arrowhead.

`trailHead` (Pulse) is 107° away from Beacon and 167° from Aurora — far enough
that *now* always reads, whatever part of the line it sits on. Note that it is
separated by **hue, not luminance**: magenta and azure have nearly identical
brightness, so a contrast-ratio check would wrongly bless an invisible design.
`ColorContrastTest` measures hue distance for exactly this reason.

### Dark and light

Dark is the real face — designed first, tuned longest. Light exists so MapMe
survives direct sunlight and is never the reference. Light reuses the same four
hues, taken down until text on them is readable (Aurora at `#2BF5C0` is far too
light for paper; light mode uses `#008E73`).

### Accessibility is enforced, not asserted

`ColorContrastTest` fails the build if any text token drops below **4.5:1**
against any surface it can appear on, or any signal colour below **3:1**. It
found three real failures when the palette was first written. Change a colour,
re-run the test.

### Map tokens

`mapLand`, `mapWater`, `mapRoadMinor`, `mapRoadMajor`, `mapBuilding`,
`mapLabel`, `mapLabelHalo` exist now so the basemap style is authored against
the same system as the interface. The map and the chrome can never drift apart.

---

## 2. Typography

**Source:** `MapMeType.kt`, `BrandFonts.kt`.

Two typefaces, both SIL OFL 1.1:

- **Space Grotesk** — titles, the wordmark, and every number. Its slightly
  mechanical letterforms are the personality; its figures have the weight a
  distance deserves.
- **Inter** — everything read at length. The most legible interface face there
  is, and it knows to get out of the way.

Two rules hold the ramp together:

1. **Display tracking tightens as size grows.** Large type at default tracking
   looks typed rather than set. `heroMetric` runs at −0.03em.
2. **Numbers are their own size class.** `heroMetric` (64sp), `metric` (28sp)
   and `metricLabel` are tabular, tight, and heavy — meant to be *looked at*,
   not read.

`label` and `labelSmall` are the only uppercase styles. Eyebrows and units
only — never buttons, never sentences.

### The fonts are not committed

`./gradlew fetchBrandFonts` pulls them into `core/design/src/main/assets/fonts/`;
CI runs it before every build. If they are absent, `BrandFonts.resolve()` falls
back to the platform grotesque and keeps the entire ramp — sizes, tracking,
weights, tabular figures. `MapMeTheme.fonts.isBranded` reports which state you
are in, and the in-app kit says so on screen. See that folder's README to bundle
them permanently.

---

## 3. Space, radius

**Source:** `MapMeSpacing.kt`.

A 4dp grid (`x1`…`x16`) with names for the distances that carry meaning:
`screenEdge` (20), `cardPadding` (20), `sectionGap` (32), `itemGap` (12),
`labelGap` (6), `minTouchTarget` (48), `controlInset` (16).

Use the semantic names in screens. Reach for raw steps only inside the design
system itself.

Radii are generous — the interface is glass panes over a map, and glass panes
have polished edges. The larger the surface, the larger the radius, so nothing
looks like a scaled-up version of something smaller: `control` 16, `card` 22,
`panel` 28, `sheet` 36 (top corners only). **Every button is a pill.** There are
no rectangular buttons in MapMe.

---

## 4. Depth and glass

**Source:** `MapMeDepth.kt`, `MapMeGlass.kt`, `GlassBackdrop.kt`.

### Depth

Four rungs: `resting`, `raised`, `floating`, `overlay` (plus `flat`). In a dark
interface a drop shadow does almost nothing — black on near-black is invisible —
so depth is carried by three cues at once: the surface gets lighter as it rises,
the top edge picks up more sheen, and the shadow spreads. Elevation is the least
important of the three.

### The glass material

A pane is four things stacked in a believable order:

1. the world behind it, thrown out of focus;
2. a veil that buys back contrast;
3. a tint that catches light from above;
4. a bright hairline along the top edge, fading towards the corners.

Three tones, chosen by how much text the pane holds, not by taste:
`Whisper` (small controls, map still readable through them), `Standard` (the
default pane), `Dense` (sheets and dialogs).

### Real refraction

`MapMeBackground` is a `GlassBackdropHost`. Panes inside it re-paint the host's
backdrop, translated so the copy lines up exactly with the real thing, and blur
that. Move a pane and the world behind it moves correctly.

**The cost, stated plainly:** the backdrop is re-composed once per pane. That is
the right trade for a procedural ground (MapMe's aurora is two gradients and a
fill) and the wrong one for a live map with tiles and labels. When the map
arrives, the host should capture itself into a `GraphicsLayer` once per frame
and panes should draw that layer. The API does not change — only what sits
behind `GlassBackdrop.content`.

### Below Android 12

`Modifier.blur` is a no-op before API 31. Rather than shipping a see-through
pane nobody can read, the veil thickens by 0.24 and the glass becomes frostier.
It still looks like glass; it just stops being a window.

---

## 5. Motion

**Source:** `MapMeMotion.kt`, `MotionPreference.kt`.

Durations are named after intent, because the right question at a call site is
"is this a state flip or a journey unfolding?", not "is this 200 or 300":

| Token       | ms   | For                                                |
| ----------- | ---- | -------------------------------------------------- |
| `instant`   | 90   | A control acknowledging a finger                   |
| `quick`     | 140  | Press feedback                                     |
| `brisk`     | 200  | State change on something already on screen        |
| `smooth`    | 300  | Arriving or leaving                                |
| `flowing`   | 450  | A panel expanding, a sheet, a camera nudge         |
| `cinematic` | 700  | The map travelling, a day becoming another day     |
| `epic`      | 1400 | The trail drawing itself. The moment we are selling. |

Easings: `standard` (the default — leaves quickly, arrives gently),
`entering`, `exiting`, `gentle` (ambient loops), `linear` (progress and replay
only). Springs: `snappy()`, `physical()` (the default), `settling()`.

### Reduced motion is a contract

When the system animator scale is 0, `LocalReduceMotion` is true and:
**nothing disappears.** Transitions collapse to their end state, ambient loops
are never started, and the trail is drawn complete instead of drawing itself.
Same information, no theatre. Wrap durations in `motionDuration(...)`.

---

## 6. Haptics

**Source:** `MapMeHaptics.kt`. Five feelings, each with a meaning: `select()`,
`confirm()`, `tick()`, `milestone()`, `warn()`.

`milestone()` is the only shaped waveform — a two-beat crescendo — because it is
the only haptic MapMe is allowed to make memorable. Everything else is a system
constant. A phone that buzzes at everything gets silenced, and then the
milestone is lost with it.

---

## 7. Icons

**Source:** `MapMeIcons.kt`. MapMe draws its own, not because the standard set
is bad but because an icon set is a handwriting: **24dp grid, 2dp stroke, round
caps, round joins, no detail smaller than the stroke.** Mixing two handwritings
is the fastest way to make a product look assembled.

Current set: `Pin`, `Trail`, `Calendar`, `Play`, `Layers`, `Sparkle`,
`ChevronRight`. Add one when a screen genuinely needs it, drawn to these rules.

---

## 8. Components

| Component                        | Notes                                              |
| -------------------------------- | -------------------------------------------------- |
| `MapMeText`                      | Colour resolves: argument → style → surrounding `LocalMapMeContentColor` → `textPrimary`. |
| `MapMeButton`                    | Primary / Secondary / Ghost, Large / Medium. Compresses and springs back with a haptic. **No ripples** — a ripple is ink spreading through paper, the wrong metaphor for glass. |
| `GlassSurface` / `GlassCard`     | The pane. `GlassCard` adds standard interior padding. |
| `MapMeBackground`                | Ink ground with two drifting aurora fields; also the backdrop host. |
| `TrailLine`                      | The journey. Catmull-Rom smoothed, time-coloured, glowing, revealable. |
| `MapMeMark` / `MapMeWordmark`    | The brand. An M drawn as a journey by the same spline. |
| `Metric`                         | A statistic as something to feel. Reads to screen readers as one sentence. |
| `MapMePill` / `LiveDot`          | State, worn on the interface. Pills say what *is*, never what to do. |

### There is no Material

MapMe depends on Compose **foundation**, not `material3`. A product whose whole
promise is "this doesn't look like anything else" cannot inherit its buttons
from a spec shipping on a billion devices. The cost is that MapMe owns its
primitives; the benefit is that nothing can quietly drift back to default.

---

## 9. Seeing it

The design system gallery ships inside the app — `FoundationScreen` → *"See
what it's made of"*. Previews lie about colour, glass, motion and haptics; a
phone in real light does not. A design system nobody can look at drifts.
