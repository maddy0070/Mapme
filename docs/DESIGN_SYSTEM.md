# The MapMe design system

Everything here lives in `:core:design`. Nothing in a feature module should
contain a hex value, a raw `dp` for spacing, a font size, or a duration in
milliseconds. If a screen needs something the system does not have, the system
gets it — that is the only way two screens built months apart still look like
the same product.

Read tokens through `MapMeTheme`:

```kotlin
MapMeTheme.colors.accent
MapMeTheme.type.displayHero
MapMeTheme.space.cardPadding
MapMeTheme.radius.card
MapMeTheme.depth.floating
MapMeTheme.motion.travel
```

---

## 1. Colour

**Source:** `MapMePalette.kt` (pigments) → `MapMeColors.kt` (meanings).

### The idea

MapMe is a journal of someone's life, so the palette is **warm before it is
technical**. The ground is a plum-tinted charcoal at night and a rose-tinted
paper by day — never neutral grey, never cold cyan, because a record of where
you have been should feel closer to a photo album than an instrument panel.

Three signal hues, each with exactly one job:

| Token       | Night              | Paper     | Means |
| ----------- | ------------------ | --------- | ----- |
| `accent`    | Rose `#FF2D6F`     | `#D6004F` | **You.** The journey, primary actions, anything alive. |
| `focus`     | Indigo `#4F6BFF`   | `#3040D6` | **The world.** Structure, focus, selection. |
| `discovery` | Citrus `#B8F02D`   | `#5E7A00` | **Discovery.** A new place, a first, a record. Rare. |
| `critical`  | `#FF5A4E`          | `#D62B1F` | Destructive actions only. |

### The trail

`trailPast → trailFar → trailNear → trailHead` is a **luminance ramp through a
single hue**, not a hue gradient. That is deliberate: a blue-to-pink gradient
passes through purple at its midpoint, and generic purple is the one look this
product must never have. Monochrome also means the line can never read as a
rainbow at any zoom.

`ColorContrastTest` enforces that the ramp is monotonic — brightening towards
now on night, darkening towards now on paper. `trailPast` is deliberately low
contrast: it is the faded tail of a journey, decorative rather than
informational, and is excluded from the text thresholds.

### Two modes, two designs

Light is **not** dark inverted, and a test fails if it ever becomes so:

|              | Night                              | Paper |
| ------------ | ---------------------------------- | ----- |
| Ground       | Plum-warm charcoal                 | Warm white with a rose whisper |
| Cards        | Lighter surfaces carry depth       | Pure white; shadow carries depth |
| Shadows      | Barely used, near-black            | Load-bearing, plum-tinted — a neutral shadow on warm paper reads as dirt |
| Primary button | Hot rose fill, **ink** label     | Deep rose fill, **white** label |
| Trail        | Light. Blooms.                     | Pigment. No bloom — a glow on white looks like a printing fault |
| Glass        | Pale veil catching light           | Genuine white frost, much denser |

The button labels differ because they have to: white on hot rose reaches only
3.6:1. Night therefore puts ink on rose, which is also the more confident look.

### Accessibility is enforced, not asserted

`ColorContrastTest` fails the build if any text token drops below **4.5:1**
against any surface it can land on, or any signal below **3:1**. It caught five
real failures while this palette was being designed.

---

## 2. Geometry — the squircle

**Source:** `MapMeShape.kt`.

Android's `RoundedCornerShape` sweeps a **circular** arc, which meets the
straight edge at an abrupt change in curvature — the eye reads it as a
rectangle with its corners cut off. MapMe uses a **superellipse** instead:

```
|x/r|^n + |y/r|^n = 1        n = MAPME_EXPONENT = 4.2
```

At `n = 2` that is a circle, which is exactly the shape being avoided. At 4.2
the curvature eases into the edge, which is what makes the form read as
*squarical* rather than spherical.

Corners are sampled **evenly along the arc**, not evenly in the angle. A
superellipse races through its flat sections and crawls round its tightest
point; uniform angle stepping left steps 3.3× longer at one end than the other,
about half a pixel of visible flattening on a 40dp corner. Arc-length
resampling drops that to 0.06px.

**There are no pills.** The fully rounded capsule is the most common button
shape in modern apps, which is precisely why MapMe does not use one. Every
interactive surface — button, card, chip, icon frame, and the logo — is cut
from this same curve.

Radii: `chip` 12, `control` 18, `button` 24, `card` 30, `panel`/`sheet` 40.

---

## 3. The mark

**Source:** `MapMeMark.kt`, and the generated vectors in `app/src/main/res`.

A journey that loops almost all the way back, and a dot that has not closed it
yet. The loop is a life's worth of movement; the dot is you, still going.

- **map** is the enclosing form, **me** is the dot, **journey** is the taper.
  No pin, no letter, no arrow — the three things every other location product
  already owns.
- The loop is the house superellipse, so the logo is not bolted onto the design;
  it is the house geometry at its purest.
- The stroke tapers using the *same function* that tapers a real journey in
  `TrailLine`. The brand mark and the product's core visual are one object
  drawn at different lengths.
- The gap makes it read as unfinished on purpose — your map is still being
  drawn — and gives the silhouette an asymmetry that survives 16px, where a
  symmetrical ring would become an anonymous blob.

The launcher icon, the splash and the in-app mark are all generated from this
geometry rather than redrawn, so they cannot drift apart.

---

## 4. Typography

**Source:** `MapMeType.kt`, `BrandFonts.kt`.

- **Bricolage Grotesque** — headlines. A grotesque with deliberate
  irregularities, so a large sentence has a voice rather than merely a size.
- **Plus Jakarta Sans** — everything read at length. Smooth, modern, friendly
  small, content to disappear.

Both SIL OFL 1.1. The voice is short, warm and confident; a rigid geometric
face fought that, which is why neither is one.

Rules: tracking tightens as size grows (`displayHero` runs at −0.035em);
numbers are their own class, tabular and tight; uppercase is rationed to
`label` and `labelSmall`, for eyebrows and units only.

The fonts are fetched at build time, not committed. If they are absent the app
falls back to the platform grotesque and keeps the entire ramp — sizes,
tracking, weights, tabular figures. CI now *fails* if the download did not
happen, because on a networked runner a missing file means a bad URL.

---

## 5. Depth and glass

**Source:** `MapMeDepth.kt`, `MapMeGlass.kt`, `GlassBackdrop.kt`.

Shadows are **tinted**, never neutral black, and the tint comes from the mode.
Depth is built per mode because the two carry it differently: night barely uses
shadow at all (black on near-black is invisible) and leans on surface lightness
and sheen; paper leans on shadow entirely, because every surface is already
white and lightness has nothing left to say.

A pane of glass is four things in a believable order: the world behind it
thrown out of focus, a veil that buys back contrast, a tint catching light from
above, and a bright hairline along the top edge.

**Glass is rationed** — one pane per screen, plus the occasional small control.
It exists to say *this is floating above your journey*; a screen where
everything is glass has said nothing.

`MapMeBackground` is a `GlassBackdropHost`, so panes genuinely refract what is
behind them. The backdrop is re-composed once per pane: right for a procedural
ground, wrong for a live map. When the map arrives the host should capture
itself into a `GraphicsLayer` once per frame; the API does not change.

Below Android 12 `Modifier.blur` is a no-op, so the veil thickens and the glass
becomes frostier rather than pretending.

---

## 6. Motion

**Source:** `MapMeMotion.kt`, `MotionPreference.kt`.

| Token       | ms   | For |
| ----------- | ---- | --- |
| `instant`   | 90   | A control acknowledging a finger |
| `quick`     | 150  | Press feedback |
| `brisk`     | 220  | State change on something already on screen |
| `smooth`    | 320  | Arriving or leaving |
| `flowing`   | 480  | A panel expanding, a sheet, a screen change |
| `travel`    | 780  | A camera move across the same physical space |
| `epic`      | 1600 | The trail drawing itself |

`travel` exists because onboarding's page change is not a page change — it is a
camera pulling back from one continuous drawing, and a camera move that lands
in 300ms reads as a cut.

**Reduced motion is a contract.** When the system animator scale is 0, nothing
disappears: transitions resolve to their end state, ambient loops are never
started, and the trail is drawn complete instead of drawing itself. Same
information, no theatre. Wrap durations in `motionDuration(...)`.

---

## 7. Icons

**Source:** `MapMeIcons.kt`. A 24dp grid, a 2dp stroke never varied, round caps
and joins, curves that ease into their straights. No gloss, no gradients, no
3D, and never a second icon family.

The set is **three icons** — `ArrowRight`, `Replay`, `Sparkle` — because three
is what the current screens need. An icon gets drawn when a screen genuinely
cannot speak without it; a set that grows ahead of its screens is how a product
ends up with four visual dialects.

**Everything stays inside the safe area.** All ink, stroke width included, sits
within 3..21 of the 24 grid. `MapMeIconsGeometryTest` fails the build otherwise.
This rule exists because the first set was drawn to the edges of its box, so an
arrow inside a squircle button looked like it was escaping — a sizing problem
that no amount of nudging the position could have fixed.

**Measurements do not see everything.** The first `Replay` passed every
assertion — safe area, optical centring, stroke weight, minimum size — and still
read as a "C" with a wart, because its arrowhead pointed right while the curve
at that point was travelling up and to the left. Direction is not something the
test can check. Any new or redrawn glyph gets **looked at, rendered at the size
it is actually used**, before it ships; a head or terminal is derived from its
own curve's tangent rather than placed by eye.

---

## 8. Components

| Component                     | Notes |
| ----------------------------- | ----- |
| `MapMeText`                   | Colour resolves: argument → style → surrounding content colour → `textPrimary`. |
| `MapMeButton`                 | Primary / Secondary / Ghost. Squircle, never a pill. Compresses and springs back with a haptic — **no ripple**, which is ink spreading through paper, the wrong physics for glass. |
| `GlassSurface` / `GlassCard`  | The pane. Two genuinely different materials per mode. |
| `MapMeBackground`             | Ground plus drifting atmosphere; also the backdrop host. |
| `TrailLine`                   | The journey. Tapered, smoothed, revealable, mode-aware. |
| `JourneyThread`               | Deterministic generative **artwork** for onboarding. Not data — see below. |
| `MapMeMark` / `MapMeWordmark` | The brand. |
| `Metric`                      | A statistic as something to feel. Reads to screen readers as one sentence. |
| `MapMePill` / `LiveDot`       | State, worn on the interface. Pills say what *is*, never what to do. |

### There is no Material

MapMe depends on Compose **foundation**, not `material3`. A product whose whole
promise is "this doesn't look like anything else" cannot inherit its buttons
from a spec shipping on a billion devices. The cost is that MapMe owns its
primitives; the benefit is that nothing can quietly drift back to default.

---

## 9. Honesty about the artwork

The line in onboarding is generated from a fixed seed by `JourneyThread`. It is
**not** a recording, not sample data, and not a stand-in for one. It is a
deterministic drawing — identical on every launch and every device, the way a
logo is — and no screen presents it as a journey anyone took. Home shows an
empty state rather than invented statistics for the same reason.

When real journeys arrive they render through `TrailLine` exactly as this does.
That is the point: the introduction is a promise the product can keep.
