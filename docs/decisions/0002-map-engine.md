# 0002 — Map engine and tile source

**Status:** Accepted (Sprint 2A). Supersedes [0001](0001-map-technology.md).
**Decides:** what draws the basemap, and where the tiles come from.

## Decision

**MapLibre Native Android `13.6.0`, with vector tiles from OpenFreeMap,
rendered through a style MapMe generates from its own design tokens.**

## What changed since 0001

0001 was written without network access to check its own assumptions. Three of
them were wrong, and they were checked from CI before anything was built:

1. **MapLibre is at 13.6.0**, not the 11.x the earlier note implied. The
   package moved to `org.maplibre.android` at 11 and has been stable since.
2. **The Protomaps demo bucket 0001 named as "the fast path" returns 404.**
   There is no free hosted Protomaps basemap to point at. Self-hosting a
   PMTiles archive means owning object storage and a ~100GB planet extract —
   real work, and not this sprint's.
3. **The map tokens 0001 claimed the design system "already carries" did not
   exist.** They were removed with the cyan palette during Sprint 1. They are
   added in this sprint.

So the recommendation survives, but its ordering flips: OpenFreeMap is the
decision, and Protomaps is the documented migration rather than the goal.

## Why MapLibre

| | MapLibre | Google Maps | Mapbox | osmdroid |
| --- | --- | --- | --- | --- |
| Licence | BSD-2, free | Proprietary | Proprietary | Apache 2, free |
| Cost | none | per map load | per monthly active user | none |
| Style control | total — it renders a style spec MapMe writes | constrained to Google's styler | total | none, raster only |
| Vector tiles | yes | n/a | yes | no |
| Offline | yes, via PMTiles later | limited | paid tier | tile cache only |
| Path to 3D | terrain + extrusions | limited | yes | none |
| Trail rendering | GeoJSON line layer, or drawn above the surface | polyline | line layer | crude |

Google Maps and Mapbox are both rejected on **cost that scales with success** —
constitution §§17–18 require an explicit cost callout for a paid map API, and
neither survives it for a product whose promise is that your journeys are
nobody's business. Google Maps is additionally the product MapMe is positioned
*against*; shipping its basemap would undercut the one sentence the product is
built on.

osmdroid is free and mature but raster-only. It would cap the visual ceiling
permanently, and MapMe's entire visual argument is a custom-styled map.

## Why OpenFreeMap

Free, no API key, no account, no usage ceiling published, OpenMapTiles schema,
served as vector tiles with a TileJSON endpoint. Verified live from CI: the
planet TileJSON resolves, `z0–z14`, sixteen source layers.

**The risk is honest and worth stating: there is no SLA.** It is a
community-run service and it can go away. That risk is survivable precisely
because it is a URL:

- the source is addressed by its **TileJSON**, not by a tile pattern, so the
  dated build id in the tile path (`.../planet/20260830_080001_pt/...`) rotates
  without a client change;
- the schema is **OpenMapTiles**, which MapTiler, a self-hosted tile server and
  a Protomaps archive all also serve. Moving is a source URL, not a restyle.

## Consequences

- **APK grows.** MapLibre carries native renderers for four ABIs.
- **Tiles are a network dependency.** Offline gets its own sprint; the load and
  failure states in this one are built to say so honestly rather than hang.
- **The style is now design work under version control.** `MapMeStyle.kt`
  generates it from `MapMeColors`, so the map cannot drift from the product,
  and both themes change together.
- **Attribution is not optional.** OpenStreetMap data is ODbL; the attribution
  control stays enabled and the source carries the string.

## The boundary

The engine is confined to `core/map/MapMeMap.kt`. Everything else — screens,
state, the future trail — speaks `GeoPoint`, `MapPosition`, `MapCameraState`
and `MapLoadState`. `EngineContainmentTest` fails the build if `org.maplibre`
is imported anywhere else, because "keep it replaceable" is not a promise code
review can keep.

## Revisit when

Offline maps are specified, OpenFreeMap's availability changes, or 3D replay
starts. The migration to a self-hosted PMTiles archive is expected to be a
source URL and a style tweak, and that expectation is the reason for the
boundary above.
