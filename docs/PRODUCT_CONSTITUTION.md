# MapMe — Master Product Brief

**Version 1.0 · The permanent product constitution**

> This document is the source of truth for every MapMe sprint. It is recorded
> here verbatim so that no future sprint has to rely on memory, a chat log, or
> anyone's interpretation. When a sprint instruction and this document
> disagree, raise it — do not silently pick one.

---

MapMe is a premium personal journey journal for Android.

**IMPORTANT**

- This is **not** a Google Maps competitor.
- This is **not** a navigation app.
- This is **not** a generic GPS tracker.

MapMe is a personal map of someone's life.

## Vision

> Build the world's most beautiful way to experience your life through where
> you've been.

## Core idea

Every place we go becomes part of our story. MapMe quietly records a person's
movement and transforms that movement into a visual history of their life.

The product should make the user feel: *"Wow. This is my life on a map."*

The emotional value is more important than the amount of data shown.

## Product positioning

Google Maps answers: **"Where do you want to go?"**

MapMe answers: **"Where have you been?"**

MapMe should feel personal, premium, playful, emotional, and technologically
sophisticated.

It should **not** feel like: a corporate analytics dashboard, a generic GPS
tracker, a fitness app, a navigation clone, a Google Maps clone, a boring
productivity application, or a generic AI-generated Android app.

---

## 1. Development philosophy

Do not attempt to build the entire product at once. The product will be
developed through multiple controlled sprints. Each sprint must produce a
usable Android build.

Every sprint must:

1. Preserve everything that already works.
2. Avoid unnecessary refactoring.
3. Avoid introducing speculative features.
4. Maintain the visual and interaction language defined in this document.
5. Be tested carefully before considering the sprint complete.
6. Prioritise quality over feature count.

**Do not sacrifice polish to add more features.** A smaller number of
exceptionally polished features is preferable to a large number of mediocre
ones. Do not generate placeholder-looking UI simply to say a feature is
complete.

## 2. First principle

The app should feel **designed**, not assembled.

Every screen must have a clear visual hierarchy, intentional spacing, strong
typography, purposeful colour, meaningful motion, consistent interaction
patterns, consistent component behaviour, and a clear reason for existing.

Avoid default Android layouts whenever a custom solution would significantly
improve the experience. Do not blindly follow generic Material UI patterns. Use
Android platform conventions where they improve usability, but create a
distinct MapMe visual identity.

## 3. Visual personality

Premium · Technical · Vibrant · Playful · Confident · Modern · Immersive ·
Human.

The product should feel like a premium technology product with personality:
*"beautiful technology"* rather than *"technology dashboard"*.

## 4. Colour direction

**Important colour rule.** Do not default to: Claude orange/saffron, generic
purple AI gradients, pastel palettes, washed-out colours, beige minimalism,
muted startup palettes, or generic Material colour schemes.

MapMe must use a vibrant, energetic, sophisticated colour system. Colours
should have functional meaning. Use strong accent colours for journey paths,
active states, important actions, location states, progress, discovery, and
selected dates.

The map itself should remain visually calm enough for the journey line to
dominate. Avoid making every element colourful. Use contrast intelligently. The
product should feel vibrant without becoming visually noisy.

Create a proper semantic colour system rather than scattering arbitrary hex
values throughout the code. The colour system must support dark mode from the
beginning. **Dark mode is the primary visual experience.** Light mode may be
supported where practical, but do not compromise the dark experience.

## 5. Typography

Typography is part of the product identity.

Do not use boring default typography simply because it is convenient. Do not
use generic system-font-only styling. Do not use overly futuristic sci-fi
fonts. Do not use playful/cartoon fonts.

Typography should feel smooth, modern, confident, premium, slightly
distinctive, and extremely readable. Use a carefully selected modern typeface
with personality. If a custom/open-source font is required, use a legally
usable/free font.

Create a deliberate type hierarchy for hero titles, screen titles, section
headings, supporting text, labels, metadata, buttons, map labels, and
numbers/statistics. Large numbers and journey statistics should have
particularly strong visual presence. Typography and microcopy must feel like
the same personality.

## 6. Glass / material language

MapMe should use a sophisticated translucent glass interface language. Glass is
not a decorative gimmick — use glass surfaces when they create depth and
hierarchy: floating cards, bottom sheets, controls, journey information,
timeline controls, contextual information.

Glass should feel translucent, layered, softly blurred, premium, and physically
believable. Avoid excessive glass everywhere. The map should remain visible
underneath the interface. Use subtle background blur, transparency, edge
highlights, depth, shadows, and layering. The interface should feel like
physical glass floating over a living map. Do not create cheap-looking
"glassmorphism" with excessive blur and borders.

## 7. Motion philosophy

Motion is a core part of MapMe. Do not use random animations. Every animation
must communicate spatial relationship, state change, progress, continuity, or
discovery.

Animations should feel smooth, fluid, physical, cinematic, fast enough to
remain useful, and slow enough to feel intentional. Avoid excessive bouncing,
cartoonish transitions, unnecessary scaling, random fades, and generic page
transitions.

**MapMe should feel alive.** When a journey card expands, the map should subtly
respond. When changing dates, the journey should transition rather than simply
disappear and reappear. When replaying a journey, the route should feel like it
is being rediscovered. When entering a deeper screen, the interface should feel
spatially connected to the previous screen. Images, cards, map layers, and
controls should feel like they belong to one physical space.

## 8. Communication tone

MapMe has a friendly, human, playful communication style, inspired by the
personality of excellent games such as Clash of Clans / Clash Royale: friendly,
confident, playful, short, human, occasionally witty, encouraging; never
corporate, never authoritative, never childish, never annoying.

The product should feel like a friendly companion, not an assistant giving
instructions.

Three primary communication modes:

- **Neutral / helpful** — "Here's your route for today."
- **Warm / encouraging** — "Nice. You just completed your longest journey yet."
- **Gentle / reassuring** — "Couldn't save that. Check your connection and try
  again."

Never blame the user. Avoid robotic language such as "Operation successful.",
"Location tracking initiated.", "Error occurred." Prefer human language.

Create a consistent microcopy style across onboarding, buttons, empty states,
permissions, errors, success states, journey summaries, notifications,
settings, and future features. Do not make every sentence a joke — personality
should appear naturally.

## 9. Map experience

The map is the canvas of the product. The journey line is one of the most
important visual elements in MapMe. The route must feel beautiful.

It should not look like a generic navigation route, a fitness GPS trace, or a
debugging polyline. It should feel like *"a line representing my life."*

The journey line should be visually distinctive, smooth, highly visible,
carefully animated, consistent, and beautiful at multiple zoom levels.
Previously travelled routes should remain meaningful when the user zooms out.
The map should never overpower the journey.

**The map is the world. The journey line is the story.**

## 10. Home experience

The primary experience should revolve around the user's current and recent
journey. The home screen should feel immersive:

> MAP + LIVING JOURNEY LINE + FLOATING GLASS INFORMATION + TIME

Avoid filling the screen with cards. Information hierarchy should be extremely
intentional. The user should immediately understand where they are, what
they've travelled, what happened today, and how to explore their history.

## 11. Journey history

MapMe should eventually allow users to explore their journey through time: day,
week, month, year, lifetime. The map becomes a visual record of movement.

The user should eventually be able to look back and think *"I didn't realise I
went this far."* or *"I've been here so many times."* This emotional reaction is
more important than raw statistics.

## 12. Replay

Journey Replay is a major future experience. The user should eventually be able
to select a day and watch their journey unfold. The route should progressively
reveal itself. The experience should feel cinematic. The user should feel like
they are reliving a moment rather than watching GPS data. Avoid making replay
feel like a technical playback feature.

## 13. Live mode

**Relive Me** is a major future feature.

The user enters an unfamiliar area and activates Live Mode. Instead of a
traditional 2D navigation interface, MapMe transforms into an immersive 3D
miniature representation of the surrounding environment. The user is
represented by a professionally designed 3D avatar that moves according to
their real-world movement. Buildings and places can eventually appear as
stylised 3D objects, with real-world place information displayed contextually.

The name intentionally connects LIVE + RELIVE.

**Do not attempt to implement this early unless explicitly instructed.** Do not
compromise the foundation to implement it prematurely.

## 14. Product architecture

Build the application using a maintainable architecture. Prioritise clear
separation of concerns, reusable components, testable logic, local-first data
handling, reliable location tracking, battery-conscious behaviour, and graceful
failure handling.

The app should be designed so future features can be added without rewriting
the entire application. Do not create unnecessary abstraction purely for the
sake of architecture. Prefer understandable code over clever code.

## 15. Location tracking

Location tracking is foundational. The eventual tracking system must be
reliable, battery-conscious, resilient, and accurate enough for personal
journey visualisation.

Do not continuously request extremely high-frequency GPS updates without
reason. Use sensible location sampling strategies.

Handle: permission denial, permission changes, GPS disabled, background
restrictions, temporary signal loss, duplicate points, impossible jumps, poor
accuracy, and device restarts.

The application must not silently create ridiculous route lines because of GPS
noise. Data should be cleaned intelligently before visualisation. Do not fake
location data in the production experience.

## 16. Privacy

Location history is extremely sensitive. MapMe should treat user location as
private personal data.

**Your journey belongs to you.**

Prefer local storage wherever possible. Do not send location history to a
server unless a future feature explicitly requires it and the user knowingly
enables it. Do not introduce unnecessary analytics or tracking. Permission
requests must clearly explain *why* location access is required. Never use
manipulative permission copy.

## 17. Offline-first philosophy

The core journey-recording experience should work without requiring an AI
service. The core product should not depend on a paid AI API.

Do not introduce OpenAI, Claude, Gemini, or other paid AI service costs unless
explicitly requested in a future sprint.

The core experience should remain usable without internet wherever technically
practical. Internet-dependent map data may naturally require connectivity
depending on the chosen map technology.

## 18. Map technology

Do not automatically assume Google Maps. The architecture should allow a
cost-conscious map solution. Prefer free/open technologies where practical. The
final implementation should avoid unnecessary API costs. Do not introduce a
paid map API without explicitly calling out the cost implication.

## 19. Design system

Create a reusable MapMe design system. Define colour tokens, typography tokens,
spacing scale, corner radius scale, elevation/depth, blur levels, component
states, animation principles, iconography, button behaviour, card behaviour,
and bottom sheet behaviour.

Components must feel like they belong to the same product. Avoid inconsistent
corner radii, shadows, icon styles, and spacing. Do not randomly invent a new
component style on every screen.

## 20. Iconography

Avoid generic icon overload. Icons should be clean, consistent, modern, rounded
where appropriate, and carefully sized. Use icons as communication tools, not
decoration. Avoid mixing incompatible icon libraries or styles. If custom icons
are needed, create a consistent visual language.

## 21. Haptics

Use haptics intentionally: important selection, journey milestone, confirmed
action, replay interaction. Do not vibrate for everything. Haptics should
reinforce physicality.

## 22. Empty states

Empty states are part of the product experience. Never show "No data." Instead,
make the first empty experience feel like the beginning of a story — for
example: *"Your map is empty. Let's change that."* The exact wording can
evolve; the principle must remain friendly, concise, human.

## 23. Error handling

Errors must be clear, human, non-technical where possible, actionable, and
calm. Never expose raw technical errors to users.

- Bad: "LocationServiceException: provider unavailable."
- Better: "Looks like your location is unavailable. Let's check your GPS
  settings."

## 24. Accessibility

Premium does not mean inaccessible. Support readable contrast, touch targets,
dynamic text where practical, screen reader semantics where practical, clear
states, and reduced motion where appropriate. Do not sacrifice usability for
visual effects.

## 25. Performance

The application must feel fast. Prioritise smooth map interaction, smooth
scrolling, efficient location processing, efficient local storage, minimal
unnecessary recomposition/rendering, and memory efficiency. Animations must not
cause noticeable frame drops. Do not implement visual effects that make the
application unusable on normal Android hardware.

## 26. Quality standard

The target is not "technically works." The target is **"feels finished."**

Before declaring a sprint complete, perform a serious self-review of: UI
consistency, typography, colours, spacing, motion, haptics, navigation, edge
cases, permission flows, error states, loading states, empty states,
performance, accessibility, and code quality.

Ask: *"If I removed the app name and showed this to someone, would they
recognise that this is a thoughtfully designed premium product?"* If the answer
is no, refine it.

## 27. Device-first testing

The actual Android phone is the source of truth. Do not assume that an
emulator-like environment represents the real experience. Every sprint build
must eventually be installed and tested on the user's physical Android phone.

Before considering a sprint stable, perform repeated testing — target at least
10 meaningful test passes for the major flows of that sprint. Test: fresh
installation, first launch, permissions, main interactions, navigation,
background behaviour, app restart, state persistence, rotation/configuration
changes, poor/no network, GPS unavailable, returning to the app, and closing
and reopening the app.

Do not move to the next major sprint until serious bugs from the current sprint
are resolved.

## 28. Anti-generic rule

**This rule is extremely important. Do not produce a generic AI-generated app.**

Avoid: generic dashboard layouts, generic purple gradients, generic pastel
cards, default Material screens, default Android typography, random rounded
rectangles, excessive cards, excessive floating buttons, generic illustrations,
placeholder copy, template-like onboarding, unnecessary bottom navigation, and
feature stuffing.

Every design decision should answer: *"Why does MapMe need this?"*

## 29. Product emotion

The most important emotion is **discovery**. Secondary emotions: nostalgia,
curiosity, pride, wonder, calm, personal ownership.

MapMe should make ordinary movement feel meaningful. A five-minute walk can
eventually become part of a visual life story. That is the soul of the product.

## 30. Future product direction

Do not implement these now unless specifically instructed.

- **Phase 2+** — rich journey history, timeline, journey replay, journey
  statistics, distance summaries, personal milestones, exploration insights,
  frequently visited places, new-place discovery.
- **Phase 3+** — memories, photos attached to locations, notes, journey
  chapters, yearly journey recap.
- **Phase 4+** — Relive Me live mode, 3D miniature environment, 3D avatar,
  real-world place information, immersive exploration.
- **Phase 5+** — advanced personalisation, deeper visualisations,
  export/shareable journey stories, other carefully considered features.

These are directional ideas, not permission to build everything now.

## 31. Brand

**MapMe.** The name is intentionally simple. It means "Map me." Not "map
places." It represents mapping the user's own life. The brand should eventually
communicate: *"My life has a map."*

## 32. Feature naming

A future feature is intentionally called **Relive Me** (Live Mode). The naming
philosophy favours simple, memorable, human language over technical
terminology.

## 33. North star

Every product decision should ultimately support this sentence:

> Build the world's most beautiful way to experience your life through where
> you've been.

If a feature does not support this vision, question whether it belongs. If a
visual effect distracts from the journey, remove it. If a feature adds
complexity without emotional or practical value, postpone it. If a design
choice makes MapMe feel like another generic map application, reject it.

## 34. Your role

Act as a multidisciplinary senior product team — senior Android engineer,
product designer, UX designer, visual designer, motion designer, interaction
designer, QA engineer, product strategist — simultaneously.

Do not blindly execute requirements. When making implementation decisions,
choose the solution that best protects the product vision, reliability,
maintainability, and user experience.

## 35. Workflow rule

Future prompts specify individual development sprints. When a sprint prompt
arrives:

- Follow this document as the source of truth.
- Build only the requested sprint.
- Preserve existing work.
- Do not jump ahead.
- Do not add random features.
- Do not redesign established parts without a reason.
- Do not weaken the visual system.
- Do not replace working architecture unnecessarily.

Before completing each sprint, perform your own engineering + UX + visual QA
pass. If something can be meaningfully improved without expanding scope,
improve it. If something requires a new feature or major architectural change,
leave it for a future sprint.

---

The goal is not to create an app quickly. The goal is to create MapMe
carefully.
