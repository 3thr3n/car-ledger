# Deployment

CarLedger runs on Kubernetes and needs an OpenID Connect provider for the login.

| Chart               | Deploys                                                                                                                                  |
|---------------------|------------------------------------------------------------------------------------------------------------------------------------------|
| `helm/stack`        | All charts below in one release, see [its README](../helm/stack/README.md)                                                                |
| `helm/car-ledger`   | The app, its PostgreSQL and Infinispan (login sessions, shared by all replicas)                                                          |
| `helm/keycloak`     | Keycloak 26.8 (via the [keycloakx chart](https://github.com/codecentric/helm-charts/tree/master/charts/keycloakx)) with its PostgreSQL  |
| `helm/authentik`    | authentik 2026.8 with its PostgreSQL, see [its README](../helm/authentik/README.md)                                                      |
| `helm/map-matching` | Valhalla for snapping the GPS tracks to the roads, see [GPS_TRACKER.md](GPS_TRACKER.md#map-matching)                                      |

The charts are published at `oci://ghcr.io/3thr3n/car-ledger/<chart>`. From the repository, run
`helm dependency update helm/<chart>` before the install.

- [Everything in one release](#everything-in-one-release)
- [Identity provider](#identity-provider)
- [Fresh install with Keycloak](#fresh-install-with-keycloak)
- [Options](#options): [E-mail](#e-mail), [Push](#push), [Service accounts](#service-accounts),
  [Attachments in S3](#attachments-in-s3), [Self-signed certificates](#self-signed-certificates),
  [Database backups](#database-backups), [Metrics](#metrics), [Legal pages](#legal-pages)
- [Upgrading to the current version](#upgrading-to-the-current-version)
- [Upgrading from the bitnami based charts (0.1.x)](#upgrading-from-the-bitnami-based-charts-01x)
- [Operations](#operations)

## Everything in one release

```bash
helm install car-ledger oci://ghcr.io/3thr3n/car-ledger/stack \
  --set global.host=example.com \
  --set global.authentik.enabled=true
```

See [the stack README](../helm/stack/README.md). The sections below install the charts one by one.

## Identity provider

- **Keycloak** (`helm/keycloak`): the default of the car-ledger chart, see [below](#fresh-install-with-keycloak).
- **authentik** (`helm/authentik`): see [its README](../helm/authentik/README.md).
- **Any other provider**. It needs:

  | Requirement              | Details                                                                                                  |
  |--------------------------|----------------------------------------------------------------------------------------------------------|
  | Confidential client      | Authorization code flow with PKCE (S256) and refresh tokens                                              |
  | Redirect URIs            | `https://car-ledger.<host>/api/auth/callback`, post logout `https://car-ledger.<host>/api/auth/logout-callback` |
  | Roles `user` and `admin` | In the claim `groups` (or `realm_access.roles`) of the tokens, every user needs one of them              |
  | User name                | Claim `preferred_username` (or `upn`, else `sub`); the account of a user is bound to it                  |
  | Scopes                   | `offline_access`, `email`, `profile` by default (`carLedger.auth.scopes`)                                |
  | Signed tokens            | Verifiable with the published keys (`jwks_uri`), e.g. RS256                                              |
  | Logout (optional)        | `end_session_endpoint`, otherwise only the session of CarLedger ends                                     |

  Set `carLedger.auth.url` (the issuer), `carLedger.auth.clientId` and `carLedger.auth.existingSecret`, a secret with
  the client secret and a state secret (see [step 3](#3-secret-for-the-app)).

With Keycloak or authentik in another namespace than the app, set `carLedger.namespace` of their chart, so the secret
is created in the namespace of the app.

Users keep their data on another provider when their user name stays the same.

## Fresh install with Keycloak

### 1. Keycloak

```bash
helm install keycloak oci://ghcr.io/3thr3n/car-ledger/keycloak \
  --set keycloak.hostname=keycloak.example.com \
  --set carLedger.hostname=car-ledger.example.com

# password of the admin user car-ledger
kubectl get secret keycloak-admin -o jsonpath='{.data.password}' | base64 -d
```

The first start can take a few minutes.

### 2. Realm and client

With `carLedger.hostname`, Keycloak imports the realm `car-ledger` on its first start, with the client `carLedger` and
the realm roles `user` (default role) and `admin`. An existing realm is not changed. Create the users in the realm
`car-ledger` and give the administrators the role `admin`.

Without `carLedger.hostname`, set the realm up by hand:

1. Realm `car-ledger` (another name: set `carLedger.auth.url` of the car-ledger chart).
2. Client `carLedger` (OpenID Connect):
    - Client authentication on, standard flow only
    - Valid redirect URI: `https://car-ledger.example.com/api/auth/callback`
    - Valid post logout redirect URI: `https://car-ledger.example.com/api/auth/logout-callback`
    - Advanced → PKCE method: `S256`
    - Client scopes → `carLedger-dedicated` → *Configure a new mapper* → *User Realm Role*: token claim name
      `realm_access.roles`, multivalued, *Add to ID token* on (the app reads the roles from the ID token)
    - Copy the client secret from the *Credentials* tab
3. Realm roles `user` and `admin`, `user` in the default roles of the realm.

### 3. Secret for the app

The app reads the client secret and the state secret from the secret `keycloak` (`carLedger.auth.existingSecret`).
With `carLedger.hostname`, the keycloak chart generates it. Without, create it:

```bash
kubectl create secret generic keycloak \
  --from-literal=secret='<client secret of carLedger>' \
  --from-literal=state-secret="$(openssl rand -hex 32)"
```

The state secret needs at least 16 characters.

### 4. CarLedger

```bash
helm install car-ledger oci://ghcr.io/3thr3n/car-ledger/car-ledger --set ingress.host=example.com
```

Versions and dev builds: see [Versions](../README.md#versions). With another release name than `car-ledger`, set
`carLedger.postgres.host` to `<release>-postgresql`.

Log in with a user with the role `admin`: the sidebar shows *Administration*. A 403 for every user means the roles are
missing in the tokens.

### Options

- **Gateway API instead of an ingress**: `ingress.enabled=false`, `httpRoute.enabled=true` and `httpRoute.parentRefs`
  for the app; `keycloak.ingress.enabled=false`, `keycloak.httpRoute.enabled=true` and `keycloak.httpRoute.hostnames`
  for Keycloak.
- **Infinispan 16**: needs a CPU with x86-64-v3, so the charts use 15.2. Set
  `infinispan.images.server=quay.io/infinispan/server:16.2` if your nodes support it.
- **External PostgreSQL**: `carledgerpostgres.enabled=false` and `carLedger.postgres.*` (or `carLedger.postgres.jdbc`).
- **Attachments**: stored in PostgreSQL or [S3](#attachments-in-s3), at most `carLedger.attachments.maxSize` (10 MB)
  per file and `carLedger.attachments.maxPerBill` (10) per entry. Uploads need requests up to
  `carLedger.attachments.maxRequestSize` (12 MB): the nginx ingress gets it automatically, other proxies need the same
  limit, otherwise uploads fail with 413.
- **Backups of the users**: the restore is an upload and limited by `carLedger.attachments.maxRequestSize`. GPS tracks
  kept forever take about 14 MB per year, raise the limit for such backups. A backup or restore may take
  `carLedger.backup.transactionTimeout` seconds (1800).
- **Network policies** (`networkPolicy.enabled`, on in all charts): only the app reaches its PostgreSQL and
  Infinispan, only Keycloak and authentik their PostgreSQL. Further sources: `networkPolicy.extraFrom`. The management
  port of Keycloak (9000) opens with `networkPolicy.managementFrom` of the keycloak chart.
- **Caching proxy or CDN**: `index.html`, `/sw.js` and `/manifest.json` are sent with `Cache-Control: no-cache`, keep
  that, otherwise new versions arrive late. Only `/assets/` may be cached.

### E-mail

CarLedger mails the administrators about new car requests, the user about the decision, and every user once a day
about due reminders.

```yaml
carLedger:
  mail:
    host: smtp.example.com
    port: 587                 # STARTTLS, or 465 with tls: true
    from: "CarLedger <car-ledger@example.com>"
    username: car-ledger@example.com
    existingSecret: car-ledger-mail   # key "password"
```

- **Recipients**: the claim `email` of the last login. Administrators are the accounts whose last login had the role
  `admin`.
- **Reminders**: at 07:00 UTC (`carLedger.mail.remindersCron`, Quartz cron, `off` for never), due within 30 days or
  1,000 km, or overdue.
- **Links**: to `carLedger.mail.appUrl`, by default `https://car-ledger.<ingress.host>`.
- Users turn the mails off in *Settings → Notifications by e-mail*. Without a host nothing is sent, in dev mode the
  mails go to the log.

### Push

The same notifications as push messages, for every device where the user turns on *Settings → Notifications → Push on
this device*. On by default (`carLedger.push.enabled`).

- The chart generates the key of the server into the secret `car-ledger-push`. A new key ends all subscriptions. An own
  key: `carLedger.push.existingSecret` with the key `private-key`, e.g.
  `openssl ecparam -name prime256v1 -genkey -noout | kubectl create secret generic my-push --from-file=private-key=/dev/stdin`.
- The app needs outgoing HTTPS to the push services of the browsers (`carLedger.push.allowedHosts`).
  `carLedger.push.subject` is the contact they see, by default `https://car-ledger.<ingress.host>`.
- On iPhone and iPad only the installed app receives push messages.

### Service accounts

A script calls the API with an access token of its own OpenID Connect client. In Keycloak, in the realm `car-ledger`:

1. **Clients → Create client**, e.g. `car-ledger-bot`, OpenID Connect.
2. *Client authentication* on, *Standard flow* and *Direct access grants* off, *Service accounts roles* on.
3. **Service accounts roles** tab → *Assign role* → realm role `admin`.
4. **Client scopes** tab → `car-ledger-bot-dedicated` → *Configure a new mapper* → **Audience**, *Included Client
   Audience* `carLedger`, *Add to access token* on. CarLedger rejects tokens without its audience
   (`carLedger.auth.audience`, `any` turns the check off).
5. Copy the client secret from the **Credentials** tab.

```bash
TOKEN=$(curl -s https://keycloak.example.com/realms/car-ledger/protocol/openid-connect/token \
  -d grant_type=client_credentials -d client_id=car-ledger-bot -d client_secret="$SECRET" | jq -r .access_token)
curl -H "Authorization: Bearer $TOKEN" "https://car-ledger.example.com/api/admin/car-requests?state=OPEN"
```

Car requests are decided with `POST /api/admin/car-requests/{id}/approve` (optional body `{ "maxCars": 2 }`) or
`POST /api/admin/car-requests/{id}/reject` (optional body `{ "comment": "..." }`).

In authentik: the `client_credentials` grant of the provider *CarLedger*, with a service account in the group `admin`
and an app password, see the
[authentik documentation](https://docs.goauthentik.io/add-secure-apps/providers/oauth2/client_credentials/).

### Attachments in S3

By default the files are stored in PostgreSQL. To keep them in an S3 compatible bucket:

```yaml
carLedger:
  attachments:
    storage: S3
    s3:
      bucket: car-ledger                # must exist, the files go below attachments/
      endpoint: https://s3.example.com  # empty for AWS
      region: us-east-1
      pathStyle: true                   # needed by most storages besides AWS
      existingSecret: car-ledger-s3     # keys accessKeyId and secretAccessKey
```

Without `existingSecret` the default AWS credentials are used (e.g. IRSA). The app needs to read, write, delete and
list below `attachments/`.

- **Switching**: files of the other storage stay readable. `carLedger.attachments.moveExisting=true` moves them into
  the configured storage in the background; *Administration* shows the files per storage. Back to PostgreSQL works the
  same way (`storage: DB`, keep the bucket set until all files are moved).
- **Backups**: a dump of PostgreSQL doesn't contain the files in S3, back up the bucket as well.

### Self-signed certificates

The app makes HTTPS requests to the identity provider, the SMTP server and the push services. Besides the public CAs
it trusts:

- **The CA of the ingresses** (`carLedger.trust.ingressCA`, on by default): the `ca.crt` of the TLS secrets of the app
  and of the identity provider of the charts, as cert-manager writes it for a CA or self-signed issuer.
- **Further certificates** (`carLedger.trust.certificates`), PEM in a secret or ConfigMap:

  ```yaml
  carLedger:
    trust:
      certificates:
        - secret: keycloak.example.com-tls
          key: tls.crt
        - configMap: company-ca
          key: ca.pem
  ```

Missing certificates show up as `PKIX path building failed` in the log. They are read at the start, restart the app
after a change (`kubectl rollout restart deployment/car-ledger`). Outside of Kubernetes, `CARLEDGER_TRUST_DIR` points
to a directory with PEM files.

### Database backups

A CronJob dumps the whole database with `pg_dump` (custom format), off by default:

```yaml
backup:
  enabled: true
  schedule: "0 3 * * *"   # time zone of the cluster or backup.timeZone
  keep: 7                 # dumps kept in the volume
  persistence:
    size: 8Gi             # stays after helm uninstall
  s3:                     # optional
    bucket: car-ledger-backups
    prefix: car-ledger-backup/
    endpoint: https://s3.example.com
    existingSecret: backup-s3   # accessKeyId and secretAccessKey
```

It dumps the database of `carLedger.postgres`, also an external one; `pg_dump` must be at least the version of the
server (`backup.image`). Without `backup.persistence.enabled` the dumps only go to S3. Use a bucket of its own with
encryption at rest; old dumps in S3 are deleted by a lifecycle rule of the bucket.

```bash
kubectl create job --from=cronjob/car-ledger-backup car-ledger-backup-now   # one now
kubectl logs job/car-ledger-backup-now -c dump
```

**Restore** (the app is stopped meanwhile):

```bash
kubectl scale deployment/car-ledger --replicas=0
kubectl run restore --rm -it --restart=Never --image=postgres:17-bookworm \
  --overrides='{"spec":{"securityContext":{"runAsUser":1001},"volumes":[{"name":"b","persistentVolumeClaim":{"claimName":"car-ledger-backup"}}],"containers":[{"name":"restore","image":"postgres:17-bookworm","stdin":true,"tty":true,"command":["bash"],"volumeMounts":[{"name":"b","mountPath":"/backup"}]}]}}' \
  --labels=app=car-ledger-backup
# in the pod (password: secret car-ledger-postgresql, key postgres-password)
ls /backup
pg_restore --clean --if-exists -h car-ledger-postgresql -U postgres -d car-ledger /backup/car-ledger-<time>.dump
exit
kubectl scale deployment/car-ledger --replicas=1
```

A dump from S3 is copied into the pod first (`kubectl cp`). An older version of CarLedger can't read the database of a
newer one.

### Metrics

The app serves Prometheus metrics at `/q/metrics` on the management port 9000, not exposed by the ingress. Open the
port for Prometheus:

```yaml
carLedger:
  metrics:
    serviceMonitor:
      enabled: true          # needs the Prometheus Operator
      labels:
        release: prometheus
networkPolicy:
  managementFrom:
    - namespaceSelector:
        matchLabels:
          kubernetes.io/metadata.name: monitoring
```

Without the operator, scrape the service `car-ledger-sv`, port `management`. Besides the metrics of Quarkus:

| Metric                                         | Tags                                       | Meaning                                                   |
|------------------------------------------------|--------------------------------------------|-----------------------------------------------------------|
| `carledger_tracker_points_total`               | `result`: accepted, duplicate, rejected    | points of the GPS trackers                                |
| `carledger_mails_total`                        | `event`, `result`: sent, failed            | mails, failed after the retries                           |
| `carledger_pushes_total`                       | `event`, `result`: delivered, gone, failed | push messages; gone: the subscription ended               |
| `carledger_map_matching_points_total`          | `result`: matched, unmatched               | [map matching](GPS_TRACKER.md#map-matching)               |
| `carledger_track_seconds` (count, sum, bucket) | `kind`: track, frequency                   | time to calculate the map of a period                     |

Keycloak serves its metrics on its own port 9000, opened with `networkPolicy.managementFrom` of the keycloak chart.

### Legal pages

Links to an Impressum and a privacy policy, for installations other people use:

```yaml
carLedger:
  legal:
    imprintUrl: https://example.com/impressum
    privacyUrl: https://example.com/datenschutz
```

They appear on the landing page, in the sidebar and in every mail. Each must be an absolute http(s) URL, otherwise the
app doesn't start. Drafts of a privacy policy: [PRIVACY_TEMPLATE.md](PRIVACY_TEMPLATE.md),
[PRIVACY_TEMPLATE.de.md](PRIVACY_TEMPLATE.de.md) (not legal advice).

## Upgrading to the current version

Which upgrades are supported is in [STABILITY.md](STABILITY.md).

1. **Back up the database**, the migrations can't be undone:

   ```bash
   kubectl exec car-ledger-postgresql-0 -- pg_dump -U postgres car-ledger > car-ledger-$(date +%F).sql
   ```

2. Do the steps of the table below for every version you skip.
3. Upgrade with your values file, not `--reuse-values` (it drops new defaults). Remove `carLedger.image.tag`:

   ```bash
   helm upgrade car-ledger oci://ghcr.io/3thr3n/car-ledger/car-ledger -f my-values.yaml
   ```

The database is migrated on the first start. An older image doesn't start on a migrated database: restore the backup,
or let it ignore the new migrations:

```bash
kubectl set env deployment/car-ledger QUARKUS_FLYWAY_IGNORE_FUTURE_MIGRATIONS=true
```

| Version                  | What to do                                                                                                    |
|--------------------------|---------------------------------------------------------------------------------------------------------------|
| 1.0.0 (map-matching 0.2.0) | Switching to `mapMatching.tiles.volume`: `kubectl delete statefulset car-ledger-map-matching` before the upgrade. Tiles built in the cluster from the three extracts of Germany, Austria and Switzerland: delete them as for [new map data](GPS_TRACKER.md#map-matching), the new default is one extract `dach-latest`. |
| 0.11.0                   | A tool that reaches the management port (9000) of the app or Keycloak from another pod needs an entry in `networkPolicy.managementFrom`. With cert-manager's HTTP-01 challenge, leave a forced https redirect of the ingress off. |
| 0.10.0                   | The firmware volume needs a release that follows [spec 1](TRACKER_FIRMWARE.md). Remove an own `keycloak.image.tag` or authentik image older than Keycloak 26.8.1 / authentik 2026.8.5 (CVE-2026-91403). An own `carLedger.securityContext` replaces the defaults (read-only root filesystem, writable `/tmp`), keep them in it. |
| 0.9.0                    | Remove `carLedger.webhook.*`, webhooks are gone; poll `GET /api/admin/car-requests?state=OPEN` with a [service account](#service-accounts) instead. |
| 0.7.0                    | A proxy in front must pass the header `Authorization` on `/api/device/`. A proxy that sets its own CSP must allow the origins of the map. |
| 0.6.0                    | A proxy in front must pass the header `Idempotency-Key`.                                                      |
| 0.5.0                    | A tool that connects to a PostgreSQL of the charts needs `networkPolicy.extraFrom`. Add the audience mapper to the clients of service accounts ([step 4](#service-accounts)). A realm imported by the keycloak chart before 0.5.1 lacks the mapper of the realm roles in the ID token ([step 2](#2-realm-and-client)), add it if the API answers 403. The app needs outgoing HTTPS to the push services. A proxy must not cache `/sw.js` and must not set stricter security headers than the app. |
| 0.4.0                    | The app chart moved from `oci://ghcr.io/3thr3n/car-ledger/helm` to `oci://ghcr.io/3thr3n/car-ledger/car-ledger`, change the path in `helm upgrade` and in your dependencies. Existing cars become petrol cars, owners of other cars change the energy type in *Edit car*. |
| 0.3.0                    | A Gateway or proxy in front needs requests up to 12 MB (`carLedger.attachments.maxRequestSize`).              |
| before 0.3.0             | Add `state-secret` to the secret `keycloak` (see [step 3](#3-secret-for-the-app)). Create the realm role `admin` and give it to the administrators. The odometer became a reading on a date: check the mileage of your cars. Coming from the bitnami based charts: see [below](#upgrading-from-the-bitnami-based-charts-01x). |

## Upgrading from the bitnami based charts (0.1.x)

Delete the StatefulSets once before the upgrade, their volumes are kept. The Keycloak database of 26.0/26.1 has to go
through 26.6 first:

```bash
# car-ledger chart
kubectl delete statefulset car-ledger-postgresql
helm upgrade car-ledger ...

# keycloak chart, wait until 26.6 is ready before the second upgrade
kubectl delete statefulset keycloak keycloak-postgresql
helm upgrade keycloak ... --set keycloak.image.tag=26.6
helm upgrade keycloak ...
```

`keycloak.ingress.hostname` is now `keycloak.hostname`, the ingress is configured as in the keycloakx chart.

## Operations

- **Health**: `/q/health/started`, `/q/health/live` and `/q/health` on the management port 9000.
- **Slow start**: the startup probe allows 150 s (`carLedger.startupProbe.failureThreshold` 30 × 5 s). On slow nodes
  raise it or the CPU limit (`carLedger.resources`, 350m).
- **HTTPS only**: over plain http the login fails.
- **Replicas**: `carLedger.replicas` can be raised.
- **Passwords**: in the secrets `car-ledger-postgresql` (`postgres-password`), `infinispan`, `keycloak-postgresql` and
  `keycloak-admin`.
- **Car limit**: new accounts may have one car, administrators change it in *Administration*.
