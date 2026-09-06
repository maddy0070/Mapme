# 0003 — How a journey is stored

**Status:** Accepted (Sprint 2B).
**Decides:** what a recorded walk is written to, and in what format.

## Decision

**An append-only text journal, one file per journey, in the app's private
internal storage.** Not a database.

## What made the decision

Constitution §24 for this sprint: *do not silently lose a journey.* That is not
a general wish for reliability — it is a specific and unusually harsh
constraint, because **a walk happens once**. A corrupted spreadsheet can be
rebuilt from its inputs; an hour someone spent walking by a river cannot. There
is no retry.

The failure that actually threatens a journey is not disk corruption. It is
**the process going away mid-walk**: Android reclaiming a backgrounded app, a
flat battery, a dropped phone. Everything else follows from designing for that.

## Why not Room

Room is the conventional answer and it was seriously considered.

- **Crash behaviour is the point, and a journal wins it outright.** Each
  accepted reading is one line, appended and flushed as it arrives. A process
  killed mid-walk leaves a file that is a valid journey minus its last second.
  There is no transaction to roll back and no half-written row.
- **The access pattern does not need a database.** Journeys are written once,
  appended during recording, read whole. There is no query here that an index
  would make faster.
- **It can actually be tested from this project.** Room needs an instrumented
  test or Robolectric, neither of which this repository can run — CI is the
  compiler and there is no device. `java.io` against a temp directory means
  persistence is verified on every CI run rather than asserted in a report.
  Given the requirement above, an untestable persistence layer was close to
  disqualifying on its own.
- **No schema migration.** A format where unknown lines are skipped can gain a
  title, a note or a photo reference without a migration or a version bump.

Room becomes right when journeys need querying — history by date, filtering,
aggregate statistics across hundreds of walks. That is a later sprint, and
migrating means reading every journal once and writing rows.

## The format

```
v 1
i <journey id>
b                                  <- a segment begins
p <millis> <lat> <lon> <acc> <alt|-> <spd|->
b                                  <- resumed after a pause
p ...
e <millis>                         <- finished; absent if it never was
```

One line per fact; the first character says what it is. **Unknown prefixes are
skipped**, which is what makes it forward compatible. A file with no `e` line
is a journey that was interrupted, and it reads back as exactly that — ending
at its last reading, which is what happened.

`b` lines are why a pause survives a restart: segments are the shape of the
walk, not a runtime detail.

## Privacy

- **Internal storage** (`filesDir`), private to the app. Not external storage,
  which would make someone's movements readable by any app with storage access.
- `allowBackup` is already `false`, so journeys are not copied to a cloud
  backup the person never agreed to.
- **No coordinate is ever logged**, at any level, in any build.
- **No coordinate leaves the device.** There is no backend, no analytics, and
  no account. The recording notification carries distance and duration and
  never a location — it is visible on a lock screen.

## Consequences

- Reading every journey means parsing every file. Fine at tens of journeys,
  and the reason a database is expected later rather than never.
- The format is ours, so an exporter (GPX, GeoJSON) is a future task rather
  than something inherited.
- Ids become filenames, so they are sanitised; a test covers an id trying to
  climb out of the directory.
