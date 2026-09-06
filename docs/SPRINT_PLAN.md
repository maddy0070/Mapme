# Sprint plan

## Sprint 1 — Visual identity and onboarding (this one)

**Goal:** replace the rejected Sprint 0 look with a visual DNA every future
MapMe screen can follow. No product functionality.

### Delivered

- **Colour rebuilt from first principles.** Plum-warm charcoal at night, rose
  paper by day. Rose is you, Indigo is the world, Citrus is discovery. The
  trail is a luminance ramp through one hue, so it can never read as a rainbow
  or pass through purple.
- **Light mode designed, not inverted** — different accent, different button
  treatment, different depth scale, a trail that stops glowing. A test fails if
  the two modes ever collapse into each other.
- **A new mark**: a journey that loops almost all the way back with a dot that
  has not closed it. Generated geometry shared by the launcher icon, the splash
  and the in-app mark.
- **True superellipse corners** everywhere, sampled evenly along the arc. No
  pills anywhere in the product.
- **Bricolage Grotesque + Plus Jakarta Sans**, chosen to match the voice.
- **Onboarding**: three beats of one continuous drawing, with the camera driven
  by the pager's fractional offset and zoom interpolated in log space.
- **Home**: the real first screen in its real first state — empty, and honest.
- Appearance control (Auto/Light/Dark) persisted across launches.
- Tests for the squircle maths and the artwork's determinism.

### Explicitly not delivered

No GPS, no recording, no map, no storage, no history, no replay. All out of
scope by instruction, and asking for location before there is a journey to
record would break §16 anyway.

### Known gaps carried forward

1. **Nothing has been seen on a phone yet.** CI proves the code is well-formed;
   it says nothing about whether MapMe *looks* like MapMe.
2. **Backdrop blur still re-composes per pane.** Fine over the procedural
   ground, must become a captured `GraphicsLayer` before it sits over a map.
3. **The design system gallery is gone** with the old foundation screen. There
   is currently no in-app way to view every token at once; if that turns out to
   be missed during QA it should come back as a debug-only screen, not a
   product one.
4. **Three icons only.** Deliberate, but the set will need to grow carefully.

---

## Sprint 0 — Foundation

**Goal:** establish the project and the visual system MapMe will be built in,
without building any product feature.

### Delivered

- **Project.** Gradle 8.14.3 / AGP 8.7.3 / Kotlin 2.0.21, three modules, pinned
  dependency catalogue, CI that tests, assembles a debug APK and lints.
- **Design system** (`:core:design`): semantic colour with enforced contrast,
  the two-typeface ramp with graceful fallback, space/radius/depth/blur/motion
  tokens, the glass material with real backdrop refraction, a custom icon set,
  a semantic haptic vocabulary, and a reduced-motion contract.
- **Brand.** The MapMe mark — an M drawn as a journey by the same spline that
  draws real trails — as the in-app mark, the adaptive launcher icon (with a
  themed variant) and the cold-start splash.
- **`TrailLine`.** The journey line: Catmull-Rom smoothed, time-coloured,
  glowing, revealable along its own length.
- **Domain vocabulary** (`:core:model`): `GeoPoint`, `TrackPoint`, `Journey`,
  with haversine geometry and tests.
- **Two screens.** A foundation screen that is honest about what this build is,
  and the design system gallery — a workshop tool that ships in the app so the
  system can be judged on real hardware.

### Verified

CI is green on the branch head: unit tests pass (the palette contrast checks
and the haversine geometry among them), the debug APK assembles, and Android
Lint reports no errors. The APK artifact on that run is the build to install.

Two real defects were found by that first compile and fixed rather than worked
around: `:core:design` was calling `Vibrator.vibrate` without declaring
`VIBRATE` in its own manifest — it only worked because the app happened to
declare it — and the font-fetch task had no network timeout, so it could have
hung a build indefinitely instead of falling back.

**Not yet verified: any of it on a phone.** Everything above is a machine
saying the code is well-formed. Whether MapMe *looks* like MapMe is a question
only the device can answer — see `QA_CHECKLIST.md`.

### Explicitly not delivered

No location permission, no recording, no map, no storage, no history, no
replay, no onboarding. All of that is a future sprint's, and asking for
location before there is a journey to record would break §16.

### Known gaps carried forward

1. **The brand fonts are not committed.** They are fetched at build time. On a
   network-restricted machine the app runs on the platform grotesque, which the
   gallery says on screen. Bundle them (see the fonts README) if reproducible
   offline builds matter more than a binary-free repo.
2. **Backdrop blur re-composes per pane.** Correct and cheap over the
   procedural aurora; must become a captured `GraphicsLayer` before it sits
   over a live map.
3. **The foundation screen is temporary.** It is replaced wholesale by the home
   experience.
4. **Light mode is unproven on device.** Defined, contrast-tested, never looked
   at in sunlight.

---

## Sprint 1 — ready to start

The foundation is arranged so Sprint 1 can be the first real feature without
touching anything above. Whatever it turns out to be, these are already in
place: theme, navigation graph with spatial transitions, glass, trail
rendering, haptics, the domain types, and a CI pipeline that hands you an
installable APK.

If Sprint 1 is journey recording, the shape it slots into is:

1. `:core:location` — sampling strategy and the foreground service. §15 lists
   the failure modes that must be handled; none of them are optional.
2. `:core:database` — Room, local only. §16.
3. `:core:data` — the journey repository, the single place features ask.
4. `feature/home` — the map, the live trail, floating glass information.

And before any of it, the permission moment: a screen that explains *why*, in
MapMe's voice, with no manipulation (§16). That screen is a design problem
before it is an engineering one.

The map engine decision (`decisions/0001-map-technology.md`) should be settled
first if Sprint 1 touches the map, since it changes what the home screen is
built on.

---

## How every sprint ends

1. Self-review against constitution §26 — the full list, honestly.
2. CI green: unit tests, assemble, lint.
3. Install the CI artifact on the physical phone.
4. Work the device checklist in `QA_CHECKLIST.md`. Ten meaningful passes over
   the sprint's major flows (§27).
5. Fix serious bugs before the next sprint starts. Not after.
