# GPS trackers

A GPS tracker in the car (e.g. an ESP32 with a GPS module) sends its track points to CarLedger. The firmware is a
separate project; this page is the contract of CarLedger with it.

## Trackers and tokens

A tracker belongs to one car. The owner creates it and sees its token **once**, CarLedger stores only its SHA-256. A
lost token can't be shown again: issue a new one (the old one stops working at once) or revoke the tracker.

The token works only for `/api/device/**`, the login of a user doesn't work there. A revoked tracker gets 401, its
points stay.

The owner manages the trackers in *Edit car* → *GPS trackers*, or with the API:

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

- `ts`: GPS time, ISO-8601 UTC; `lat`, `lon`: degrees; `speed`: km/h; `alt`: meters. Only `ts`, `lat` and `lon` are
  required.
- G-forces since the point before, in g: `accel` peak forward acceleration and `brake` peak braking (both ≥ 0),
  `lateral` peak sideways (+ right, - left). `gsource`: `imu` or `gps` (averages, lower); missing means `gps`.
- At most 500 points per request.
- A point sent again (same tracker and `ts`) is stored once.
- Invalid points are skipped: time more than 5 minutes in the future or before 2020, position out of range, speed
  outside 0..400, hdop ≤ 0. An implausible g-force (`accel`/`brake` outside 0..2, `lateral` beyond ±2) is left out, the
  point is stored.

| Status | Meaning                                                    | Tracker                         |
|--------|------------------------------------------------------------|---------------------------------|
| 200    | `{"accepted":n,"duplicates":n,"rejected":n}`               | drops the batch, it is stored   |
| 400    | not JSON, not an array of points, or more than 500 points  | drops the batch                 |
| 401    | missing, unknown or revoked token                          | keeps the batch, sends it later |
| 429    | more than 60 requests per minute, see `Retry-After`        | keeps the batch, sends it later |
| 5xx    | error of the server                                        | keeps the batch, sends it later |

## Status

Whenever its WLAN connects, a tracker reports its status (capability `status` of the
[firmware spec](TRACKER_FIRMWARE.md)), with the same token:

```http
POST /api/device/status
Authorization: Bearer cl_…
Content-Type: application/json

{"spec":1,"firmware":"v1.1.0","storageUsed":262144,"storageTotal":1048576,"storageError":false,
 "resetReason":"watchdog","uploadResult":"ok","uploadStatus":200,"configOk":true,"ssid":"Home","rssi":-67,
 "parts":["sd","oled","imu"]}
```

All fields are optional, the ranges are in [`tracker-device-api.yaml`](tracker-device-api.yaml). The answer is 204, 401
for a revoked tracker, 429 above the request limit (shared with the points).

CarLedger keeps only the latest status and shows it in *Edit car* → *GPS trackers*, with warnings for `storageError`,
`configOk` false and a firmware other than the one offered for flashing.

## Settings

| Property                               | Default | Meaning                                                                      |
|----------------------------------------|---------|------------------------------------------------------------------------------|
| `carledger.tracker.rate-limit`         | 60      | requests per tracker and minute, per instance of the app                     |
| `carledger.tracker.max-batch`          | 500     | points per request                                                           |
| `carledger.tracker.firmware-dir`       |         | directory with a release of the firmware, see [Flashing](#flashing)          |
| `carledger.tracker.stop`               | 5M      | a drive ends after a stop this long                                          |
| `carledger.tracker.min-drive-distance` | 500     | meters a drive needs to be suggested                                         |
| `carledger.tracker.min-drive-duration` | 2M      | how long a drive needs to be suggested                                       |

As environment variables: `CARLEDGER_TRACKER_RATE_LIMIT` etc. With Helm, the last three are
`carLedger.tracker.suggestions.stop`, `.minDriveDistance` and `.minDriveDuration`.

## Flashing

CarLedger flashes the tracker firmware onto an ESP32 and writes its settings (WLAN, server URL, token) from the
browser over USB (Web Serial). It needs Chrome or Edge on a desktop and HTTPS. The firmware has to follow the
[firmware interface spec](TRACKER_FIRMWARE.md).

The firmware isn't part of CarLedger. Put a release into a directory and set `carledger.tracker.firmware-dir`;
CarLedger serves only the files its `manifest.json` lists, to logged-in users. Without it, or with an unsupported
manifest, flashing is hidden (warning in the log). A new release needs no restart.

```text
firmware/
├── manifest.json
├── bootloader.bin
├── partitions.bin
└── firmware.bin
```

With Helm, `carLedger.tracker.firmware.volume` takes any volume source, mounted read-only:

```yaml
carLedger:
  imagePullSecrets:      # for a private registry, applies to the app image as well
    - name: regcred
  tracker:
    firmware:
      volume:            # an OCI image (Kubernetes 1.33+), or e.g. persistentVolumeClaim: {claimName: tracker-firmware}
        image:
          reference: registry.example.com/car-ledger/tracker-firmware:v1.0.0
          pullPolicy: IfNotPresent
      subPath: ""        # directory with the manifest.json
```

*Edit car* → *GPS trackers* → *Set up over USB* offers, as far as the manifest declares them
([capabilities](TRACKER_FIRMWARE.md#capabilities)):

- **Install firmware** (`install`): the firmware and the settings. Asks first over other partitions, see
  [the layout](TRACKER_FIRMWARE.md#the-layout).
- **Update firmware only** (`update-firmware`): keeps the settings. Asks first over other partitions.
- **Update settings only** (`settings`).
- **Read settings** (`read-settings`): fills the form with the settings on the tracker.

None of them erases the points not sent yet.

| Request                                   | Answer                                                   |
|-------------------------------------------|----------------------------------------------------------|
| `GET /api/tracker/firmware`               | `{"version":…}`, without a version flashing isn't set up |
| `GET /api/tracker/firmware/manifest.json` | the manifest, 404 when flashing isn't set up             |
| `GET /api/tracker/firmware/{file}`        | a file of the manifest, 404 for any other                |

The manifest and the settings are described in the [firmware interface spec](TRACKER_FIRMWARE.md).

## Retention and deletion

In *Edit car* → *GPS trackers* → *Track history* the owner chooses how long the points are kept (forever by default, or
7 days to 2 years); older points are deleted every night at 02:30. *Delete all tracks* deletes all points of the car,
*Delete this track* on the tab *Map* the points of the period shown. Trips and odometers don't depend on the points.

| Request                                          | Answer                                                                         |
|--------------------------------------------------|--------------------------------------------------------------------------------|
| `PUT /api/track/{carId}/retention` `{"days":30}` | 204; `{"days":null}` keeps them forever, 1 to 3650 days                        |
| `DELETE /api/track/{carId}?from=…&to=…`          | `{"points":n}`, from `from` (inclusive) to `to` (exclusive)                    |
| `DELETE /api/track/{carId}`                      | `{"points":n}`, all points of the car                                          |

The backup contains the trackers with the hashes of their tokens, so they keep sending after a restore on another
server. The points are in the backup only for cars that keep them forever, as a CSV per car
(`tracker,ts,lat,lon,speed,alt,hdop,accel,brake,lateral,gsource`). A year at one point per second is about 14 MB, above
the default upload limit of the restore, see [DEPLOYMENT.md](DEPLOYMENT.md#options).

## Suggested trips

With the trip log on, the tab *Trips* lists drives of the last 7 days without a trip as *Drives without a trip*. *Add
as trip* opens a prefilled past trip, *Dismiss* hides a drive for good. GPS never adds a trip itself.

A drive ends after a stop longer than `carledger.tracker.stop`. Drives shorter than `min-drive-distance` or
`min-drive-duration` and drives overlapping a trip are not suggested.

| Request                                                   | Answer                                                               |
|-----------------------------------------------------------|----------------------------------------------------------------------|
| `GET /api/trip/{carId}/suggestions`                       | the drives with `startedAt`, `endedAt`, `distance` and the odometers |
| `POST /api/trip/{carId}/suggestions/dismiss`              | 204, `{"startedAt":…,"endedAt":…}` of the drive                      |
| `GET /api/trip/{carId}/suggestions/drive?start=…&end=…`   | one drive of the map as a suggestion, see [Drives](#drives)          |

## Map

The tab *Map* shows the tracks of a day or a period of up to 31 days, colored by speed, by *Frequency* (how often each
road was driven) or by *G-forces* (calm blue to harsh red, separate scales for IMU and GPS; gray without g-forces).
Drives of the same road are drawn on one line.

It appears for cars with a tracker once a map is set up:

| Property                  | Helm value               | Meaning                                                                       |
|---------------------------|--------------------------|-------------------------------------------------------------------------------|
| `carledger.map.style-url` | `carLedger.map.styleUrl` | URL of a MapLibre style (`style.json`); without one the map is hidden         |
| `carledger.map.sources`   | `carLedger.map.sources`  | further origins of tiles, glyphs or sprites, e.g. `https://tiles.example.com` |

The browser loads the style and the tiles, CarLedger adds their origins to its Content Security Policy.

Examples (check the terms of use):

- [OpenFreeMap](https://openfreemap.org): `https://tiles.openfreemap.org/styles/liberty`, free, used in dev mode
- [MapTiler](https://www.maptiler.com): `https://api.maptiler.com/maps/streets-v2/style.json?key=<key>`
- An own server, e.g. [Martin](https://martin.maplibre.org) or [tileserver-gl](https://github.com/maptiler/tileserver-gl)

`tile.openstreetmap.org` provides no MapLibre style and isn't meant for apps like this.

## Map matching

Optional: a self-hosted [Valhalla](https://valhalla.github.io/valhalla/) snaps the tracks to the roads of
OpenStreetMap.

**With the stack chart**: `global.mapMatching.enabled=true`. On the first start Valhalla downloads the extracts of
`mapMatching.extracts` (Germany, Austria and Switzerland as one extract by default; overlapping extracts can fail with
`vector::_M_range_check`) and builds its tiles into its volume (40Gi): about an hour and up to 8 GB of memory.
Outside of the stack, install `helm/map-matching`, set `carLedger.map.matchingUrl` (e.g.
`http://map-matching.maps:8002`) and `networkPolicy.extraFrom` of the chart for the namespace of the app.

**Tiles built on another machine**: for a small server, build the tiles elsewhere (docker or podman, about 4× the size
of the extracts free). The server then starts in seconds with about 150 MB of memory and no volume.

```bash
hack/map-matching/build-tiles.sh git.example.com/me/car-ledger-tiles:2026-11   # THREADS, MEMORY, WORK_DIR, see the script
docker push git.example.com/me/car-ledger-tiles:2026-11                         # or PUSH=true
```

```yaml
mapMatching:          # in the stack; on its own at the root
  tiles:
    volume:           # any volume source; a PVC with tiles.subPath works as well
      image:
        reference: git.example.com/me/car-ledger-tiles:2026-11
        pullPolicy: IfNotPresent
  imagePullSecrets:
    - name: forgejo-registry
```

Switching from tiles built in the cluster: `kubectl delete statefulset car-ledger-map-matching` before the upgrade,
then delete the volume `data-car-ledger-map-matching-0`.

**How**: every 2 minutes the app sends the finished drives (no point for 5 minutes) to Valhalla and stores the position
on the road next to the GPS position. Points far from a road keep the GPS position. Without a reachable service the
points wait. The map, its drive distances and the suggested trips use the matched positions; the trip log, the places
and the backup use the GPS positions.

**Older points** aren't matched by themselves, e.g. for the last year:

```bash
kubectl exec car-ledger-postgresql-0 -- psql -U postgres car-ledger -c \
  "UPDATE track_point SET tk_match = 'P' WHERE tk_match IS NULL AND tk_ts > now() - interval '1 year'"
```

**New map data**: with tiles of another machine, build a new tag and set it. With tiles built in the cluster:

```bash
kubectl exec car-ledger-map-matching-0 -- sh -c \
  'rm -rf /custom_files/*.osm.pbf /custom_files/valhalla_tiles /custom_files/valhalla_tiles.tar /custom_files/file_hashes.txt'
kubectl delete pod car-ledger-map-matching-0
```

Then match the points again if wanted:

```bash
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

The switch *Places* below the map shows places the car stopped at at least 3 times in the period, with the number of
visits. Stops within 75 m are one place. A click names, renames or unnames it; a named place keeps its name.

| Request                                                | Answer                                                                        |
|--------------------------------------------------------|-------------------------------------------------------------------------------|
| `GET /api/track/{carId}/places?from=…&to=…`            | the places with `visits` and `lastVisit`, the named ones with `id` and `name` |
| `PUT /api/track/{carId}/places` `{"lat","lon","name"}` | 204, names the place at the position or renames a named one near it           |
| `PUT /api/track/{carId}/places/{id}` `{"name"}`        | 204, renames it                                                               |
| `DELETE /api/track/{carId}/places/{id}`                | 204, removes the name                                                         |

## Drives

Below the map, the drives of the period are listed by day with times, distance and the named places at both ends.
Picking drives shows only them on the map. The owner can delete a drive (its points up to the next drive). With the
trip log on, *Add as trip* adds a drive without a trip as a past trip.

| Request                                                      | Answer                                                                   |
|--------------------------------------------------------------|--------------------------------------------------------------------------|
| `GET /api/track/{carId}/drives?from=…&to=…`                  | the drives with `start`, `end`, `distance` (km), `from`, `to` and `trip` |
| `GET /api/track/{carId}?from=…&to=…&drive=start/end&drive=…` | the track of only these drives, each a segment of its own                |
| `GET /api/track/{carId}/frequency?…&drive=start/end`         | the passes of only these drives                                          |

## Not in the OpenAPI spec

`/api/device/**` is excluded from `openapi.json` (`mp.openapi.scan.exclude.packages`), it is described by
[`tracker-device-api.yaml`](tracker-device-api.yaml).
