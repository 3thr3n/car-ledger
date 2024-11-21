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
    - [Data Aggregation & Summarization](#data-aggregation--summarization)
- [CSV Format](#csv-format)
- [Contributing](#contributing)
- [License](#license)

## Features

- **Add/Delete Receipts:** Easily add or delete individual refueling receipts to keep your records up-to-date.
- **CSV Import:** Import multiple receipts at once using a CSV file, making it quick to upload historical or bulk data.
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

### Deleting Receipts

- Remove individual receipts to keep your record accurate and clutter-free.

### Importing Receipts via CSV

- Use the CSV import feature to upload multiple receipts at once. Ensure your file follows the specified format below.

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

## Contributing

Contributions are welcome! Please follow these steps to contribute:

1. Fork the repository.
2. Create a new branch (`git checkout -b feature/YourFeature`).
3. Commit your changes (`git commit -m 'Add YourFeature'`).
4. Push to the branch (`git push origin feature/YourFeature`).
5. Open a Pull Request.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
