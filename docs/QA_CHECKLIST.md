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
