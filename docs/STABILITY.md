# Stability

What stays compatible within 1.x. A change that breaks one of these rules waits for 2.0. Releases before 1.0.0 are not
covered.

## API

The REST API under `/api` (`openapi.json`), also used by scripts with a
[service account](DEPLOYMENT.md#service-accounts), only grows within 1.x:

- **Allowed**: new endpoints, new optional fields in requests, new fields in answers, new values of an enum, new
  optional query parameters.
- **Not before 2.0**: removing or renaming an endpoint, a field, a query parameter or an enum value, changing the type
  or the meaning of a field, making an optional field required, changing the status code of an answer that worked.

Clients ignore unknown fields and expect new enum values. `/api` has no version in its path.

A security fix can reject input that was accepted before (e.g. a length limit) in a minor release, the release notes
name it.

## Backup

A backup of a 1.x can be restored by every later 1.x. The format (`version` 1 in `backup.json`) only gets new optional
fields. Backups since 0.5.0 should restore as well (`BackupTest#shouldRestoreABackupOf050`, never changed).

## CSV

- **Import**: the formats in the [README](../README.md#csv-format) (refuels and charges, the date formats, the
  columns) stay accepted, and so does every export of a 1.x that can be imported again (e.g. the charges).
- **Export**: a column keeps its name, its place and its meaning; new columns are added at the end.

## Upgrade

An upgrade from every version since 0.3 directly to the newest one should work; a released migration is never
changed. Back up the database first ([Upgrading](../README.md#upgrading)), exceptions are in
[DEPLOYMENT.md](DEPLOYMENT.md#upgrading-to-the-current-version).

## Charts

The values of the charts (`car-ledger`, `stack`, `keycloak`, `authentik`) are stable within 1.x. A renamed or removed
value keeps working for at least one more minor release, with a warning in the chart notes. New values default to the
existing behaviour, unless a release note says otherwise.

## Device API of the trackers

The interface between CarLedger and the firmware of a GPS tracker (`/api/device/**`,
[tracker-device-api.yaml](tracker-device-api.yaml), the manifest of [TRACKER_FIRMWARE.md](TRACKER_FIRMWARE.md)) is
stays supported in spec 1 for all of 1.x. A new spec version within 1.x is an addition.

## Breaking changes

A change is breaking when an installation, a script or a tracker stops working with a later 1.x without being changed:

- an API contract that is removed or changed (see [API](#api)),
- a backup of a 1.x that can't be restored anymore,
- a CSV file of the documented formats or of a 1.x export that isn't imported anymore, or an export column that is
  renamed, moved or removed,
- a chart value that stops working without the minor release in between,
- a tracker of spec 1 that can't send its points or be flashed anymore.

Not covered: the database schema, the pages and texts of the app, the logs and the management port (`/q/*`).
