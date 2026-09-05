# 0001 — Map technology

**Status:** Proposed. Not decided, and deliberately not decided during the
foundation sprint.
**Decides:** what draws the basemap under the journey line.

## Why this is written now

Constitution §18: *"Do not automatically assume Google Maps… Do not introduce a
paid map API without explicitly calling out the cost implication."* The map is
the single largest architectural and financial commitment MapMe will make, and
the foundation must not quietly pre-empt it. Writing the options down now means
the map sprint starts from analysis instead of from a default.

Nothing in the foundation depends on a map engine. The design system already
carries `mapLand`, `mapWater`, `mapRoadMinor`, `mapRoadMajor`, `mapBuilding`,
`mapLabel` and `mapLabelHalo` tokens, so whichever engine wins, the basemap is
styled from the same palette as the interface.

## What MapMe actually needs

1. A **calm, dark, custom-styled** basemap. Non-negotiable: the map must recede
   so the trail dominates (§9). An engine that cannot be restyled is disqualified
   however good it looks.
2. **Smooth vector rendering** with a polyline overlay that stays beautiful at
   every zoom.
3. **No per-user cost that scales badly**, and no surprise bill (§17, §18).
4. **Offline tolerance** where practical.
5. Eventually, a path towards **3D** (Relive Me, §13).

## Options

### MapLibre Native (Android)

Open-source fork of Mapbox GL Native, BSD-2. No SDK cost, no attribution
lock-in, full vector style control, 3D terrain and extrusions supported. It
needs a tile source, which is the actual decision:

| Tiles | Cost | Notes |
| --- | --- | --- |
| **OpenFreeMap** | Free, no key | Community-run, no usage limits stated. No SLA. |
| **Protomaps (PMTiles)** | Storage only | One file on object storage, range requests. Fully self-owned, genuinely offline-capable. Most work up front. |
| **MapTiler** | Free tier, then paid | Generous free tier, real SLA, needs a key. |
| **Self-hosted OpenMapTiles** | Server cost | Total control, real ops burden. |

### Google Maps SDK

Excellent quality, `MapsSDKForAndroid` billed per map load beyond the monthly
credit. Styling is constrained to Google's style spec — a genuinely custom dark
map is possible but not free-form. Also the exact product MapMe is defined
*against* (§positioning). **Cost implication: pay-per-load.**

### Mapbox

Superb rendering and styling. Billed on monthly active users. **Cost
implication: per-MAU billing that scales with success.**

### osmdroid

Raster tiles, mature, free. No vector styling, no smooth rotation/tilt, no path
to 3D. Would cap the visual ambition permanently.

## Recommendation

**MapLibre Native + Protomaps, with OpenFreeMap as the fast path.**

- MapLibre gives full style control at zero SDK cost and does not foreclose 3D.
- Starting on OpenFreeMap gets a beautiful custom-styled map running quickly
  with no key and no bill.
- Migrating to a self-hosted PMTiles archive later is a URL change, not a
  rewrite, and is the only option that makes MapMe genuinely offline and
  genuinely nobody else's business — which is the same promise as §16.

Rejected for now: Google Maps and Mapbox, on cost and on styling freedom.
osmdroid, on visual ceiling.

## Consequences if adopted

- A MapLibre style JSON must be authored from the MapMe map tokens. That is
  real design work, not configuration.
- MapLibre Native adds several MB to the APK.
- Tile availability becomes a network dependency; the offline story needs its
  own decision.

## Revisit when

The map sprint begins. Re-read this, confirm the tile source is still healthy,
then supersede this ADR with an accepted one.
