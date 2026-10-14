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
- **CSV Import:** Import multiple receipts at once using a CSV file, making it quick to upload historical or bulk data.
- **CSV Export:** Export the receipts of a type as CSV, in the format of the import.
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

## Tests

```bash
# backend
./mvnw verify

# frontend unit tests
cd src/main/webapp && yarn test

# end-to-end tests, start Quarkus (with dev services, needs Docker) and Vite themselves,
# running servers are reused
cd src/main/webapp && npx playwright install chromium && yarn test:e2e
```

The build writes the OpenAPI spec of the backend to `src/main/webapp/public/openapi.json`, the frontend client is
generated from it with `yarn openapi-ts`. Commit both, the CI fails if they are outdated.

## Contributing

Contributions are welcome! Please follow these steps to contribute:

1. Fork the repository.
2. Create a new branch (`git checkout -b feature/YourFeature`).
3. Commit your changes (`git commit -m 'Add YourFeature'`).
4. Push to the branch (`git push origin feature/YourFeature`).
5. Open a Pull Request.

## Helm

For using the helm chart, you can add this as a dependency and update the values as needed.

```yaml
dependencies:
  - name: helm
    version: "0.3.x"
    repository: oci://ghcr.io/3thr3n/car-ledger
```

Every [release](https://github.com/3thr3n/car-ledger/releases) publishes the chart and the image in its version (e.g.
`0.3.0`), the release notes name both. Builds of `main` are published as prereleases (`0.4.0-dev.<run number>`, chart
and image of the same build), a range takes them only with a prerelease suffix (e.g. `>=0.4.0-0`).

The login uses OpenID Connect: `helm/keycloak` deploys Keycloak, `helm/authentik` deploys authentik with CarLedger
already set up, any other provider works as well.

See [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md) for choosing the identity provider, a fresh install (including the Keycloak
realm and the secret the app needs), the checklist for upgrading and the upgrade from the bitnami based charts.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
