# Architecture

An overview for contributors: how the parts of CarLedger fit together. The details are in the Javadoc of the backend,
each package starts with an overview (`package-info.java`):

```bash
./mvnw javadoc:javadoc    # target/reports/apidocs/index.html, private members included
```

## The parts

```
Browser: React app (src/main/webapp), service worker, offline queue in IndexedDB
   │  /api, session cookie                       GPS tracker: /api/device, own token
   ▼                                             │
Quarkus app ◄────────────────────────────────────┘
   ├── rest           resources, records, OpenAPI spec       → contract with the frontend
   ├── services       TripService, attachment, tracker, ...  → logic and transactions
   ├── data.repository  queries, CarAccess                   → who may do what with a car
   ▼
PostgreSQL (Flyway)   Infinispan (sessions)   S3 (optional, files)   SMTP / push services (notifications)
```

- **Backend**: Quarkus with REST (Jackson), Hibernate ORM with Panache, Flyway, OIDC, the scheduler and the mailer. In
  production the frontend is served by the same app (Quinoa builds it into the jar).
- **Frontend**: React with MUI, TanStack Router and Query, i18next. Its API client is generated from the OpenAPI spec of
  the backend (`yarn openapi-ts`), so the records and `@Operation`s of `rest` are the contract.
- **Identity provider**: Keycloak or authentik (OpenID Connect). The tokens of a login are kept in Infinispan
  (`state`), the cookie holds only a key; scripts call the API with tokens of service accounts.

## Layers of the backend

1. **REST** (`rest` and its subpackages): checks the input, calls a service, converts entities into records. A
   resource never returns an entity. `RestHandler` turns the exceptions into answers (404, 409, 400 with the messages).
2. **Services** (the root package and the packages of a topic): the logic and the transactions, e.g. `TripService`,
   `CarAccessService`, `attachment.AttachmentService`, `tracker.DeviceService`, `backup.BackupService`.
3. **Repositories** (`data.repository`): the queries, as Panache repositories over the entities of `data`.

## Access to a car

A car belongs to its owner; other accounts get a role (editor, viewer, driver) through an accepted invitation, optionally
for a period. **`data.repository.CarAccess` is the one place that decides what a user may do with a car**: every
query over a car or its entries takes the user name and an action (`USE`, `DRIVE`, `READ`, `WRITE`, `MANAGE`) and adds
the condition of the roles and periods. A car the user may not use is not found (404). A driver sees only the own trips
and refuels. The methods named `Owned` (car limit, backup) are about the owner only.

## Data

- **PostgreSQL** with the Flyway migrations of `src/main/resources/db/migration`. A few rules live in the database:
  triggers keep the trips append-only and queue the files of deleted attachments for removal.
- **Files** (`attachment`): the metadata in PostgreSQL, the content in the database or an S3 bucket
  (`AttachmentStorages`). Each attachment names its storage, so switching keeps the old files readable.
- **Track points** of the GPS trackers: millions per car, written and read with JDBC and `COPY`
  (`TrackPointRepository`), not as entities.

## Scheduled jobs

| Job                                         | When                    | What                                                         |
|---------------------------------------------|-------------------------|--------------------------------------------------------------|
| `ScheduledPayments`                         | 01:00                   | the payments of the recurring costs that became due          |
| `notification.ReminderNotifications`        | 07:00 (configurable)    | one notification per account with its reminders due soon     |
| `tracker.TrackRetention`                    | 02:30                   | deletes the track points older than the car keeps them       |
| `attachment.AttachmentCleanup`              | every minute, and 02:45 | deletes the content of removed files outside the database    |
| `attachment.AttachmentMover`                | hourly, when configured | moves the existing files into the configured storage         |

Every instance runs them, so they are safe to run in parallel: a due reminder is claimed by a conditional update,
the files with `FOR UPDATE SKIP LOCKED`, a payment is unique per due date, and deleting old points twice does no harm.

## Notifications

The services call `notification.Notifications`, which picks the recipients and renders the texts (Qute templates in
`src/main/resources/templates`) in the transaction; the mails and push messages are sent after the commit, so a failed
change sends nothing and a failing mail server never fails a request. Push is Web Push with the means of the JDK
(`notification.push`).

## GPS trackers

A tracker sends its points to `/api/device/points` with its own token (`tracker.DeviceAuthenticationMechanism`). The
`tracker` package evaluates the points of a car in the order of time: the simplified track for the map, the drives
(`DriveDetector`), which become suggested trips and places, and how often the car drove a road (`PassCounter`). GPS
never writes a trip itself. See [GPS_TRACKER.md](GPS_TRACKER.md).

## Offline

The frontend keeps the account, the cars and the open trip on the device and queues refuels, charges and trips made
without a connection (`src/main/webapp/src/offline`). Each queued request carries an `Idempotency-Key`, so one sent
again after a lost answer doesn't create the entry twice; the backend stores the key with the entry.
