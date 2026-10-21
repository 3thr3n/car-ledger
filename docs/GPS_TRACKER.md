# GPS trackers

A GPS tracker in the car (e.g. an ESP32 with a GPS module) sends its track points to CarLedger. The firmware is a
separate project; this page is the contract of CarLedger with it.

## Trackers and tokens

A tracker belongs to one car. The owner of the car creates it and gets its token **once**, CarLedger only stores the
SHA-256 of it. A lost token can't be shown again: revoke the tracker and create a new one.

The token works only for `/api/device/**`, never for the rest of the API, and the login of a user doesn't work there.
A revoked tracker gets 401, its points stay.

The owner manages the trackers in *Edit car* → *GPS trackers*, scripts can use the API of the users:

| Request                                  | Answer                                                         |
|------------------------------------------|----------------------------------------------------------------|
| `GET /api/tracker/{carId}`               | the trackers of the car with created, last seen and revoked    |
| `PUT /api/tracker/{carId}` `{"name":…}`  | 201 with the tracker and its `token`                           |
| `POST /api/tracker/{carId}/{id}/revoke`  | the tracker with `revokedAt`                                   |

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

| Property                       | Default | Meaning                                                          |
|--------------------------------|---------|------------------------------------------------------------------|
| `carledger.tracker.rate-limit` | 60      | requests per tracker and minute, counted per instance of the app |
| `carledger.tracker.max-batch`  | 500     | points per request                                               |

As environment variables: `CARLEDGER_TRACKER_RATE_LIMIT`, `CARLEDGER_TRACKER_MAX_BATCH`.

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
