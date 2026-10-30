# Architecture

An overview for contributors: how the parts of CarLedger fit together. The details are in the Javadoc of the backend,
each package starts with an overview (`package-info.java`):

```bash
./mvnw javadoc:javadoc    # target/reports/apidocs/index.html, private members included
```

A change of the API, the backup, the migrations, the values of the charts or the device API has to keep the rules of
[STABILITY.md](STABILITY.md).

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
| `notification.inbox.InboxCleanup`           | 03:15 (configurable)    | deletes the notifications of the inboxes after 90 days       |
| `tracker.TrackRetention`                    | 02:30                   | deletes the track points older than the car keeps them       |
| `attachment.AttachmentCleanup`              | every minute, and 02:45 | deletes the content of removed files outside the database    |
| `attachment.AttachmentMover`                | hourly, when configured | moves the existing files into the configured storage         |

Every instance runs them, so they are safe to run in parallel: a due reminder is claimed by a conditional update,
the files with `FOR UPDATE SKIP LOCKED`, a payment is unique per due date, and deleting old points twice does no harm.

## Notifications

The services call `notification.Notifications`, which picks the recipients and renders the texts (Qute templates in
`src/main/resources/templates`) in the transaction; the mails and push messages are sent after the commit, so a failed
change sends nothing and a failing mail server never fails a request. Push is Web Push with the means of the JDK
(`notification.push`). Every notification also goes into the inbox of the account (`notification.inbox`, the page
*Notifications*), stored with the change in its transaction, also without mail and push; the settings choose what
comes by mail and push as well.

## GPS trackers

A tracker sends its points to `/api/device/points` with its own token (`tracker.DeviceAuthenticationMechanism`). The
`tracker` package evaluates the points of a car in the order of time: the simplified track for the map, the drives
(`DriveDetector`), which become suggested trips and places, and how often the car drove a road (the streets of the frequency). GPS
never writes a trip itself. See [GPS_TRACKER.md](GPS_TRACKER.md).

### The geometry of the map stays in the app

The geometry of the map (`SharedLine`, `TrackSimplifier`, the streets of the frequency, `DriveDetector`, `Selection`)
is plain Java: points in, lines out, no Quarkus and no database. It isn't a service of its own, on purpose:

- it reads the same `track_point` rows, through the same access to a car (`CarAccess`), and follows the retention and
  the deletion of the car; the drives feed the suggested trips and the places as well. A service would need the
  database shared or a copy of the points with its own access, deletion and backup;
- it isn't heavy: a busy month takes about 0.5 s per request (#533), nothing to scale apart for an instance of a few
  cars;
- every service is another image, deployment and upgrade for each operator of a self-hosted install;
- the same language, data and releases.

What is a product of its own stays outside, optional, as the map tiles today (`carledger.map.style-url`): map matching
with OpenStreetMap data (#504, e.g. OSRM or Valhalla) would deliver the geometry of the streets of the frequency. The
classes above stay free of the framework, so they could move out later. Worth a new look when requests on real data
take more than 1–2 s regularly, the map needs work in the background, CarLedger is hosted for many users, or map
matching becomes the standard.

## Offline

The frontend keeps the account, the cars and the open trip on the device and queues refuels, charges and trips made
without a connection (`src/main/webapp/src/offline`). Each queued request carries an `Idempotency-Key`, so one sent
again after a lost answer doesn't create the entry twice; the backend stores the key with the entry.
