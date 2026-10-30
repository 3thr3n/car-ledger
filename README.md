# CarLedger

**CarLedger** keeps the costs and the history of your cars in one place: refuels and charges, maintenance, other
costs and recurring costs like insurance, with the receipts attached. It shows the consumption and what a car really
costs, reminds of due dates, keeps a trip log (Fahrtenbuch) and shows the GPS tracks of a car on a map.

It is self-hosted: CarLedger runs on Kubernetes, installed with Helm, and the users log in with OpenID Connect (Keycloak,
authentik or any other provider).

## Table of Contents

- [Features](#features)
- [Installation](#installation)
    - [Requirements](#requirements)
    - [Charts](#charts)
    - [Quick start](#quick-start)
    - [With an existing identity provider](#with-an-existing-identity-provider)
    - [Important values](#important-values)
    - [Versions](#versions)
    - [Upgrading](#upgrading)
    - [Uninstalling](#uninstalling)
- [Usage](#usage)
- [CSV Format](#csv-format)
- [Development](#development)
    - [Architecture](#architecture)
- [Contributing](#contributing)
- [License](#license)

## Features

**Costs**

- **Refuels, charges, maintenance and other costs** per car, corrected or deleted afterwards; calculated values like
  the consumption follow.
- **Electric and hybrid cars:** charges in kWh with the consumption in kWh/100km next to the refuels in liters.
- **Recurring costs** (insurance, tax, leasing) with their payments and files like the contract; ending one removes the
  payments from the end date on.
- **Receipts:** photos and PDFs attached to the entries and the recurring costs, shown small in the lists, a picture per
  car, details like plate, VIN and purchase price.
- **Units:** metric, US (miles, gallons, mpg) or UK (miles, liters, mpg) per account. They are stored in km and liters,
  the API always uses those.
- **Statistics:** totals, averages and the consumption over time per car and for all cars.
- **Accent colors:** one of five presets per account, in light and dark mode.
- **Modifications:** the parts of a tuned car with their ABE, Teiletypgenehmigung, Teilegutachten or Einzelabnahme, the
  conditions, the Abnahme and the entry in the registration certificate, with the documents; removed parts stay as the
  history of the car.
- **HU:** the Hauptuntersuchung with its result, the defects and the report; the next due month moves the reminder of
  the HU.
- **Papers as PDF:** the modifications with their approvals and the HUs on the first pages, their documents and
  reports appended; for a check the installed parts and the latest HU, for selling the whole history.

**On the road**

- **Installable app:** added to the home screen of the phone it starts like an app; a long press on its icon opens a
  new refuel (or charge) of the last opened car.
- **Offline:** refuels, charges and trips entered without a connection are sent when it is back; the documents of the
  modifications of a car can be kept on the device, e.g. for a roadside check.
- **Trip log (Fahrtenbuch):** start and finish trips with the odometer, enter forgotten ones, delete with a reason
  (they stay in the log), export as PDF or CSV.
- **GPS tracking:** a tracker in the car (e.g. an ESP32) sends its points, the app shows the tracks on a map colored
  by speed or by how often a road was driven and the track of a trip, single drives and in fullscreen, names the
  places the car stops at often and suggests forgotten trips from the drives; it keeps the g-forces of the points;
  the owner chooses how long the points are kept, sees the status of the tracker (firmware, storage, last upload) and
  updates only its firmware with the settings kept, see [docs/GPS_TRACKER.md](docs/GPS_TRACKER.md).
- **Reminders and notifications:** due reminders, invitations to cars and the answers to requests in the inbox of the
  app (*Notifications*, kept for 90 days), and by e-mail and push message for the topics you choose.

**Data and integration**

- **Sharing a car:** the owner invites other accounts (by user name or e-mail address) as editor, viewer or driver,
  optionally for a period; the entries show who created them, a driver sees only the own trips and refuels. Only the
  owner backs up a shared car.

- **CSV import and export** of each entry type, and all expenses of a car in one CSV for spreadsheets.
- **Backup:** every user downloads all of their data (settings, cars, entries, recurring costs, reminders, trips,
  modifications and their documents, the HUs, GPS trackers and the tracks kept forever, receipts, contracts and
  pictures) as one ZIP and restores it into an account without cars, e.g. on another installation.
- **Car requests:** users who reached their car limit request more cars, administrators decide on the administration
  page.
- **Service accounts:** scripts use the API with tokens of the identity provider, e.g. to decide car requests, see
  [Service accounts](docs/DEPLOYMENT.md#service-accounts).
- **Administration:** usage statistics, all accounts with their cars, the car limit per account, deleting accounts.

## Installation

CarLedger is installed with the Helm charts of this repository, published at `oci://ghcr.io/3thr3n/car-ledger/<chart>`.
This section gets an installation running; [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md) has the details of every option,
the identity providers and the upgrades.

### Requirements

- **Kubernetes** with **Helm 3.8+** (OCI charts).
- **An ingress controller or the Gateway API**, with **HTTPS**: the login fails over plain http. The charts request the
  certificates from cert-manager when `ingress.certManager` names a cluster issuer; otherwise provide the TLS secret
  yourself.
- **DNS** for the host names below the base domain: `car-ledger.<host>`, plus `keycloak.<host>` or `authentik.<host>` when
  the charts deploy the identity provider (a wildcard record is simplest).
- **A default StorageClass** for the PostgreSQL volumes (8 GiB for the app by default).
- **Memory:** the app requests 256 MiB (limit 768 MiB), its PostgreSQL 256 MiB, plus Infinispan; Keycloak or authentik
  add about 1 GiB each.
- **Optional:** an SMTP server for e-mails, a MapLibre style (own tile server or a provider with a key) for the map of the
  GPS tracks.

### Charts

| Chart        | Deploys                                                                                                   | Docs                                         |
|--------------|-----------------------------------------------------------------------------------------------------------|----------------------------------------------|
| `stack`      | Everything below in one release, the identity provider switched on with one value, all secrets set up      | [README](helm/stack/README.md)               |
| `car-ledger` | The app, its PostgreSQL and Infinispan (the login sessions, shared by all replicas)                       | [values](helm/car-ledger/values.yaml)        |
| `keycloak`   | Keycloak with its PostgreSQL, the realm of CarLedger imported                                             | [DEPLOYMENT.md](docs/DEPLOYMENT.md#fresh-install-with-keycloak) |
| `authentik`  | authentik with its PostgreSQL, CarLedger set up as application                                            | [README](helm/authentik/README.md)           |

For a new installation, use the `stack`. The single charts are for an existing identity provider, separate namespaces or
your own umbrella chart.

### Quick start

The stack with authentik (or `global.keycloak.enabled=true` for Keycloak), with certificates from a cert-manager
cluster issuer `lets-encrypt`. A values file, e.g. `my-values.yaml`:

```yaml
global:
  host: example.com          # CarLedger at car-ledger.example.com, authentik at authentik.example.com
  authentik:
    enabled: true

app:                         # values of the car-ledger chart
  ingress:
    certManager: lets-encrypt
authentik:                   # values of the authentik chart
  ingress:
    certManager: lets-encrypt
```

```bash
helm install car-ledger oci://ghcr.io/3thr3n/car-ledger/stack -f my-values.yaml
```

Name the release `car-ledger` and install it once per namespace, the names of the resources are fixed. The notes of
the release show the next steps:

1. **Log in to the identity provider** as administrator: authentik as `akadmin`
   (`kubectl get secret authentik-bootstrap -o jsonpath='{.data.password}' | base64 -d`), Keycloak as `car-ledger`
   (secret `car-ledger-keycloak-admin`).
2. **Add the users:** in authentik to the group `user` (or `admin`); in Keycloak in the realm `car-ledger`, they get the
   role `user`, administrators get the realm role `admin` as well.
3. **Open `https://car-ledger.<host>`** and log in. Administrators see *Administration* in the sidebar; new accounts may
   have one car, the limit is changed there.

With the Gateway API instead of an ingress, see [the stack README](helm/stack/README.md#values).

### With an existing identity provider

The `car-ledger` chart alone works with any OpenID Connect provider: a confidential client with the authorization code
flow, the redirect URI `https://car-ledger.<host>/api/auth/callback` and the roles `user` and `admin` in the tokens (the
[full list](docs/DEPLOYMENT.md#identity-provider)).

```bash
# the client secret, and a random state secret that encrypts the login cookie
kubectl create secret generic car-ledger-oidc \
  --from-literal=secret='<client secret>' \
  --from-literal=state-secret="$(openssl rand -hex 32)"
```

```yaml
# my-values.yaml
ingress:
  host: example.com
  certManager: lets-encrypt
carLedger:
  auth:
    url: https://login.example.com/realms/main   # the issuer
    clientId: car-ledger
    existingSecret: car-ledger-oidc
```

```bash
helm install car-ledger oci://ghcr.io/3thr3n/car-ledger/car-ledger -f my-values.yaml
```

The chart can also be a dependency of your own chart:

```yaml
dependencies:
  - name: car-ledger
    version: "0.7.x"
    repository: oci://ghcr.io/3thr3n/car-ledger
```

### Important values

Values of the `car-ledger` chart; in the stack they are below `app.` (e.g. `app.carLedger.mail.host`). All of them are
described in [values.yaml](helm/car-ledger/values.yaml), the options in [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md#options).

| Value                                         | Default                         | Description                                                            |
|-----------------------------------------------|---------------------------------|------------------------------------------------------------------------|
| `ingress.host`                                | `global.host` of the stack      | Base domain, the app is `car-ledger.<host>`                            |
| `ingress.className`                           | `nginx`                         | Ingress class                                                          |
| `ingress.certManager`                         |                                 | Cluster issuer of cert-manager for the TLS certificate                 |
| `httpRoute.enabled`                           | `false`                         | Gateway API instead of the ingress (with `ingress.enabled=false`)      |
| `carLedger.auth.url`                          | Keycloak / authentik of the charts | Issuer of the OpenID Connect provider                               |
| `carLedger.auth.clientId`                     | `carLedger`                     | Client of CarLedger at the provider                                    |
| `carLedger.auth.existingSecret`               | `keycloak`                      | Secret with the client secret and the state secret                     |
| `carLedger.mail.host`                         |                                 | SMTP server for the notifications, none sent without it ([E-mail](docs/DEPLOYMENT.md#e-mail)) |
| `carLedger.push.enabled`                      | `true`                          | Push messages to the browsers, the key is generated ([Push](docs/DEPLOYMENT.md#push)) |
| `carLedger.map.styleUrl`                      |                                 | MapLibre style for the map of the GPS tracks, hidden without it        |
| `carLedger.attachments.maxSize`               | `10M`                           | Largest receipt; proxies in front need `maxRequestSize` (`12M`) as well |
| `carLedger.attachments.storage`               | `DB`                            | `S3` with `carLedger.attachments.s3.*` keeps the files in a bucket ([docs](docs/DEPLOYMENT.md#attachments-in-s3)) |
| `carLedger.trust.certificates`                | `[]`                            | CAs the app trusts for its own HTTPS requests ([self-signed](docs/DEPLOYMENT.md#self-signed-certificates)) |
| `carLedger.replicas`                          | `1`                             | Replicas of the app, sessions are shared by Infinispan                 |
| `carLedger.resources`                         | 100m / 256Mi, limit 350m / 768Mi | Resources of the app                                                  |
| `carledgerpostgres.enabled`                   | `true`                          | PostgreSQL of the chart; `false` with `carLedger.postgres.*` for your own |
| `carledgerpostgres.persistence.size`          | `8Gi`                           | Volume of the PostgreSQL                                               |
| `networkPolicy.enabled`                       | `true`                          | Only the app reaches its PostgreSQL and Infinispan                     |

### Versions

A [release](https://github.com/3thr3n/car-ledger/releases) (e.g. `0.10.0`) publishes the image
`ghcr.io/3thr3n/car-ledger:0.10.0` and the charts `car-ledger` and `stack` in the same version, which run that image.
`keycloak` and `authentik` have their own versions, the release notes name them. Without `--version`, Helm installs the
latest release.

Every build of `develop` is published as a prerelease, `<next release>-dev.<run number>` (e.g. `0.11.0-dev.123`): `--devel`
installs the latest one, a version range takes them only with a prerelease suffix (`>=0.11.0-0`). Leave
`carLedger.image.tag` empty, the chart runs the image of its own build.

Up to 0.3.x the app chart was `oci://ghcr.io/3thr3n/car-ledger/helm`, since 0.4.0 it is `car-ledger`.

### Upgrading

```bash
kubectl exec car-ledger-postgresql-0 -- pg_dump -U postgres car-ledger > car-ledger-$(date +%F).sql
helm upgrade car-ledger oci://ghcr.io/3thr3n/car-ledger/stack -f my-values.yaml
```

With the single chart, upgrade `oci://ghcr.io/3thr3n/car-ledger/car-ledger` instead. Pass your values file instead of
`--reuse-values`, which would drop new defaults of the chart. The database is migrated
on the first start of the new version and the migrations can not be undone, so back it up first. Read the
[upgrade checklist](docs/DEPLOYMENT.md#upgrading-to-the-current-version) before upgrading an older installation.

### Uninstalling

```bash
helm uninstall car-ledger
```

The volumes of the PostgreSQL databases stay (`kubectl get pvc`), delete them to remove the data as well. The generated
passwords are removed with the release: to install again on the kept volumes, save the secret `car-ledger-postgresql`
before (`kubectl get secret car-ledger-postgresql -o yaml`) and apply it again first, otherwise the new release gets
a new password that the existing database does not accept.

## Usage

- **Cars:** add a car with its energy type (petrol, diesel, LPG, CNG, electric, plug-in hybrid) and the details; deleting a car deletes all
  of its entries.
- **Entries:** refuels, charges, maintenance and other costs per car, with receipts attached. Correct or delete them
  afterwards, the consumption is calculated again.
- **Recurring costs:** a cost with its interval, the payments follow from it. Ending it (e.g. a cancelled insurance)
  removes the payments from the end date on.
- **Settings:** the default VAT country, the currency, the language, the units, the notifications and the backup are
  stored with your account.
- **Administration:** users with the role `admin` see the usage statistics, all accounts with their cars and entries,
  the car limit per account and the car requests, and delete accounts.
- **CSV export:** the lists of refuels, charges, maintenance and other costs export their entries, all or of the
  selected year, with the field names of the import as header, ISO dates and a dot as decimal separator, so the file
  can be imported again (existing entries are skipped). The ledger on the overview of a car exports all expenses
  including the payments of the recurring costs as one CSV for spreadsheets; it cannot be imported again.

## CSV Format

When importing refuels via CSV, ensure your file follows this format:

| Date       | Fuel Volume (L) | Price per L (€) | Distance (km) | Car estimate (L) |
|------------|-----------------|-----------------|---------------|------------------|
| YYYY-MM-DD | Float           | Float           | Float         | Float            |
| DD.MM.YYYY | Float           | Float           | Float         | Float            |

Example:

```csv
Date,Fuel Volume (L),Price per L (€),Distance (km),Car estimate (L)
2024-10-09,40.03,171.9,551.5,8.0
2024-10-18,38.23,171.9,494.2,8.2
```

or

```
Date,Fuel Volume (L),Price per L (€),Distance (km),Car estimate (L)
09.10.2024,40.03,171.9,551.5,8.0
18.10.2024,38.23,171.9,494.2,8.2
```

### Charges

Charges of electric cars and plug-in hybrids have their own import. The columns are assigned in the import, required
are the date, the kWh and the price per kWh (in cent). The distance is the km since the last charge, the location is
`HOME` or `PUBLIC` (in any case) and the state of charge the percent after charging, all three are optional:

```csv
date,unit,pricePerUnit,distance,location,stateOfCharge
2026-03-01,40,30,250,HOME,80
12.03.2026,25.5,"59,0",120,public,
```

The export of the charges writes these columns with the total and the VAT rate, it can be imported again.

## Development

The backend is Quarkus (Java), the frontend React with Vite in `src/main/webapp`.

```bash
git clone https://github.com/3thr3n/car-ledger.git
cd car-ledger

# backend on http://localhost:8080, with dev services (PostgreSQL, Keycloak; needs Docker)
./mvnw quarkus:dev

# frontend on http://localhost:5173, talks to the backend
cd src/main/webapp
yarn install
yarn dev
```

### Tests

```bash
# backend
./mvnw verify

# frontend unit tests
cd src/main/webapp && yarn test

# end-to-end tests, start Quarkus (with dev services, needs Docker) and Vite themselves,
# running servers are reused. Each spec starts with new accounts of alice and bob (resetAccounts in
# e2e/helpers.ts), so one runs alone too: yarn test:e2e e2e/trips.spec.ts
cd src/main/webapp && npx playwright install chromium && yarn test:e2e

# charts: the template tests of helm/<chart>/tests (plugin helm-unittest) and the rendered
# resources against the schemas of Kubernetes (kubeconform), after helm dependency update
helm unittest --with-subchart=false helm/car-ledger helm/keycloak helm/authentik helm/stack
helm/validate-manifests.sh
```

The build writes the OpenAPI spec of the backend to `src/main/webapp/public/openapi.json`, the frontend client is
generated from it with `yarn openapi-ts`. Commit both, the CI fails if they are outdated.

### Architecture

[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) shows how the parts fit together: the layers of the backend, the access
check of the cars, the storages, the scheduled jobs. Each package of the backend has an overview in its Javadoc;
`./mvnw javadoc:javadoc` builds the site to `target/reports/apidocs`, and the CI fails on broken Javadoc.

### Local cluster

`hack/kind/up.sh` creates a cluster in Docker with [kind](https://kind.sigs.k8s.io) and installs the stack chart from
the working copy: Cilium (also as ingress controller) or ingress-nginx, local-path and cert-manager with a self-signed
CA, which nobody trusts, like in many company clusters. It needs Docker, kind, kubectl and helm and the
ports 80 and 443.

```bash
hack/kind/up.sh                    # Keycloak, Cilium, local-path, builds the image of the working copy
hack/kind/up.sh --idp authentik --ingress nginx --no-build
node hack/kind/login.cjs keycloak alice alice   # logs in with a browser, the user has to exist
hack/kind/down.sh
```

The app is https://car-ledger.localhost, Keycloak https://keycloak.localhost and authentik https://authentik.localhost;
the CA is in `target/kind/ca.crt`. Running `up.sh` again upgrades the stack.

## Contributing

Contributions are welcome! Please follow these steps to contribute:

1. Fork the repository.
2. Create a new branch from `develop` (`git checkout -b feature/YourFeature develop`).
3. Commit your changes (`git commit -m 'Add YourFeature'`).
4. Push to the branch (`git push origin feature/YourFeature`).
5. Open a Pull Request against `develop`.

### Branches and releases

- `develop` gets all pull requests, its builds are published as prereleases (`<next release>-dev.<run number>`).
- Every milestone ends with a security review of the pull requests since the last release, an issue of the
  template *Security review of a milestone* (`.github/ISSUE_TEMPLATE/security-review.md`). Its findings are fixed
  before the release or moved to the next milestone on purpose.
- `main` only holds released states. A release is a pull request from `develop` to `main`, merged with a merge
  commit (not squashed, so both branches keep the same history), then the tag of the version in
  `helm/car-ledger/Chart.yaml` on `main`. Afterwards the next version is set in `Chart.yaml` on `develop`.
- A hotfix branches from `main` and goes to `main` like a release. `develop` keeps a linear history, so the fix comes
  to `develop` as a cherry-pick in a pull request of its own.

## License

Copyright (C) 2024–2026 3thr3n

CarLedger is free software: you can redistribute it and/or modify it under the terms of the
[GNU Affero General Public License version 3](LICENSE) (AGPL-3.0-only) as published by the Free Software Foundation.
It is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the license for more details.

In short: you can use, host and change CarLedger freely, also in a company. If you run a **modified** version for other
users over a network, you have to offer them its source code (section 13 of the license). Releases up to 0.5.x were
published under the MIT License and stay available under it.
