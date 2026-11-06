# Changelog

The notable changes of each release, in the format of [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
From 1.0.0 on the versions follow the promises of [docs/STABILITY.md](docs/STABILITY.md): a breaking change needs a
new major version, and what is deprecated is listed under **Deprecated** until it is removed. The upgrade steps of each
release are in [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md#upgrading-to-the-current-version).

The release job puts the entry of the released version at the top of its
[GitHub release](https://github.com/3thr3n/car-ledger/releases), next to the artifacts and the list of the pull
requests. Changes of a pull request go under **Unreleased**; the release names it with its version and date.

## [Unreleased]

## [1.0.0] - unreleased

The first stable release, for own installations (owner, family and friends). From here on the API, the backups, the
CSV formats, the chart values and the device API of the trackers stay compatible within 1.x
([docs/STABILITY.md](docs/STABILITY.md)). An upgrade from 0.3.0 or later is expected to work; back up the database
first. The changes before are in the releases [0.2.0 to 0.11.0](https://github.com/3thr3n/car-ledger/releases).

### Added

- Links to an Impressum and a privacy policy for installations other people use: `carLedger.legal.imprintUrl` and
  `carLedger.legal.privacyUrl` show them on the landing page, in the sidebar and in the mails, answered by the public
  `GET /api/legal`. A draft of the privacy policy in English and German lists what CarLedger processes
  ([docs/PRIVACY_TEMPLATE.md](docs/PRIVACY_TEMPLATE.md)). ([#609](https://github.com/3thr3n/car-ledger/pull/609))
- Map matching with tiles built on another machine: `hack/map-matching/build-tiles.sh` builds them into an image of a
  registry, the chart `map-matching` 0.2.0 serves them with `tiles.volume` (Kubernetes 1.33+) and `imagePullSecrets`,
  in seconds and with about 150 MB of memory. ([#602](https://github.com/3thr3n/car-ledger/pull/602))

### Changed

- With map matching, the line between two matched points follows the road instead of cutting the curves: migration
  1.38.0 stores the road and sets the matched points to be matched once again. The distances of the track, its drives
  and the suggested trips are measured along the road. ([#608](https://github.com/3thr3n/car-ledger/pull/608))
- The map of a month is calculated in about half the time with a fraction of the memory, most of all how often the
  roads were driven. ([#606](https://github.com/3thr3n/car-ledger/pull/606))
- The default extract of the map matching is Germany, Austria and Switzerland in one file (`dach-latest`).
  ([#602](https://github.com/3thr3n/car-ledger/pull/602))
- The charts `car-ledger` and `stack` are 1.0.0.

### Fixed

- The tiles of the map matching failed to build after about 30 minutes (`vector::_M_range_check`) from the three
  extracts of Germany, Austria and Switzerland that overlap at their borders.
  ([#602](https://github.com/3thr3n/car-ledger/pull/602))

[Unreleased]: https://github.com/3thr3n/car-ledger/compare/1.0.0...develop
[1.0.0]: https://github.com/3thr3n/car-ledger/compare/0.11.0...1.0.0
