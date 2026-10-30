# Stability

What CarLedger 1.x promises, so that an installation, a script or a tracker keeps working with every later 1.x release.
Every change is checked against these rules; a change that breaks one of them waits for 2.0.

Releases before 1.0.0 are not covered: they could change anything, the upgrade notes in
[DEPLOYMENT.md](DEPLOYMENT.md#upgrading-to-the-current-version) name what changed.

## API

The REST API under `/api` (described by `openapi.json`) is the same contract for the frontend of the app and for
scripts with a [service account](DEPLOYMENT.md#service-accounts). Within 1.x it only grows:

- **Allowed**: new endpoints, new optional fields in requests, new fields in answers, new values of an enum, new
  optional query parameters.
- **Not before 2.0**: removing or renaming an endpoint, a field, a query parameter or an enum value, changing the type
  or the meaning of a field, making an optional field required, changing the status code of an answer that worked.
- New features follow the same rule: once released in a 1.x, they stay.

A client ignores the fields it doesn't know and expects new enum values. `/api` keeps its paths without a version;
the rule replaces an `/api/v1`.

A security fix can reject input that was accepted before (e.g. a length limit) in a minor release, the release notes
name it.

## Backup

A backup of a 1.x can be restored by every later 1.x. The format (`version` 1 in `backup.json`) only gets new
optional fields; a backup without them is restored with the defaults. A new, incompatible version of the format would
still restore the older ones.

The backups of before 1.0.0 are not guaranteed, but every one since 0.5.0 should be restorable as well, like the
upgrades. `BackupTest#shouldRestoreABackupOf050` restores a backup in the shape of 0.5.0 and stays as it is; a change
that makes it fail needs a reason in the release notes.

## CSV

The CSV formats are stable within 1.x like the API:

- **Import**: the formats in the [README](../README.md#csv-format) (refuels and charges, the date formats, the
  columns) stay accepted, and so does every export of a 1.x that can be imported again (e.g. the charges).
- **Export**: a column keeps its name, its place and its meaning; new columns are added at the end.

## Upgrade

An upgrade from every version since 0.3 directly to the newest one should work: the Flyway migrations of the database
run in order, a released migration is never changed afterwards. This is not guaranteed, back up the database first
([Upgrading](../README.md#upgrading)). Exceptions are named in
[DEPLOYMENT.md](DEPLOYMENT.md#upgrading-to-the-current-version), e.g. the bitnami based charts of 0.1.x and the
Keycloak 26.0/26.1 database, which needs Keycloak 26.6 first.

## Charts

The values of the charts (`car-ledger`, `stack`, `keycloak`, `authentik`) are stable within 1.x. A value that is
renamed or removed in a minor release keeps working for at least one more minor release, with a warning in the notes
of the chart (e.g. renamed in 1.2, still read in 1.3, gone in 1.4 at the earliest). New values have defaults that keep
the behaviour of an existing installation, unless a release note says otherwise (e.g. a security default).

Up to 1.0.0 the only values that went away were `carLedger.webhook.*`, removed with the webhooks in 0.9.0.

## Device API of the trackers

The interface between CarLedger and the firmware of a GPS tracker (`/api/device/**`,
[tracker-device-api.yaml](tracker-device-api.yaml), the manifest of [TRACKER_FIRMWARE.md](TRACKER_FIRMWARE.md)) is
promised in the version that 1.0.0 supports, spec 1: every 1.x keeps accepting it. A new spec version within 1.x is an
addition, spec 1 is supported until 2.0. Nothing before 1.0.0 is covered.

The firmware is independent of this repository and has its own versions; it only follows the spec.

## Breaking changes

A change is breaking when an installation, a script or a tracker that worked with a 1.x stops working with a later
1.x without being changed itself:

- an API contract that is removed or changed (see [API](#api)),
- a backup of a 1.x that can't be restored anymore,
- a CSV file of the documented formats or of a 1.x export that isn't imported anymore, or an export column that is
  renamed, moved or removed,
- a chart value that stops working without the minor release in between,
- a tracker of spec 1 that can't send its points or be flashed anymore.

A breaking change needs 2.0. Not covered are what isn't an interface: the schema of the database (only through the
migrations), the pages and texts of the app, the logs, and the endpoints of the management port (`/q/*`).
