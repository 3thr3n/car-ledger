# Architecture

How the parts of CarLedger fit together. Each package of the backend has an overview in its `package-info.java`:

```bash
./mvnw javadoc:javadoc    # target/reports/apidocs/index.html, private members included
```

Changes to the API, the backup, the migrations, the chart values or the device API follow [STABILITY.md](STABILITY.md).

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
  the backend (`yarn openapi-ts`).
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

Every instance runs them, so each job must be safe to run in parallel (conditional updates, `FOR UPDATE SKIP LOCKED`,
unique keys).

## Notifications

The services call `notification.Notifications`, which picks the recipients and renders the texts (Qute templates in
`src/main/resources/templates`) in the transaction. Mails and push messages (`notification.push`, Web Push with the
JDK) are sent after the commit. Every notification is also stored in the inbox of the account (`notification.inbox`).

## GPS trackers

A tracker sends its points to `/api/device/points` with its own token (`tracker.DeviceAuthenticationMechanism`). The
`tracker` package evaluates the points of a car in the order of time: the simplified track for the map, the drives
(`DriveDetector`), which become suggested trips and places, and the frequency of the roads. See
[GPS_TRACKER.md](GPS_TRACKER.md).

The geometry of the map (`SharedLine`, `TrackSimplifier`, `DriveDetector`, `Selection`) is plain Java without
Quarkus or the database; it stays in the app because it reads the same points with the same access rules.

## Offline

The frontend keeps the account, the cars and the open trip on the device and queues refuels, charges and trips made
without a connection (`src/main/webapp/src/offline`). Each queued request carries an `Idempotency-Key`, which the
backend stores with the entry.

## Accessibility

The app aims at WCAG 2.1 AA (#543), checked by `e2e/accessibility.spec.ts`: axe on the main pages in light and dark
mode and with every accent color, the keyboard and 320 px width. Rules:

- **Headings**: the title of a page is the `h1` (`PageHeader`); the theme renders `h5`/`h6` as `h2` and
  `subtitle1`/`subtitle2` as `h3`. A group directly under the title sets `component="h2"`, a text in a heading variant
  that isn't one sets `component="p"` or `"div"`.
- **Colors**: a category color as text or icon on its tint goes through `categoryInk` (the darker shade in light mode);
  the accent colors and the warning and info colors of the theme have 4.5:1 for white and as text.
- **Names**: icon buttons and the columns of buttons (`visuallyHidden`) are named, a chart has its values as a
  `ChartTable` for screen readers, the map is a named region next to the list of its drives.
- **Language**: `<html lang>` follows the language of the account.
