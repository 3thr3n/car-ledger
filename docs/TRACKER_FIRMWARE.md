# Tracker firmware interface

CarLedger owns the interface between itself and the firmware of a GPS tracker as a versioned spec. A firmware declares
in its `manifest.json` which version of the spec it implements and what of it, CarLedger offers only that. The
firmware itself is a separate project (e.g. [car-ledger-esp32](https://github.com/3thr3n/car-ledger-esp32)), how
CarLedger serves and flashes it is in [GPS_TRACKER.md](GPS_TRACKER.md#flashing).

This is **spec 1**. It consists of:

| Part                                                                                     | What                                                     |
|------------------------------------------------------------------------------------------|----------------------------------------------------------|
| this page                                                                                | the rules, the capabilities, the settings and their form |
| [`manifest.schema.json`](../src/main/resources/tracker/manifest.schema.json)             | JSON Schema of the manifest, CarLedger validates with it |
| [`tracker-device-api.yaml`](tracker-device-api.yaml)                                     | OpenAPI of `/api/device/**`, what the tracker sends      |

## Compatibility

- Changes to the interface happen here first. A firmware follows and raises its `spec`; one release implements one
  spec version.
- CarLedger supports the current and the previous spec version, and spec 1 (the one of 1.0.0) for all of 1.x, see
  [STABILITY.md](STABILITY.md#device-api-of-the-trackers). A manifest of another version, an invalid one or one
  without `spec` (a firmware of before the spec) turns flashing off with a warning in the log.
- Unknown fields and capabilities are ignored, in the manifest as in the device API. A newer firmware keeps working with
  an older CarLedger and the other way round: what one side doesn't know is skipped and never blocks the upload of
  points.
- Nothing that isn't declared is assumed.

## Manifest

```json
{
  "name": "CarLedger GPS tracker",
  "version": "v1.1.0",
  "spec": 1,
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
  "capabilities": ["install", "settings"],
  "layout": 1,
  "config": {
    "offset": 4128768,
    "size": 8192,
    "maxLength": 8192,
    "format": "json-nul",
    "version": 1,
    "schema": { "…": "the settings, see below" }
  }
}
```

| Field          | Meaning                                                                                                     |
|----------------|-------------------------------------------------------------------------------------------------------------|
| `name`         | name of the firmware                                                                                        |
| `version`      | version of the firmware, shown on the install button                                                        |
| `spec`         | version of this spec the firmware implements                                                                |
| `builds`       | as in the manifest of [ESP Web Tools](https://esphome.github.io/esp-web-tools/): the build of the detected chip is written; a `path` is a file name next to the manifest |
| `capabilities` | what of the spec the firmware implements, see below                                                         |
| `layout`       | version of the flash layout: partition table, config and data partitions. A firmware that moves them raises it |
| `config`       | the partition with the settings: `offset`, `size` and `maxLength` in bytes, `format`, `version` and `schema` |

`config.maxLength` is at most `config.size`. The complete [example manifest](../src/test/resources/firmware/manifest.json)
is the release the tests use.

### Capabilities

| Capability        | CarLedger offers                                                                          |
|-------------------|-------------------------------------------------------------------------------------------|
| `install`         | *Install firmware*: writes the parts of the build and the settings                        |
| `settings`        | *Update settings only*: writes only the config partition, e.g. a new WLAN or token        |
| `update-firmware` | *Update firmware only*: writes the parts of the build, never the config partition, so the settings stay; see [the layout](#the-layout) |
| `read-settings`   | *Read settings*: reads the config partition back (format `json-nul`, `version` of the manifest) to fill the form, e.g. to change only the WLAN |
| `status`          | the tracker reports its status (`POST /api/device/status`), incl. its firmware and spec version, shown on the tracker |

None of them erases the whole flash: the points the tracker hasn't sent yet stay.

### The layout

Before *Update firmware only*, CarLedger reads the partition table on the tracker (at `0x8000`, where the ESP32
bootloader reads it) and compares it byte by byte with the part of the build written there (`partitions.bin`). The
same table means the config and data partitions stay where the new firmware looks for them. Another table, or none
that can be read or found in the build, asks the user first: the update is still possible, a full install is safer. A
firmware that moves its partitions raises `layout` and changes `partitions.bin` with it.

## Settings

### Format `json-nul`

The settings are a JSON object, followed by one NUL byte and padded with `0xFF` to a multiple of 4 bytes, at most
`config.maxLength` bytes with the NUL. The firmware reads up to the first `0x00` or `0xFF`.

```json
{
  "version": 1,
  "wifi": [{ "ssid": "Home", "pass": "secret-password" }, { "ssid": "Open" }],
  "url": "https://carledger.example.com/api/device/points",
  "token": "cl_…"
}
```

- `version` is `config.version`, written by CarLedger first. The schema must not declare it.
- `url` and `token` are **reserved**: the schema must declare both as `"type": "string"`, CarLedger fills them with the
  points endpoint (changeable, `https://` or `http://` for local tests) and the token of the tracker.
- The other fields follow the order of the schema. An optional field left empty is left out, e.g. the `pass` of an open
  network.
- Invalid or missing settings never stop the logging of points: the tracker logs, doesn't upload, and shows the reason
  (e.g. on its serial output: `Config: no token`).

### Schema

`config.schema` describes the settings as a subset of JSON Schema, the form in CarLedger is rendered from it. The
manifest schema rejects what the form can't show.

```json
{
  "type": "object",
  "required": ["wifi", "url", "token"],
  "properties": {
    "wifi": {
      "type": "array",
      "title": "WLAN networks",
      "description": "2.4 GHz only, tried in this order",
      "translations": { "de": { "title": "WLAN-Netze" } },
      "minItems": 1,
      "items": {
        "type": "object",
        "title": "Network",
        "required": ["ssid"],
        "properties": {
          "ssid": { "type": "string", "title": "SSID", "minBytes": 1, "maxBytes": 32 },
          "pass": { "type": "string", "title": "Password", "format": "password", "minBytes": 8, "maxBytes": 63 }
        }
      }
    },
    "url": { "type": "string" },
    "token": { "type": "string" },
    "ca": { "type": "string", "title": "Root certificate (PEM)", "format": "pem", "advanced": true }
  }
}
```

| Keyword                           | For                    | Meaning                                                                  |
|-----------------------------------|------------------------|--------------------------------------------------------------------------|
| `type`                            | all                    | `string`, `integer`, `number`, `boolean`, or `array` of objects of these |
| `required`                        | the object, an item    | fields that can't be empty                                               |
| `title`, `description`            | all, items             | label and help text, the name of the field without a title               |
| `translations`                    | all, items             | `title` and `description` per language, e.g. `{"de": {"title": …}}`      |
| `default`                         | scalars                | initial value                                                            |
| `advanced`                        | fields of the object   | shown folded away                                                        |
| `minBytes`, `maxBytes`            | `string`               | length in UTF-8 bytes, as the firmware stores it                         |
| `minLength`, `maxLength`          | `string`               | length in characters                                                     |
| `pattern`                         | `string`               | regular expression (ECMAScript, `u` flag)                                |
| `format`                          | `string`               | `password` (hidden), `pem` (one certificate, written with `\n` line ends), `uri` (`http(s)://`); others are plain text |
| `minimum`, `maximum`              | `integer`, `number`    | range                                                                    |
| `minItems`, `maxItems`            | `array`                | number of items; the form starts with at least one                       |

Other keywords are ignored. A list holds only scalars, no nested objects or lists.

## Device API

What the tracker sends is described in [`tracker-device-api.yaml`](tracker-device-api.yaml) (OpenAPI 3.1): the points,
`POST /api/device/points` with the token of the tracker, and the status, `POST /api/device/status` (capability
`status`), whenever its WLAN connects, also without points. It isn't part of the `openapi.json` of the app. The status
carries the `spec` version the firmware implements; an older CarLedger answers 404 to it, which the firmware ignores,
and a failed status never holds up the points.