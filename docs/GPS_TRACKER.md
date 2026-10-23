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

[{"ts":"2026-10-20T20:15:03Z","lat":51.1,"lon":10.2,"speed":42.3,"alt":250.5,"hdop":1.2}]
```

- `ts`: GPS time, ISO-8601 UTC; `lat`, `lon`: degrees; `speed`: km/h; `alt`: meters. `ts`, `lat` and `lon` are
  required, the others may be missing.
- At most 500 points per request (the firmware sends 200).
- A point sent again (same tracker and `ts`) is stored once, so a batch can be sent again safely.
- Invalid points are skipped, not rejected: a time more than 5 minutes in the future or before 2020, a position outside
  of -90..90 / -180..180, a speed outside of 0..400 or an hdop of 0 or less.

| Status | Meaning                                                    | Tracker                         |
|--------|------------------------------------------------------------|---------------------------------|
| 200    | `{"accepted":n,"duplicates":n,"rejected":n}`               | drops the batch, it is stored   |
| 400    | not JSON, not an array of points, or more than 500 points  | drops the batch                 |
| 401    | missing, unknown or revoked token                          | keeps the batch, sends it later |
| 429    | more than 60 requests per minute, see `Retry-After`        | keeps the batch, sends it later |
| 5xx    | error of the server                                        | keeps the batch, sends it later |

The last contact of a tracker (`lastSeenAt`) is updated at most once per minute.

## Settings

| Property                         | Default | Meaning                                                             |
|----------------------------------|---------|---------------------------------------------------------------------|
| `carledger.tracker.rate-limit`   | 60      | requests per tracker and minute, counted per instance of the app    |
| `carledger.tracker.max-batch`    | 500     | points per request                                                  |
| `carledger.tracker.firmware-dir` |         | directory with a release of the firmware, see [Flashing](#flashing) |

As environment variables: `CARLEDGER_TRACKER_RATE_LIMIT`, `CARLEDGER_TRACKER_MAX_BATCH`,
`CARLEDGER_TRACKER_FIRMWARE_DIR`.

## Flashing

CarLedger can flash the tracker firmware onto an ESP32 and write its settings (WLAN, server URL, token) from the
browser, over USB with Web Serial: no PlatformIO, no settings compiled into the firmware. It needs Chrome or Edge on a
desktop and HTTPS (or `localhost`); other browsers see a hint instead.

The firmware isn't public and not part of CarLedger or its image. An admin puts a release into a directory and sets
`carledger.tracker.firmware-dir`; CarLedger serves it only to logged-in users, and only the files its `manifest.json`
lists. Without the directory, flashing is hidden. The directory is read on every request, a new release needs no
restart.

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
token. For an existing tracker, paste its token or issue a new one there. The two actions:

- **Install firmware**: writes the parts of the manifest and the settings.
- **Update settings only**: writes only the settings, e.g. a new WLAN or token.

Neither erases the whole flash: the points the tracker hasn't sent yet stay. The tracker restarts afterwards, the
dialog shows its boot output, e.g. `Config: 1 WLAN(s), url=…, CA=bundle`.

| Request                                   | Answer                                                   |
|-------------------------------------------|----------------------------------------------------------|
| `GET /api/tracker/firmware`               | `{"version":…}`, without a version flashing isn't set up |
| `GET /api/tracker/firmware/manifest.json` | the manifest, 404 when flashing isn't set up             |
| `GET /api/tracker/firmware/{file}`        | a file of the manifest, 404 for any other                |

### `manifest.json`

```json
{
  "name": "CarLedger GPS tracker",
  "version": "v1.0.0",
  "builds": [
    {
      "chipFamily": "ESP32",
      "parts": [
        { "path": "bootloader.bin", "offset": 4096 },
        { "path": "partitions.bin", "offset": 32768 },
        { "path": "firmware.bin", "offset": 65536 }
      ]
    }
  ],
  "config": { "offset": 4128768, "size": 8192, "maxLength": 8192, "version": 1 }
}
```

`builds` follows the manifest of [ESP Web Tools](https://esphome.github.io/esp-web-tools/), the build of the detected
chip is written. `config` is the partition with the settings. A path is a file name next to the manifest; a manifest
with another path, or missing fields, turns flashing off with a warning in the log.

### Settings

The settings are JSON followed by one NUL byte, padded with `0xFF` to a multiple of 4 bytes, at most
`config.maxLength` bytes with the NUL:

```json
{
  "version": 1,
  "wifi": [{ "ssid": "Home", "pass": "secret-password" }],
  "url": "https://carledger.example.com/api/device/points",
  "token": "cl_…",
  "ca": "-----BEGIN CERTIFICATE-----\n…\n-----END CERTIFICATE-----\n"
}
```

| Field   | Rules                                                                                            |
|---------|--------------------------------------------------------------------------------------------------|
| `wifi`  | at least one network, 2.4 GHz only; `ssid` 1–32 bytes, `pass` empty (open network) or 8–63 bytes |
| `url`   | `https://`, `http://` only for local tests                                                       |
| `token` | the token of the tracker                                                                         |
| `ca`    | optional PEM root certificate for a private or self-signed CA, else the built-in CA bundle       |

Without valid settings the tracker keeps logging points but doesn't upload, its ERR LED blinks at 1 Hz and it prints
the reason on boot, e.g. `Config: no token`.

## Retention and deletion

Track points are personal data. In *Edit car* → *GPS trackers* → *Track history* the owner chooses how long the points
of the car are kept (forever by default, or 7 days to 2 years); every night at 02:30 (time zone of the server) the
points older than that are deleted. *Delete all tracks* deletes all points of the car at once, its trackers stay.
Deleting a car deletes its trackers and points as well.

| Request                                          | Answer                                                  |
|--------------------------------------------------|---------------------------------------------------------|
| `PUT /api/track/{carId}/retention` `{"days":30}` | 204; `{"days":null}` keeps them forever, 1 to 3650 days |
| `DELETE /api/track/{carId}`                      | `{"points":n}`, the points deleted                      |

The backup of a user contains the trackers of each car, with the SHA-256 of their tokens, never the tokens: after a
restore on another server the trackers keep sending. A tracker whose hash is taken on the server already gets a new
one, set it up again with a new token. The points are in the backup only for cars that keep them forever, as a CSV per
car (`tracker,ts,lat,lon,speed,alt,hdop`); a car that keeps them for a while is backed up without its points, so a
backup kept for longer doesn't keep them either. A restore takes the points that are in the file and skips invalid
ones like the ingest.

## Map

The tab *Map* of a car shows its tracks of a day or a period of at most 31 days, colored by speed (orange: slow, dark
blue: fast, 0 to 160 km/h). It appears for cars with a tracker once a map is set up:

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

## Not in the OpenAPI spec

`/api/device/**` isn't part of `openapi.json` and the generated client of the frontend: the package
`de.codeflowwizardry.carledger.rest.device` is excluded with `mp.openapi.scan.exclude.packages` in
`application.properties`. This page describes it instead.
