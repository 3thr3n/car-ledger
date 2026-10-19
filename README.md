# CarLedger

**CarLedger** is an app designed to help you track and manage your fuel expenses by saving and organizing
refueling receipts. With this app, you can not only add and delete receipts but also import them in bulk through CSV
files. CarLedger offers data aggregation and summarization features, giving you insights into your fuel
spending and usage patterns.

## Table of Contents

- [Features](#features)
- [Getting Started](#getting-started)
- [Installation](#installation)
- [Usage](#usage)
    - [Adding Receipts](#adding-receipts)
    - [Deleting Receipts](#deleting-receipts)
    - [Importing Receipts via CSV](#importing-receipts-via-csv)
    - [Exporting Receipts as CSV](#exporting-receipts-as-csv)
    - [Data Aggregation & Summarization](#data-aggregation--summarization)
- [CSV Format](#csv-format)
- [Tests](#tests)
- [Contributing](#contributing)
- [Helm](#helm)
- [License](#license)

## Features

- **Add/Delete Receipts:** Easily add or delete individual refueling receipts to keep your records up-to-date.
- **Electric and Hybrid Cars:** Record the charges of electric cars and plug-in hybrids in kWh, with the consumption in
  kWh/100km next to the refuels in liters.
- **Units:** Show and enter the values in metric units, US units (miles, gallons, mpg) or UK units (miles, liters,
  mpg), set per account. They are stored in km and liters, the API always uses those.
- **Backup:** Every user downloads all of their data (settings, cars, entries, recurring costs, reminders, receipts
  and pictures) as one ZIP and restores it into an account without cars, e.g. on another installation.
- **Car Requests:** Users who reached their car limit request more cars in the app, administrators approve or reject
  them on the administration page.
- **Installable app:** On the phone, CarLedger can be added to the home screen and starts like an app; a long press
  on its icon offers *Add refuel*, the form of a new refuel (or charge) of the last opened car.
- **Notifications by e-mail and push:** Users get notified about the reminders of their cars that became due (once a
  day), administrators about new car requests, users about the decision. By e-mail and as push message on every device
  where it is turned on; each one can be turned off in the settings.
- **Webhooks:** Events like a new car request are sent as signed HTTP requests to other tools or your own scripts,
  see [docs/WEBHOOKS.md](docs/WEBHOOKS.md).
- **CSV Import:** Import multiple receipts at once using a CSV file, making it quick to upload historical or bulk data.
- **CSV Export:** Export the receipts of a type as CSV, in the format of the import, or all expenses of a car
  including the payments of the recurring costs in one CSV for spreadsheets.
- **Data Aggregation & Summarization:** Get insights on your fuel expenses, including:
    - Total money spent on fuel
    - Total fuel volume purchased
    - Average cost per refuel
    - Fuel economy trends and comparisons (e.g., monthly or yearly)

## Getting Started

To start using CarLedger, follow these steps:

1. **Clone the Repository:**
   ```bash
   git clone https://github.com/3thr3n/car-ledger.git
   cd car-ledger
   ```

### Backend Setup (Quarkus)

1. **Build the Quarkus application**:
   ```bash
   mvn clean install
   ```

2. **Run the application in development mode**:
   ```bash
   mvn quarkus:dev
   ```

   This will start the Quarkus server on the default port (usually `http://localhost:8080`).

### Frontend Setup

1. **Navigate to the frontend directory**:
   ```bash
   cd src/main/webapp
   ```

2. **Install dependencies using Yarn**:
   ```bash
   yarn install
   ```

3. **Start the frontend development server**:
   ```bash
   yarn dev
   ```

   By default, the frontend will run on `http://localhost:5173` and should be configured to communicate with the Quarkus
   backend.

## Usage

### Adding Receipts

- Manually add new receipts by entering fuel details, cost, and date.

### Editing and Deleting Receipts

- Correct receipts afterwards, calculated values like the consumption are updated.
- Remove individual receipts to keep your record accurate and clutter-free.
- End recurring costs (e.g. a cancelled insurance), payments from the end date on are removed.
- Delete a car together with all its receipts.

### Settings

- The default VAT country, the currency of the amounts and the language are stored with your account.

### Administration

- Users with the Keycloak role `admin` see the administration: usage statistics, all accounts with their cars and
  entries, the car limit per account and deleting accounts.

### Importing Receipts via CSV

- Use the CSV import feature to upload multiple receipts at once. Ensure your file follows the specified format below.

### Exporting Receipts as CSV

- The lists of refuels, maintenance and other costs export their entries as CSV, all of them or of the selected year.
- The file uses the field names of the import as header, ISO dates and a dot as decimal separator, so it can be
  imported again without assigning the columns. Entries that exist already are skipped by the import.
- The ledger on the overview of a car exports all expenses as one CSV (every entry and the payments of the recurring
  costs, oldest first), for spreadsheets; it cannot be imported again.

### Data Aggregation & Summarization

- View summaries of your data to understand spending trends, fuel efficiency, and other insights.

## CSV Format

When importing receipts via CSV, ensure your file follows this format:

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

## Tests

```bash
# backend
./mvnw verify

# frontend unit tests
cd src/main/webapp && yarn test

# end-to-end tests, start Quarkus (with dev services, needs Docker) and Vite themselves,
# running servers are reused
cd src/main/webapp && npx playwright install chromium && yarn test:e2e

# charts: the template tests of helm/<chart>/tests (plugin helm-unittest) and the rendered
# resources against the schemas of Kubernetes (kubeconform), after helm dependency update
helm unittest --with-subchart=false helm/car-ledger helm/keycloak helm/authentik helm/stack
helm/validate-manifests.sh
```

The build writes the OpenAPI spec of the backend to `src/main/webapp/public/openapi.json`, the frontend client is
generated from it with `yarn openapi-ts`. Commit both, the CI fails if they are outdated.

### Local cluster

`hack/kind/up.sh` creates a cluster in Docker with [kind](https://kind.sigs.k8s.io) and installs the stack chart from
the working copy: Cilium (also as ingress controller) or ingress-nginx, local-path or Longhorn, and cert-manager with a
self-signed CA, which nobody trusts, like in many company clusters. It needs Docker, kind, kubectl and helm and the
ports 80 and 443.

```bash
hack/kind/up.sh                    # Keycloak, Cilium, local-path, builds the image of the working copy
hack/kind/up.sh --idp authentik --ingress nginx --storage longhorn --no-build
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
- `main` only holds released states. A release is a pull request from `develop` to `main`, merged with a merge
  commit (not squashed, so both branches keep the same history), then the tag of the version in
  `helm/car-ledger/Chart.yaml` on `main`. Afterwards the next version is set in `Chart.yaml` on `develop`.
- A hotfix branches from `main` and goes to `main` like a release. `develop` keeps a linear history, so the fix comes
  to `develop` as a cherry-pick in a pull request of its own.

## Helm

For using the helm chart, you can add this as a dependency and update the values as needed.

```yaml
dependencies:
  - name: car-ledger
    version: "0.5.x"
    repository: oci://ghcr.io/3thr3n/car-ledger
```

Up to 0.3.x, the chart was named `helm` (`oci://ghcr.io/3thr3n/car-ledger/helm`), it is `car-ledger` since 0.4.0.

Every [release](https://github.com/3thr3n/car-ledger/releases) publishes the chart and the image in its version (e.g.
`0.5.0`), the release notes name both. Builds of `develop` are published as prereleases (`0.6.0-dev.<run number>`,
chart and image of the same build), a range takes them only with a prerelease suffix (e.g. `>=0.6.0-0`).

The login uses OpenID Connect: `helm/keycloak` deploys Keycloak with the realm of CarLedger, `helm/authentik` deploys authentik with CarLedger
already set up, any other provider works as well.
`helm/stack` deploys the app with any of them in one release and sets up the secrets between them, see
[its README](helm/stack/README.md). All charts are published at `oci://ghcr.io/3thr3n/car-ledger/<chart>`.

See [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md) for choosing the identity provider, a fresh install (including the Keycloak
realm and the secret the app needs), the checklist for upgrading and the upgrade from the bitnami based charts.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
