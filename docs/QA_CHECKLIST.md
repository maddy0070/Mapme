# Device QA checklist

The phone is the source of truth (constitution §27). An emulator, a Compose
preview and a screenshot all lie about the three things MapMe is made of:
colour in real light, motion at real frame rates, and haptics.

Install the debug APK from the CI run for the commit under test:
**Actions → the run → Artifacts → `mapme-debug-apk`.**

Target **ten meaningful passes** over a sprint's major flows before calling it
stable. "Meaningful" means you were looking for something, not tapping through.

---

## Every sprint

### Install and first launch
- [ ] Fresh install over nothing. App appears with the MapMe launcher icon, not a default.
- [ ] The themed (monochrome) icon looks right on Android 13+ with a tinted launcher.
- [ ] Cold start: ink → mark → app. **No white flash at any point.**
- [ ] Reinstall over an existing copy. Still clean.

### The look
- [ ] Outdoors in daylight, screen at full brightness: is any text hard to read?
- [ ] In a dark room at minimum brightness: is anything glaring?
- [ ] Glass panes: does the ground behind them actually blur, and does it move correctly with the pane?
- [ ] On a device below Android 12: glass is frosted and legible, not transparent mush.
- [ ] Are the brand typefaces loaded? The kit screen says so if not.

### Motion
- [ ] Every animation holds frame rate. Watch the trail draw specifically.
- [ ] Navigate in and back out repeatedly. Transitions stay smooth and feel like the same space.
- [ ] Settings → Developer options → Animator duration scale **off**: nothing disappears, everything lands in its end state, no loop keeps running.

### Appearance (Auto / Light / Dark)

The control cycles Auto → Light → Dark. "Auto" resolves against the phone, so
the six directions worth checking are the three cycle steps run **twice** —
once with the phone in dark mode, once in light:

| Phone is dark | Phone is light |
| ------------- | -------------- |
| - [ ] Auto → Light — reveal, dark to light | - [ ] Auto → Light — same appearance either side |
| - [ ] Light → Dark — reveal, light to dark | - [ ] Light → Dark — reveal, light to dark |
| - [ ] Dark → Auto — same appearance either side | - [ ] Dark → Auto — reveal, dark to light |

The two "same appearance" cases still run the whole transition; they must not
flicker, stall, or leave anything behind.

For every one of them:
- [ ] The change **starts at the control you touched**, not at the screen centre or an edge.
- [ ] The boundary is soft and slightly irregular. It is not a hard circle and not a Material ripple.
- [ ] Background, text, borders, shadows, glass, the trail and the icons all change **together**. Nothing lags, nothing arrives twice.
- [ ] About seven tenths of a second, and **watchable for all of it** — the boundary should still be
      moving around 600ms, not finished at 200 with the rest spent waiting.
- [ ] The far corner does not pop. Watch the corner furthest from the control as it lands.
- [ ] Nothing jumps at the start or the end.

Then the ones that break things:
- [ ] **Tap the control repeatedly, fast.** The outgoing change finishes travelling and the next starts from the new touch point. No frozen screenshot, no half-erased screen, no stranding between two themes.
- [ ] Tap it during the onboarding trail draw. Both keep their frame rate.
- [ ] Rotate the phone mid-transition.
- [ ] Background the app mid-transition and come back.
- [ ] Animator duration scale off: the theme simply arrives, with no reveal and nothing left over.
- [ ] The dot on the control is present for Light and Dark, absent for Auto.
- [ ] Light mode read outdoors, dark mode read in a dark room. Both should feel deliberately designed, not inverted from each other.

### The map

Tiles come from a network, so do these twice: once on wifi, once on mobile data.

- [ ] First launch after onboarding lands on the map, not on a blank screen.
- [ ] The loading state is MapMe's, and tiles arrive **into** it without a flash — the
      loading ground and the map's own ground are the same colour on purpose.
- [ ] Pan. Fast pan and fling. Pinch zoom, repeatedly, in both directions.
- [ ] Labels are present and readable. If there are **no labels at all**, the font stack
      is missing — the host serves `Noto Sans Regular` and nothing else.
- [ ] Roads separate into two weights; water, parks and buildings are all distinguishable.
- [ ] Nothing on the map is vivid. It should feel like a stage waiting for something.
- [ ] Switch theme while the map is visible. The map changes with everything else.
- [ ] Rotate, background and return, kill and relaunch — all while on the map.

### Location

- [ ] Fresh install: the permission prompt does **not** appear before you reach the map.
- [ ] The card explains why, and the button opens the *system* dialog — not a MapMe copy of it.
- [ ] Grant precise. The dot appears, with a halo sized to the accuracy.
- [ ] Grant approximate only (Android 12+). MapMe accepts it and does not ask again.
- [ ] Deny once: the card explains, and offers the dialog again.
- [ ] Deny twice: the card switches to Settings, because the dialog is spent.
- [ ] Turn location services off system-wide: the map still works, and says so.
- [ ] Grant from Settings while MapMe is backgrounded, then return — the prompt is gone.
- [ ] **Pan away from yourself. The map must not drag itself back.** Then press recentre;
      it should come back and resume following.
- [ ] Recentre while already following: nothing jumps.

### Map failure

- [ ] Aeroplane mode, then open the map: a friendly explanation, and a retry that works.
- [ ] Retry after turning the network back on actually loads the map.

### Recording a journey — the walk test

**This is the only test that counts for recording.** Nothing here can be judged
from a screenshot or an emulator: the whole feature is about what a real GPS
does on a real street, and the failure modes it exists to handle only appear
outdoors.

Do the walk once properly, then read the list:

1. Open MapMe, tap **Start journey**, grant location when asked.
2. Walk for **three to five minutes**. Watch the line appear behind you.
3. **Pause.** Keep walking for a minute or two while paused.
4. **Resume.** Walk another few minutes.
5. **Finish**, confirm, and look at the saved journey.

Then check:

- [ ] The line follows **the pavement you actually walked**, not a smoothed
      curve through the buildings beside it. This is the one that matters most.
- [ ] Standing still for a minute does **not** draw a scribble where you stood.
- [ ] The stretch you walked **while paused is not drawn**, and there is no
      straight line joining where you paused to where you resumed.
- [ ] Distance does not include that gap. A five-minute pause should add roughly
      nothing to the total.
- [ ] After Finish, the journey is still on the map and reads as finished.
- [ ] **Force-stop MapMe, reopen it.** The journey is still there.
- [ ] Pause, then **lock the phone** for two minutes, then resume. Still one
      journey.

The ones that break it:

- [ ] **Lock the screen and put the phone in a pocket for five minutes while
      recording.** The walk must continue. This is what the foreground service
      is for; if the line has a hole in it, the service is not doing its job.
- [ ] The recording notification appears, says a distance and a time, and
      **never shows a location**.
- [ ] Pause and resume from the **notification** rather than the app.
- [ ] Switch Auto/Light/Dark **mid-walk**. Recording must continue, the trail
      must stay on the map, and no points may be lost. The trail vanishing on a
      theme switch is the specific bug the style-reload path exists to prevent.
- [ ] Rotate the phone mid-walk. Nothing resets.
- [ ] Pan away from yourself mid-walk, explore, then recentre. Recording
      continues the whole time.
- [ ] Revoke location permission in Settings **during** a walk, then return.
      MapMe should say the journey stopped and **keep what was already walked**.
- [ ] Turn location services off entirely mid-walk. Same.
- [ ] Aeroplane mode mid-walk: tiles stop, **GPS and recording do not**. The
      line should keep growing over a blank map.
- [ ] Tap Finish and then "Keep walking" — the journey must survive.

Long-walk sanity, if you get the chance:

- [ ] A journey of **thirty minutes or more** stays smooth to pan and zoom. The
      trail is a map layer rather than per-frame drawing precisely so that a
      long walk does not get slower, and that claim wants checking once.

### Haptics
- [ ] Each of the five feelings is distinguishable.
- [ ] `milestone` feels different from `confirm` — that is its whole job.
- [ ] Nothing vibrates that should not.

### Lifecycle and state
- [ ] Rotate on every screen. Nothing is lost, nothing crashes.
- [ ] Background the app, wait a minute, return.
- [ ] Kill from recents and relaunch.
- [ ] Reboot the phone and launch.
- [ ] Split screen / freeform, if the device supports it.

### Accessibility
- [ ] Font size at maximum: does anything clip or overlap?
- [ ] Display size at maximum.
- [ ] TalkBack through each screen. Statistics read as one sentence, not three fragments. Decorative icons stay silent.
- [ ] Every tappable thing is at least 48dp.

### Network and hardware
- [ ] Aeroplane mode: nothing breaks, nothing spins forever, no technical error text.
- [ ] Slow network.

---

## From the sprint that adds location

- [ ] Permission screen explains *why* before the system dialog. No dark patterns.
- [ ] Deny → the app is still usable and still friendly. Never blames you.
- [ ] Deny permanently → there is a way back, explained.
- [ ] Grant "while using" only.
- [ ] Grant background, if asked for at all.
- [ ] Revoke permission while the app runs.
- [ ] GPS switched off entirely.
- [ ] Indoors with a poor fix: **does a bad reading become a ridiculous line?** (§15)
- [ ] Standing still for ten minutes: does a fake journey appear?
- [ ] Battery drain over an hour of recording. Write the number down.
- [ ] Battery saver on.
- [ ] Recording survives the app being backgrounded, and being killed from recents.
- [ ] Recording survives a reboot, or fails in a way that is explained.

---

## The last question

Constitution §26: *if you removed the app name and showed this to someone,
would they recognise a thoughtfully designed premium product?*

If no, the sprint is not finished. Write down what specifically said "no" —
that sentence is the next thing to fix.
