# GPS trackers

A GPS tracker in the car (e.g. an ESP32 with a GPS module) sends its track points to CarLedger. The firmware is a
separate project; this page is the contract of CarLedger with it.

## Trackers and tokens

A tracker belongs to one car. The owner of the car creates it and gets its token **once**, CarLedger only stores the
SHA-256 of it. A lost token can't be shown again: issue a new one for the tracker (the old one stops working at once,
the tracker and its points stay), or revoke the tracker and create a new one.

The token works only for `/api/device/**`, never for the rest of the API, and the login of a user doesn't work there.
A revoked tracker gets 401, its points stay.

The owner manages the trackers in *Edit car* → *GPS trackers*, scripts can use the API of the users:

| Request                                 | Answer                                                      |
|-----------------------------------------|-------------------------------------------------------------|
| `GET /api/tracker/{carId}`              | the trackers of the car with created, last seen and revoked |
| `PUT /api/tracker/{carId}` `{"name":…}` | 201 with the tracker and its `token`                        |
| `POST /api/tracker/{carId}/{id}/token`  | the tracker and its new `token`, 409 for a revoked tracker  |
| `POST /api/tracker/{carId}/{id}/revoke` | the tracker with `revokedAt`                                |

## Sending points

```http
POST /api/device/points
Authorization: Bearer cl_…
Content-Type: application/json

[{"ts":"2026-10-20T20:15:03Z","lat":51.1,"lon":10.2,"speed":42.3,"alt":250.5,"hdop":1.2,
  "accel":0.12,"brake":0,"lateral":-0.31,"gsource":"imu"}]
```

- `ts`: GPS time, ISO-8601 UTC; `lat`, `lon`: degrees; `speed`: km/h; `alt`: meters. `ts`, `lat` and `lon` are
  required, the others may be missing.
- The g-forces since the point before, in g with 2 decimals: `accel` the peak forward acceleration and `brake` the peak
  braking, both 0 or more; `lateral` the peak sideways acceleration, + turning right, - turning left. `gsource` says
  where they come from: `imu` (real peaks) or `gps` (averages over a second, noticeably lower for the same drive);
  without it, values come from the GPS (firmware before the IMU), another source is stored as `unknown`. A firmware
  before the g-forces sends none of them. The map shows them, see [Map](#map).
- At most 500 points per request (the firmware sends 200).
- A point sent again (same tracker and `ts`) is stored once, so a batch can be sent again safely.
- Invalid points are skipped, not rejected: a time more than 5 minutes in the future or before 2020, a position outside
  of -90..90 / -180..180, a speed outside of 0..400 or an hdop of 0 or less. An implausible g-force only leaves out
  that value, the point is stored: `accel` or `brake` below 0 or above 2 g, `lateral` beyond ±2 g.

| Status | Meaning                                                    | Tracker                         |
|--------|------------------------------------------------------------|---------------------------------|
| 200    | `{"accepted":n,"duplicates":n,"rejected":n}`               | drops the batch, it is stored   |
| 400    | not JSON, not an array of points, or more than 500 points  | drops the batch                 |
| 401    | missing, unknown or revoked token                          | keeps the batch, sends it later |
| 429    | more than 60 requests per minute, see `Retry-After`        | keeps the batch, sends it later |
| 5xx    | error of the server                                        | keeps the batch, sends it later |

The last contact of a tracker (`lastSeenAt`) is updated at most once per minute.

## Status

Whenever its WLAN connects, also without points, a tracker reports its status (capability `status` of the
[firmware spec](TRACKER_FIRMWARE.md)), with the same token:

```http
POST /api/device/status
Authorization: Bearer cl_…
Content-Type: application/json

{"spec":1,"firmware":"v1.1.0","storageUsed":262144,"storageTotal":1048576,"storageError":false,
 "resetReason":"watchdog","uploadResult":"ok","uploadStatus":200,"configOk":true,"ssid":"Home","rssi":-67,
 "parts":["sd","oled","imu"]}
```

All fields are optional, a field out of its range is left out and unknown ones are ignored (the fields and their
ranges are in [`tracker-device-api.yaml`](tracker-device-api.yaml)). The answer is 204, 401 for a revoked tracker and
429 above the limit of requests, which the status shares with the points. It updates the last contact like the points.

CarLedger keeps only the latest status of each tracker, no history, and shows it on the tracker in *Edit car* → *GPS
trackers*: firmware, storage, last upload, last start, WLAN and the installed parts. Warnings: storage that can't be
written (`storageError`), invalid settings (`configOk` false) and *Different firmware* when the tracker runs another
version than the one offered for flashing. The status is deleted with its tracker and part of the backup.

## Settings

| Property                               | Default | Meaning                                                                      |
|----------------------------------------|---------|------------------------------------------------------------------------------|
| `carledger.tracker.rate-limit`         | 60      | requests per tracker and minute, counted per instance of the app             |
| `carledger.tracker.max-batch`          | 500     | points per request                                                           |
| `carledger.tracker.firmware-dir`       |         | directory with a release of the firmware, see [Flashing](#flashing)          |
| `carledger.tracker.stop`               | 5M      | a drive ends after a stop this long, see [Suggested trips](#suggested-trips) |
| `carledger.tracker.min-drive-distance` | 500     | meters a drive needs to be suggested                                         |
| `carledger.tracker.min-drive-duration` | 2M      | how long a drive needs to be suggested                                       |

As environment variables: `CARLEDGER_TRACKER_RATE_LIMIT`, `CARLEDGER_TRACKER_MAX_BATCH`,
`CARLEDGER_TRACKER_FIRMWARE_DIR`, `CARLEDGER_TRACKER_STOP`, `CARLEDGER_TRACKER_MIN_DRIVE_DISTANCE`,
`CARLEDGER_TRACKER_MIN_DRIVE_DURATION`. Durations like `5M` (minutes) or `PT90S`. With Helm, the limits of the
suggested trips are `carLedger.tracker.suggestions.stop`, `.minDriveDistance` and `.minDriveDuration`, with the same
defaults.

## Flashing

CarLedger can flash the tracker firmware onto an ESP32 and write its settings (e.g. WLAN, server URL, token) from the
browser, over USB with Web Serial: no PlatformIO, no settings compiled into the firmware. It needs Chrome or Edge on a
desktop and HTTPS (or `localhost`); other browsers see a hint instead. The firmware has to follow the
[firmware interface spec](TRACKER_FIRMWARE.md): its manifest declares what CarLedger offers and which settings it
asks for.

The firmware isn't public and not part of CarLedger or its image. An admin puts a release into a directory and sets
`carledger.tracker.firmware-dir`; CarLedger serves it only to logged-in users, and only the files its `manifest.json`
lists. Without the directory, or with a manifest that doesn't follow a supported version of the spec, flashing is
hidden (with a warning in the log). The directory is read on every request, a new release needs no restart.

```text
firmware/
├── manifest.json
├── bootloader.bin
├── partitions.bin
└── firmware.bin
```

With Helm, `carLedger.tracker.firmware.volume` takes any volume source with the release, mounted read-only; a
ConfigMap is too small for the firmware:

```yaml
carLedger:
  # for an image from a private registry, applies to the app image as well
  imagePullSecrets:
    - name: regcred
  tracker:
    firmware:
      # an OCI image with the files (Kubernetes 1.33+), or e.g. persistentVolumeClaim: {claimName: tracker-firmware}
      volume:
        image:
          reference: registry.example.com/car-ledger/tracker-firmware:v1.0.0
          pullPolicy: IfNotPresent
      # directory of the volume with the manifest.json, its root without one
      subPath: ""
```

In *Edit car* → *GPS trackers*, *Set up over USB* opens the flashing, also right after creating a tracker, with its
token. For an existing tracker, paste its token or issue a new one there. The form of the settings comes from the
manifest. The actions, as far as the manifest declares them in its
[capabilities](TRACKER_FIRMWARE.md#capabilities):

- **Install firmware** (`install`): writes the parts of the manifest and the settings.
- **Update firmware only** (`update-firmware`): writes the parts of the manifest and keeps the settings on the
  tracker, no form needed. With other partitions on the tracker than the ones of the release, it asks first, see
  [the layout](TRACKER_FIRMWARE.md#the-layout).
- **Update settings only** (`settings`): writes only the settings, e.g. a new WLAN or token.
- **Read settings** (`read-settings`): fills the form with the settings on the tracker, also its token, so only the
  changes need to be entered. The token of a tracker just created stays.

None of them erases the whole flash: the points the tracker hasn't sent yet stay. The tracker restarts afterwards, the
dialog shows its boot output, e.g. `Config: 1 WLAN(s), url=…, CA=bundle`.

| Request                                   | Answer                                                   |
|-------------------------------------------|----------------------------------------------------------|
| `GET /api/tracker/firmware`               | `{"version":…}`, without a version flashing isn't set up |
| `GET /api/tracker/firmware/manifest.json` | the manifest, 404 when flashing isn't set up             |
| `GET /api/tracker/firmware/{file}`        | a file of the manifest, 404 for any other                |

### `manifest.json` and the settings

Described in the [firmware interface spec](TRACKER_FIRMWARE.md): the [manifest](TRACKER_FIRMWARE.md#manifest), its
[JSON Schema](../src/main/resources/tracker/manifest.schema.json) and how the [settings](TRACKER_FIRMWARE.md#settings)
are encoded and described.

## Retention and deletion

Track points are personal data. In *Edit car* → *GPS trackers* → *Track history* the owner chooses how long the points
of the car are kept (forever by default, or 7 days to 2 years); every night at 02:30 (time zone of the server) the
points older than that are deleted. *Delete all tracks* deletes all points of the car at once, its trackers stay. A
wrong track, e.g. GPS jumps after a cold start, goes with *Delete this track* on the tab *Map*: the points of the day or
period shown. Trips and odometers never depend on the track, only the track of a trip and its GPS distance follow.
Deleting a car deletes its trackers and points as well.

| Request                                          | Answer                                                                         |
|--------------------------------------------------|--------------------------------------------------------------------------------|
| `PUT /api/track/{carId}/retention` `{"days":30}` | 204; `{"days":null}` keeps them forever, 1 to 3650 days                        |
| `DELETE /api/track/{carId}?from=…&to=…`          | `{"points":n}`, the points deleted from `from` (inclusive) to `to` (exclusive) |
| `DELETE /api/track/{carId}`                      | `{"points":n}`, all points of the car                                          |

The backup of a user contains the trackers of each car, with the SHA-256 of their tokens, never the tokens: after a
restore on another server the trackers keep sending. A tracker whose hash is taken on the server already gets a new
one, set it up again with a new token. The points are in the backup only for cars that keep them forever, as a CSV per
car (`tracker,ts,lat,lon,speed,alt,hdop,accel,brake,lateral,gsource`: the time in epoch seconds, latitude and
longitude with 6 decimals, about 0.1 m, speed, altitude and hdop with 1, the g-forces with 2); a car that keeps them for a while is backed up without its points, so a
backup kept for longer doesn't keep them either. A restore takes the points that are in the file and skips invalid
ones like the ingest; it also reads the ISO times of the backups of the dev builds before 0.7.0 and the points without
g-forces of the backups before 0.10.0 (`tracker,ts,lat,lon,speed,alt,hdop`).

A point takes about 6 bytes in the ZIP, a year of commuting at one point per second (2.3 million points) 14 MB: above
the upload limit of 12 MB (`carLedger.attachments.maxRequestSize`), raise it to restore such a backup, see
[DEPLOYMENT.md](DEPLOYMENT.md). `hack/perf/measure.cjs` measures the map, the suggested trips, the backup and the
deletions with a year of points in the dev mode.

## Suggested trips

With the trip log on, the tab *Trips* shows the drives of the GPS tracks without a trip as *Drives without a trip*:
since the end of the last trip, of at most the last 7 days. GPS never adds a trip on its own: *Add as trip* opens the
past trip, prefilled, and the user confirms or corrects it; the checks of the trip log apply. *Dismiss* hides a drive
for good, also when it is found a little differently later. Dismissed drives are not part of the backup.

- A drive ends after a stop longer than `carledger.tracker.stop`: no points (the tracker is off), or a car that stays
  within 30 m (the drift of the GPS) while the tracker keeps sending. A red light or a jam that moves doesn't end it.
- A drive needs a stop before it: the rest of a drive that was on the road at the end of the last trip isn't one.
- Drives shorter than `carledger.tracker.min-drive-distance` or `carledger.tracker.min-drive-duration` (parking, GPS
  drift) and drives overlapping a trip, also an open one, are not suggested.
- The odometers are prefilled: the start with the highest odometer known before the drive (of the car, a maintenance
  or a trip; the one of the car without any) or the end of the suggestion before, the end with the start and the GPS
  distance rounded to km. Start and end are a few seconds off: the drift radius delays them.

| Request                                                   | Answer                                                               |
|-----------------------------------------------------------|----------------------------------------------------------------------|
| `GET /api/trip/{carId}/suggestions`                       | the drives with `startedAt`, `endedAt`, `distance` and the odometers |
| `POST /api/trip/{carId}/suggestions/dismiss`              | 204, `{"startedAt":…,"endedAt":…}` of the drive                      |
| `GET /api/trip/{carId}/suggestions/drive?start=…&end=…`   | one drive of the map as a suggestion, see [Drives](#drives)          |

## Map

The tab *Map* of a car shows its tracks of a day or a period of at most 31 days, colored by speed (orange: slow, dark
blue: fast, 0 to 160 km/h) or by how often the car drove each road in the period (*Frequency*: yellow and thin once,
dark red and wide the most often). The frequency draws every street once, for all drives on it: the drives, on their
shared line (below), lie on a grid of 8 m, each drive is a path from cell to cell, and the pieces of all paths between
junctions and ends are the streets, at the average position of their cells. A drive marks its cells and the ones
around, so the drift of the GPS between two drives doesn't matter. Both directions of a street count, a crossing
street doesn't (a pass counts for its direction ±56°), and a cell counts again once the car was away from it for more
than 5 minutes: standing or a jam doesn't add a pass, the way back after a stop does. A street is cut where its
passes change, e.g. where a drive turned in. Small detours of the grid (up to 40 m) are left out, a roundabout stays.
Streets closer than about 10 m may merge. A busy month is about 80 streets, 30 KB, in about a second.

*G-forces* colors the track by the largest of the [g-forces](#sending-points) of each piece: forward acceleration,
braking and cornering to either side, from calm (blue) over yellow to harsh (red). The g-forces of a point are the
peaks since the point before, so they color the piece to it. The IMU measures real peaks, the GPS only averages over a
second, noticeably lower for the same drive: each source has its own range of the same colors, harsh from 0.5 g of the
IMU and from 0.3 g of the GPS (a guess, to tune with real drives), so a period with both doesn't look calmer because of
the GPS. Pieces without g-forces (a firmware before them) are gray. Hovering the track shows the g-force there, its
kind and source. Like the speeds, the drives are drawn on their shared line, also in fullscreen and for picked drives.

A street driven more than once shows as one line, in both views: the GPS drifts by a few meters from drive to drive
and the two directions use different lanes. CarLedger lays the drives of the period on a grid of 8 m and moves each
point sideways onto the average of the drives around it in about the same direction (both directions count as one, a
crossing street doesn't pull). The more often a street is driven, the steadier its line; one driven once stays as it
is. Streets closer than about 10 m may merge. Only the map is changed: the stored points, the trips, the suggestions,
the places and the backup use the points as the tracker sent them, and so does the track of a single trip. With
[map matching](#map-matching) the points are on the roads before, the shared line only moves the ones without a road.

The server simplifies the lines only by 1 m and sends them compact (`[lon, lat, seconds, speed]` per point, gzip; for
*G-forces* also `[accel, brake, lateral, imu]`, imu 1 or 0, all `null` without, only in that mode so the others stay as
small); the
map simplifies further by its zoom, so the drives of a street stay together at every zoom (a simplification per drive
by the length of the period took them apart again by up to 50 m, #526). A point left out passes its g-forces on to the
next one kept, so the peaks stay: the highest acceleration and braking and the strongest cornering of the points in
between. The speeds are drawn as lines of the same band of 10 km/h, the g-forces of a tenth of the range of their
source, each line with the highest of its pieces, so a short harsh braking isn't averaged away. A busy month (about 2000 km, a point every 2 s) is about 940 KB, 320 KB with gzip.

It appears for cars with a tracker once a map is set up:

| Property                  | Helm value               | Meaning                                                                       |
|---------------------------|--------------------------|-------------------------------------------------------------------------------|
| `carledger.map.style-url` | `carLedger.map.styleUrl` | URL of a MapLibre style (`style.json`); without one the map is hidden         |
| `carledger.map.sources`   | `carLedger.map.sources`  | further origins of tiles, glyphs or sprites, e.g. `https://tiles.example.com` |

The browser loads the style and the tiles itself, CarLedger only adds their origins to its Content Security Policy:
the origin of the style URL always, the further sources if the style loads from other hosts. Anything that isn't an
origin (`https://host[:port]`) is left out with a warning in the log.

Examples (check the terms of use of the provider):

- [OpenFreeMap](https://openfreemap.org): `https://tiles.openfreemap.org/styles/liberty`, free and without a key,
  used in dev mode
- [MapTiler](https://www.maptiler.com): `https://api.maptiler.com/maps/streets-v2/style.json?key=<key>`
- An own server, e.g. [Martin](https://martin.maplibre.org) or [tileserver-gl](https://github.com/maptiler/tileserver-gl)
  with an OpenStreetMap extract

The public tile servers of OpenStreetMap (`tile.openstreetmap.org`) aren't meant for apps like this and provide no
MapLibre style.

## Map matching

Optional: a self-hosted [Valhalla](https://valhalla.github.io/valhalla/) snaps the tracks to the roads of
OpenStreetMap, so all drives of a street lie on exactly the same line, and cut corners and gaps in tunnels follow the
road. Without it (the default) everything stays as before: the map shows the shared line of the drives. It has to be
self-hosted: a public service would get the positions of the users.

**Set up** with the stack chart, `global.mapMatching.enabled=true`: the chart `map-matching` runs Valhalla as
`car-ledger-map-matching` and the app uses it by itself. On the first start Valhalla downloads the extracts of
`mapMatching.extracts` (Germany, Austria and Switzerland of [Geofabrik](https://download.geofabrik.de) by default) and
builds its routing tiles into its volume (`mapMatching.persistence.size`, 40Gi): about an hour and up to 8 GB of
memory (`mapMatching.resources`), the startup probe waits up to 12 hours. Later starts use the tiles of a
finished build, a failed one (`valhalla_build_tiles failed` in the log) is built again. Outside of the
stack, install `helm/map-matching` and set `carLedger.map.matchingUrl` (e.g. `http://map-matching.maps:8002`) and
`networkPolicy.extraFrom` of the chart for the namespace of the app. Its NetworkPolicy lets only the app in.

**How**: the points that arrive while a service is set up wait for the matching. Every 2 minutes the app takes the
finished drives (no point of the car for 5 minutes) and sends each, in parts of at most 2000 points, to
`trace_attributes` with `map_snap`. The position on the road is stored next to the one of the GPS, which stays. A
point Valhalla leaves out, one more than 50 m from its road (a parking lot) and a drive without a road near it (off-road,
a new road) keep the position of the GPS, the shared line of the map applies to them. Without a reachable service the
points wait and are matched once it is back. `carledger_map_matching_points_total{result=matched|unmatched}` counts
them ([Metrics](DEPLOYMENT.md#metrics)).

**Where**: the map (speeds, g-forces, frequency), the distances of its drives and the distance of the
[suggested trips](#suggested-trips) use the positions on the roads. The trip log keeps its odometers, the track of a
single trip, the stops of the places and the backup use the points of the GPS; restored points are matched again.

**Older points** (sent before the service was set up) aren't matched by themselves. To match them, e.g. of the last
year:

```bash
kubectl exec car-ledger-postgresql-0 -- psql -U postgres car-ledger -c \
  "UPDATE track_point SET tk_match = 'P' WHERE tk_match IS NULL AND tk_ts > now() - interval '1 year'"
```

**New map data** is loaded by hand, e.g. every few months: delete the extracts and the tiles, then the pod downloads
and builds them again on its start (the map matching waits meanwhile), and match the points again if wanted:

```bash
kubectl exec car-ledger-map-matching-0 -- sh -c \
  'rm -rf /custom_files/*.osm.pbf /custom_files/valhalla_tiles /custom_files/valhalla_tiles.tar /custom_files/file_hashes.txt'
kubectl delete pod car-ledger-map-matching-0
# once it is ready again
kubectl exec car-ledger-postgresql-0 -- psql -U postgres car-ledger -c \
  "UPDATE track_point SET tk_match = 'P' WHERE tk_match IS NOT NULL"
```

| Property                              | Helm value                  | Meaning                                                     |
|---------------------------------------|-----------------------------|-------------------------------------------------------------|
| `carledger.map.matching.url`          | `carLedger.map.matchingUrl` | base URL of Valhalla; without it nothing is matched         |
| `carledger.map.matching.max-distance` |                             | meters, a position further from its road keeps the GPS (50) |
| `carledger.map.matching.every`        |                             | how often the finished drives are matched (`2m`, `off`)     |
| `carledger.map.matching.timeout`      |                             | of a request (`60s`)                                        |
| `carledger.map.matching.max-points`   |                             | points of one request, a longer drive is split (2000)       |

## Places

The switch *Places* below the map shows the places the car stopped at at least 3 times in the period, e.g. a fuel
station or the office, as markers with the number of visits; a click names the place, renames it or removes its
name. A visit is the end of a drive, found like the [suggested trips](#suggested-trips) (a stop longer than
`carledger.tracker.stop`), but a drive of 200 m and 1 minute is enough: the fuel station around the corner counts.
Stops within 75 m of each other (more than the drift of the GPS) are one place, its marker sits in the middle of the
stops. The threshold of 3 visits and the radius are fixed.

The name is stored with the position, the place itself is found again in the stops: a named place keeps its name when
the car stops there again later, and shows without visits (e.g. after its track points were deleted). Naming a place
within 75 m of a named one renames that one. The named places are part of the backup and deleted with the car.

| Request                                                | Answer                                                                        |
|--------------------------------------------------------|-------------------------------------------------------------------------------|
| `GET /api/track/{carId}/places?from=…&to=…`            | the places with `visits` and `lastVisit`, the named ones with `id` and `name` |
| `PUT /api/track/{carId}/places` `{"lat","lon","name"}` | 204, names the place at the position or renames a named one near it           |
| `PUT /api/track/{carId}/places/{id}` `{"name"}`        | 204, renames it                                                               |
| `DELETE /api/track/{carId}/places/{id}`                | 204, removes the name                                                         |

## Drives

Below the map, the drives of the period are listed by day, newest first, with their times, duration, distance and the
named places at both ends. A drive is found like a visit of a [place](#places): from a stop to the next (longer than
`carledger.tracker.stop`, also with the tracker on), at least 200 m and 1 minute; one already on the road at the start
of the period starts there. Picking drives shows only them on the map and zooms to them, the speeds and the frequency
included; *Show all* goes back. Hovering a drive in the list highlights it on the map, a click on the track picks its
drive. The period and the picked drives are part of the URL, so a reload or a link shows the same.

The owner deletes a single drive, e.g. a broken one: its points and those of the stop after it, up to the start of the
next drive (or the end of the period). Only the points of the drive would leave the stops before and after it, minutes
apart after a short drive, and the map would join them with a straight line, a drive again. The other drives, the
places and the trips stay.

With the trip log on, who may add trips (owner, editor) adds a drive without a trip as a past trip: *Add as trip* opens
it prefilled like a [suggestion](#suggested-trips), also for a drive that was dismissed or is older than 7 days. Its
start odometer is the highest one known before the drive, not the end of the drive before: only the suggestions follow
each other. A drive that a trip overlaps (`trip` in the list) has no button.

| Request                                                      | Answer                                                                   |
|--------------------------------------------------------------|--------------------------------------------------------------------------|
| `GET /api/track/{carId}/drives?from=…&to=…`                  | the drives with `start`, `end`, `distance` (km), `from`, `to` and `trip` |
| `GET /api/track/{carId}?from=…&to=…&drive=start/end&drive=…` | the track of only these drives, each a segment of its own                |
| `GET /api/track/{carId}/frequency?…&drive=start/end`         | the passes of only these drives                                          |

## Not in the OpenAPI spec

`/api/device/**` isn't part of `openapi.json` and the generated client of the frontend: the package
`de.codeflowwizardry.carledger.rest.device` is excluded with `mp.openapi.scan.exclude.packages` in
`application.properties`. This page and its own OpenAPI file, [`tracker-device-api.yaml`](tracker-device-api.yaml),
describe it instead.
