# Architecture

How the parts of CarLedger fit together. Each backend package has an overview in its `package-info.java`:

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

- **Backend**: Quarkus with REST (Jackson), Hibernate ORM with Panache, Flyway, OIDC, the scheduler and the mailer. The
  same app serves the frontend (Quinoa builds it into the jar).
- **Frontend**: React with MUI, TanStack Router and Query, i18next. Its API client is generated from the OpenAPI spec of
  the backend (`yarn openapi-ts`).
- **Identity provider**: Keycloak or authentik (OpenID Connect). The login tokens are kept in Infinispan (`state`), the
  cookie holds only a key. Scripts use tokens of service accounts.

## Layers of the backend

1. **REST** (`rest`): checks the input, calls a service, answers with records, never with entities. `RestHandler`
   turns the exceptions into answers (404, 409, 400 with the messages).
2. **Services** (the root package and the topic packages, e.g. `attachment`, `tracker`, `backup`): the logic and the
   transactions.
3. **Repositories** (`data.repository`): the queries, as Panache repositories over the entities of `data`.

## Access to a car

A car belongs to its owner; other accounts get a role (editor, viewer, driver) through an accepted invitation, optionally
for a period. **`data.repository.CarAccess` decides what a user may do with a car**: every query over a car or its
entries takes the user name and an action (`USE`, `DRIVE`, `READ`, `WRITE`, `MANAGE`). A car the user may not use is not
found (404); a driver sees only the own trips and refuels. Methods named `Owned` (car limit, backup) are about the owner
only.

## Data

- **PostgreSQL** with the Flyway migrations of `src/main/resources/db/migration`. Triggers keep the trips append-only
  and queue the files of deleted attachments for removal.
- **Files** (`attachment`): the metadata in PostgreSQL, the content in the database or S3 (`AttachmentStorages`); each
  attachment names its storage.
- **Track points**: millions per car, read and written with JDBC and `COPY` (`TrackPointRepository`), not as entities.

## Scheduled jobs

| Job                                         | When                    | What                                                         |
|---------------------------------------------|-------------------------|--------------------------------------------------------------|
| `ScheduledPayments`                         | 01:00                   | the payments of the recurring costs that became due          |
| `notification.ReminderNotifications`        | 07:00 (configurable)    | one notification per account with its reminders due soon     |
| `notification.inbox.InboxCleanup`           | 03:15 (configurable)    | deletes the notifications of the inboxes after 90 days       |
| `tracker.TrackRetention`                    | 02:30                   | deletes the track points older than the car keeps them       |
| `attachment.AttachmentCleanup`              | every minute, and 02:45 | deletes the content of removed files outside the database    |
| `attachment.AttachmentMover`                | hourly, when configured | moves the existing files into the configured storage         |
| `tracker.MapMatcher`                        | every 2 minutes, with map matching | snaps the points of finished drives to the roads  |

Every instance runs them: each job must be safe to run in parallel (conditional updates, `FOR UPDATE SKIP LOCKED`,
unique keys).

## Notifications

The services call `notification.Notifications`, which renders the texts (Qute templates in
`src/main/resources/templates`) in the transaction. Mails and push messages (`notification.push`) are sent after the
commit; every notification is also kept in the inbox of the account (`notification.inbox`).

## GPS trackers

A tracker sends its points to `/api/device/points` with its own token (`tracker.DeviceAuthenticationMechanism`). The
`tracker` package evaluates the points of a car in the order of time: the track for the map, the drives
(`DriveDetector`) for suggested trips and places, and how often each road was driven. See
[GPS_TRACKER.md](GPS_TRACKER.md).

## Offline

The frontend keeps the account, the cars and the open trip on the device and queues refuels, charges and trips made
without a connection (`src/main/webapp/src/offline`). A request sent again answers like the first time: bills by their
`Idempotency-Key`, trips by their id created on the device.

## Accessibility

The app aims at WCAG 2.1 AA, checked by `e2e/accessibility.spec.ts` (axe in both themes and every accent color, the
keyboard, 320 px width). Rules:

- **Headings**: the title of a page is the `h1` (`PageHeader`); the theme renders `h5`/`h6` as `h2` and
  `subtitle1`/`subtitle2` as `h3`. A group directly under the title sets `component="h2"`, a text in a heading variant
  that isn't one sets `component="p"` or `"div"`.
- **Colors**: a category color as text or icon on its tint goes through `categoryInk` (the darker shade in light mode);
  the accent colors and the warning and info colors of the theme have 4.5:1 for white and as text.
- **Names**: icon buttons and the columns of buttons (`visuallyHidden`) are named, a chart has its values as a
  `ChartTable` for screen readers, the map is a named region next to the list of its drives.
- **Language**: `<html lang>` follows the language of the account.
