# CarLedger

**CarLedger** keeps the costs and the history of your cars in one place: refuels and charges, maintenance, other
costs and recurring costs like insurance, with the receipts attached. It shows the consumption and what a car really
costs, reminds of due dates, keeps a trip log (Fahrtenbuch) and shows the GPS tracks of a car on a map.

It is self-hosted: CarLedger runs on Kubernetes, installed with Helm, and the users log in with OpenID Connect (Keycloak,
authentik or any other provider).

## Features

**Costs**

- **Refuels, charges, maintenance and other costs** per car; the consumption follows every correction. Electric and
  hybrid cars track kWh next to liters.
- **Recurring costs** (insurance, tax, leasing) with their payments and contracts.
- **Receipts:** photos and PDFs attached to every entry; a picture and details (plate, VIN, price) per car.
- **Units:** metric, US or UK per account. Stored and sent by the API in km and liters.
- **Statistics:** totals, averages and the consumption over time, per car and for all cars.
- **Two looks:** road signs in the light theme, an instrument cluster in the dark one; five accent colors. The main
  pages follow WCAG 2.1 AA.
- **Modifications:** the parts of a tuned car with their approval (ABE, Teilegutachten, Einzelabnahme, …), conditions
  and documents; removed parts stay in the history.
- **HU:** the Hauptuntersuchung with result, defects and report; it sets the next HU reminder.
- **Papers as PDF:** modifications, approvals and HUs with their documents attached, for a roadside check or for selling.

**On the road**

- **Installable app** with a home screen shortcut to a new refuel of the last car.
- **Offline:** refuels, charges and trips are sent when the connection is back; the documents of the modifications can
  be kept on the device.
- **Trip log (Fahrtenbuch):** trips with the odometer, deleted ones stay with a reason, export as PDF or CSV.
- **GPS tracking:** a tracker in the car (e.g. an ESP32) sends its points. The map colors the tracks by speed, g-force
  or frequency, names frequent stops and suggests forgotten trips; optionally snapped to the roads (Valhalla). The
  browser flashes the tracker's firmware, see [docs/GPS_TRACKER.md](docs/GPS_TRACKER.md).
- **Reminders and notifications** in the app, by e-mail and as push messages.

**Data and integration**

- **Sharing a car** with other accounts as editor, viewer or driver, optionally for a period.
- **CSV import and export** per entry type, and all expenses of a car in one CSV.
- **Backup:** each user downloads all of their data as one ZIP and restores it into an empty account, also on another
  installation.
- **Administration:** usage statistics, accounts, car limits and the users' requests for more cars.
- **Service accounts** use the API with tokens of the identity provider, see
  [Service accounts](docs/DEPLOYMENT.md#service-accounts).
- **Operations:** Prometheus metrics and nightly database backups (volume and S3), see
  [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md#metrics).

## Installation

The Helm charts are published at `oci://ghcr.io/3thr3n/car-ledger/<chart>`, the details are in
[docs/DEPLOYMENT.md](docs/DEPLOYMENT.md).

### Requirements

- **Kubernetes** with **Helm 3.8+** (OCI charts).
- **An ingress controller or the Gateway API** with **HTTPS** (the login needs it): cert-manager via
  `ingress.certManager`, or your own TLS secret.
- **DNS** for `car-ledger.<host>`, and `keycloak.<host>` or `authentik.<host>` if the charts deploy one (a wildcard
  record is simplest).
- **A default StorageClass** for the PostgreSQL volumes (8 GiB for the app by default).
- **Memory:** the app requests 256 MiB (limit 768 MiB), its PostgreSQL 256 MiB, plus Infinispan; Keycloak or authentik
  add about 1 GiB each.
- **Optional:** an SMTP server for e-mails, a MapLibre style for the map of the GPS tracks.

### Charts

| Chart        | Deploys                                                                                                   | Docs                                         |
|--------------|-----------------------------------------------------------------------------------------------------------|----------------------------------------------|
| `stack`      | Everything below in one release, the identity provider switched on with one value, all secrets set up      | [README](helm/stack/README.md)               |
| `car-ledger` | The app, its PostgreSQL and Infinispan (the login sessions, shared by all replicas)                       | [values](helm/car-ledger/values.yaml)        |
| `keycloak`   | Keycloak with its PostgreSQL, the realm of CarLedger imported                                             | [DEPLOYMENT.md](docs/DEPLOYMENT.md#fresh-install-with-keycloak) |
| `authentik`  | authentik with its PostgreSQL, CarLedger set up as application                                            | [README](helm/authentik/README.md)           |
| `map-matching` | Valhalla, snaps the GPS tracks to the roads (optional)                                                  | [GPS_TRACKER.md](docs/GPS_TRACKER.md#map-matching) |

Use the `stack` for a new installation; the single charts for an existing identity provider, separate namespaces or
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

Name the release `car-ledger`, once per namespace (the resource names are fixed). Then:

1. **Log in to the identity provider** as administrator: authentik as `akadmin`
   (`kubectl get secret authentik-bootstrap -o jsonpath='{.data.password}' | base64 -d`), Keycloak as `car-ledger`
   (secret `car-ledger-keycloak-admin`).
2. **Add the users:** in authentik to the group `user` (or `admin`); in Keycloak in the realm `car-ledger`, they get the
   role `user`, administrators get the realm role `admin` as well.
3. **Open `https://car-ledger.<host>`** and log in. Administrators see *Administration* in the sidebar; new accounts may
   have one car, the limit is changed there.

For the Gateway API instead of an ingress, see [the stack README](helm/stack/README.md#values).

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

Or as a dependency of your own chart:

```yaml
dependencies:
  - name: car-ledger
    version: "<release>"
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

A [release](https://github.com/3thr3n/car-ledger/releases) (e.g. `0.11.0`) publishes the image
`ghcr.io/3thr3n/car-ledger:0.11.0` and the charts `car-ledger` and `stack` in the same version, which run that image.
`keycloak` and `authentik` have their own versions, the release notes name them. Without `--version`, Helm installs the
latest release.

Every build of `develop` is a prerelease `<next release>-dev.<run number>`, installed with `--devel` (a version range
needs a prerelease suffix, e.g. `>=1.0.0-0`). Leave `carLedger.image.tag` empty: each chart runs the image of its own
build.

What changed is in [CHANGELOG.md](CHANGELOG.md) (from 1.0.0 on), what stays stable within 1.x in
[docs/STABILITY.md](docs/STABILITY.md).

### Upgrading

```bash
kubectl exec car-ledger-postgresql-0 -- pg_dump -U postgres car-ledger > car-ledger-$(date +%F).sql
helm upgrade car-ledger oci://ghcr.io/3thr3n/car-ledger/stack -f my-values.yaml
```

The single chart is upgraded the same way. Pass your values file, not `--reuse-values`, and back up first: the
migrations can't be undone. Older installations: [upgrade steps](docs/DEPLOYMENT.md#upgrading-to-the-current-version).

### Uninstalling

```bash
helm uninstall car-ledger
```

The PostgreSQL volumes stay (`kubectl get pvc`); delete them to remove the data. To reinstall on them, keep the secret
`car-ledger-postgresql` and apply it before the install, otherwise the password doesn't match.

## CSV Format

Each entry type is imported and exported as CSV. The import assigns the columns of the file to the fields; dates are
`YYYY-MM-DD`, `DD.MM.YYYY` or `MM/DD/YYYY`, values in the units chosen for the file (metric by default).
`pricePerUnit` is in cent per liter or kWh (US: currency per gallon). Entries that exist already are skipped.

| Type          | Required                      | Optional                                         |
|---------------|-------------------------------|--------------------------------------------------|
| Refuel        | `date`, `unit`, `pricePerUnit` | `total`, `vatRate`, `distance`, `estimateConsumption` |
| Charge        | `date`, `unit`, `pricePerUnit` | `total`, `vatRate`, `distance`, `location` (`HOME`/`PUBLIC`), `stateOfCharge` |
| Maintenance   | `date`, `total`, `workshop`    | `vatRate`, `odometer`, `laborCost`, `partsCost`, `description` |
| Miscellaneous | `date`, `total`               | `vatRate`, `description`                         |

```csv
date,unit,pricePerUnit,distance,estimateConsumption
2024-10-09,40.03,171.9,551.5,8.0
18.10.2024,38.23,171.9,494.2,8.2
```

An export uses these field names as header (km, liters, ISO dates), so it can be imported again. The expenses of a car,
including the recurring costs, export as one CSV for spreadsheets, which can't be imported.

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

# end-to-end tests, start Quarkus (dev services, needs Docker) and Vite or reuse running ones;
# each spec resets the accounts first, so one runs alone too: yarn test:e2e e2e/trips.spec.ts
cd src/main/webapp && npx playwright install chromium && yarn test:e2e

# charts (after helm dependency update): template tests (helm-unittest), schemas (kubeconform)
helm unittest --with-subchart=false helm/car-ledger helm/keycloak helm/authentik helm/map-matching helm/stack
helm/validate-manifests.sh
```

The build writes the OpenAPI spec to `src/main/webapp/public/openapi.json`, `yarn openapi-ts` generates the client
from it. Commit both; the CI fails if they are outdated.

### Architecture

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md). `./mvnw javadoc:javadoc` builds the Javadoc into
`target/reports/apidocs`; the CI fails on broken Javadoc.

### Local cluster

`hack/kind/up.sh` creates a [kind](https://kind.sigs.k8s.io) cluster and installs the stack from the working copy. It
needs Docker, kind, kubectl, helm and the ports 80 and 443.

```bash
hack/kind/up.sh                    # Keycloak, Cilium, local-path, builds the image of the working copy
hack/kind/up.sh --idp authentik --ingress nginx --no-build
node hack/kind/login.cjs keycloak alice alice   # logs in with a browser, the user has to exist
hack/kind/down.sh
```

The app is https://car-ledger.localhost, the CA is in `target/kind/ca.crt`. Running `up.sh` again upgrades the stack.

## Contributing

Contributions are welcome: branch from `develop` and open the pull request against `develop`.

### Branches and releases

- `develop` gets all pull requests, its builds are published as prereleases (`<next release>-dev.<run number>`).
- Every milestone ends with a security review (issue template *Security review of a milestone*); its findings are
  fixed before the release or moved on purpose.
- Every pull request with a notable change adds it under **Unreleased** in [CHANGELOG.md](CHANGELOG.md); a release
  names that section with its version and date.
- `main` only holds released states. A release is a pull request from `develop` to `main`, merged with a merge
  commit (not squashed, so both branches keep the same history), then the tag of the version in
  `helm/car-ledger/Chart.yaml` on `main`; the release job adds the changelog entry to the GitHub release. Afterwards
  the next version is set in `Chart.yaml` on `develop`.
- A hotfix branches from `main` and goes to `main` like a release, and to `develop` as a cherry-pick in its own pull
  request (`develop` keeps a linear history).

## License

Copyright (C) 2024–2026 3thr3n

CarLedger is free software: you can redistribute it and/or modify it under the terms of the
[GNU Affero General Public License version 3](LICENSE) (AGPL-3.0-only) as published by the Free Software Foundation.
It is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the license for more details.

In short: you can use, host and change CarLedger freely, also in a company. If you run a **modified** version for other
users over a network, you have to offer them its source code (section 13 of the license). Releases up to 0.5.x were
published under the MIT License and stay available under it.
