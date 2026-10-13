# Deployment

CarLedger runs on Kubernetes. It needs an OpenID Connect provider for the login, the repository has charts for two:

| Chart             | Deploys                                                                                                                                 |
|-------------------|-----------------------------------------------------------------------------------------------------------------------------------------|
| `helm/car-ledger` | The app, its PostgreSQL and Infinispan (keeps the login sessions, shared by all replicas)                                                |
| `helm/keycloak`   | Keycloak 26.7 (via the [keycloakx chart](https://github.com/codecentric/helm-charts/tree/master/charts/keycloakx)) with its own PostgreSQL |
| `helm/authentik`  | authentik 2026.8 with its own PostgreSQL, CarLedger is set up by a blueprint, see [its README](../helm/authentik/README.md)              |

The app is reachable as `car-ledger.<host>`.

- [Identity provider](#identity-provider)
- [Fresh install with Keycloak](#fresh-install-with-keycloak)
- [Upgrading to the current version](#upgrading-to-the-current-version)
- [Upgrading from the bitnami based charts (0.1.x)](#upgrading-from-the-bitnami-based-charts-01x)
- [Operations](#operations)

## Identity provider

CarLedger uses standard OpenID Connect, nothing is specific to Keycloak:

- **Keycloak** (`helm/keycloak`): the default of the car-ledger chart. The realm, the client and the secret for the
  app are set up by hand, see [below](#fresh-install-with-keycloak).
- **authentik** (`helm/authentik`): sets up the client, the groups and the secret for the app by itself. See
  [its README](../helm/authentik/README.md). It is not lighter than Keycloak, idle it uses about 1.1 GiB. In separate
  namespaces, set `carLedger.namespace`, so the secret is created in the namespace of the app as well.
- **Any other provider**, e.g. an existing one. It has to offer:

  | Requirement                    | Details                                                                                                  |
  |--------------------------------|----------------------------------------------------------------------------------------------------------|
  | Confidential client            | Authorization code flow with PKCE (S256) and refresh tokens                                              |
  | Redirect URIs                  | `https://car-ledger.<host>/api/auth/callback`, post logout `https://car-ledger.<host>/api/auth/logout-callback` |
  | Roles `user` and `admin`       | In the claim `groups` (or Keycloak's `realm_access.roles`) of the tokens, every user needs one of them    |
  | User name                      | Claim `preferred_username` (or `upn`, else `sub`); the account of a user is bound to it                  |
  | Scopes                         | `offline_access`, `email`, `profile` by default (`carLedger.auth.scopes`)                                |
  | Signed tokens                  | Verifiable with the published keys (`jwks_uri`), e.g. RS256                                              |
  | Logout (optional)              | `end_session_endpoint`, otherwise only the session of CarLedger ends                                     |

  Configure the car-ledger chart with `carLedger.auth.url` (the issuer), `carLedger.auth.clientId` and a secret with
  the client secret and a state secret (see [step 3](#3-secret-for-the-app)), named by
  `carLedger.auth.existingSecret` (default `keycloak`, keys `carLedger.auth.secretKeys.*`).

Moving to another provider: the accounts of CarLedger are bound to the user name. Users keep their data when they
have the same user name at the new provider.

## Fresh install with Keycloak

### 1. Keycloak

```bash
helm dependency update helm/keycloak
helm install keycloak helm/keycloak --set keycloak.hostname=keycloak.example.com
```

The admin user is `car-ledger`, its password is generated and kept in the secret `keycloak-admin`:

```bash
kubectl get secret keycloak-admin -o jsonpath='{.data.password}' | base64 -d
```

The first start creates the database and can take a few minutes.

### 2. Realm and client

There is no realm import, set it up once in the Keycloak admin console:

1. Realm `car-ledger` (another name: set `carLedger.auth.url` of the car-ledger chart).
2. Client `carLedger` (OpenID Connect):
    - Client authentication: on (confidential), standard flow only
    - Valid redirect URI: `https://car-ledger.example.com/api/auth/callback`
    - Valid post logout redirect URI: `https://car-ledger.example.com/api/auth/logout-callback`
    - Advanced → PKCE method: `S256`
    - Copy the client secret from the *Credentials* tab
3. Realm roles `user` and `admin`. Every user needs `user` (or `admin`), add `user` to the default roles of the
   realm so new users get it. `admin` opens the administration.

### 3. Secret for the app

The app reads the client secret and the state secret from the secret `keycloak` (`carLedger.auth.existingSecret`),
the keycloak chart does not create it:

```bash
kubectl create secret generic keycloak \
  --from-literal=secret='<client secret of carLedger>' \
  --from-literal=state-secret="$(openssl rand -hex 32)"
```

The state secret encrypts the PKCE state cookie during the login. It must be at least 16 characters (Quarkus refuses
shorter ones), 32 or more are recommended, and all replicas need the same one.

### 4. CarLedger

```bash
helm install car-ledger oci://ghcr.io/3thr3n/car-ledger/helm --set ingress.host=example.com
```

Without `--version` the latest release is installed. A release (e.g. `0.3.0`, see the
[releases](https://github.com/3thr3n/car-ledger/releases)) publishes the image `ghcr.io/3thr3n/car-ledger:0.3.0` and the
chart `oci://ghcr.io/3thr3n/car-ledger/helm` in version `0.3.0`, which runs that image.

Every build of `main` publishes the image `ghcr.io/3thr3n/car-ledger:<run number>` and the chart as the prerelease
`<next release>-dev.<run number>`, e.g. `0.4.0-dev.123`, which runs the image of the same build: `--version
0.4.0-dev.123` installs build 123, `--devel` the latest build. Leave `carLedger.image.tag` empty, it would override
the image of the chart.

From the repository (`helm dependency update helm/car-ledger`, `helm install car-ledger helm/car-ledger ...`) the chart
runs the image `main`, the latest build of `main`.

With another release name than `car-ledger`, set `carLedger.postgres.host` to `<release>-postgresql`. Passwords of
PostgreSQL and Infinispan are generated on the first install and kept on upgrades.

Log in with a user with the role `admin`: the sidebar shows *Administration*. If the API answers with 403 for users
with the role `user`, the roles are missing in the tokens of the client.

### Options

- **Gateway API instead of an ingress** (ingress-nginx is retired): `ingress.enabled=false`, `httpRoute.enabled=true`
  and `httpRoute.parentRefs` for the app; `keycloak.ingress.enabled=false`, `keycloak.httpRoute.enabled=true` and
  `keycloak.httpRoute.hostnames` for Keycloak.
- **Infinispan 16**: the charts use the 15.2 server image, as 16.x needs a CPU with x86-64-v3 support (not offered e.g.
  by VMs with the default QEMU CPU model). Set `infinispan.images.server=quay.io/infinispan/server:16.2` if your nodes
  support it.
- **External PostgreSQL**: `carledgerpostgres.enabled=false` and `carLedger.postgres.*` (or `carLedger.postgres.jdbc`).
- **Attachments**: receipts are stored in PostgreSQL, up to `carLedger.attachments.maxSize` (10 MB) per file and
  `carLedger.attachments.maxPerBill` (10) per entry. Uploads need requests up to `carLedger.attachments.maxRequestSize`
  (12 MB): the nginx ingress gets the annotation `nginx.ingress.kubernetes.io/proxy-body-size` automatically, other
  proxies (e.g. a Gateway or a reverse proxy in front of the cluster) need the same limit, otherwise uploads fail with
  413.

## Upgrading to the current version

Checklist for installations running an older image of the app.

### Before

1. **Back up the database** — the migrations below can not be undone:

   ```bash
   kubectl exec car-ledger-postgresql-0 -- pg_dump -U postgres car-ledger > car-ledger-$(date +%F).sql
   ```

2. **Add `state-secret` to the secret `keycloak`** if it is missing. The deployment reads it, without it the pod does
   not start (`CreateContainerConfigError`):

   ```bash
   kubectl get secret keycloak -o jsonpath='{.data.state-secret}' | wc -c   # 0: missing
   kubectl patch secret keycloak -p "{\"stringData\":{\"state-secret\":\"$(openssl rand -hex 32)\"}}"
   ```

3. **Create the realm role `admin`** and assign it to the administrators, the same way as the existing role `user`.

4. Coming from the bitnami based charts: follow [the steps below](#upgrading-from-the-bitnami-based-charts-01x) first.

### Upgrade

```bash
helm upgrade car-ledger oci://ghcr.io/3thr3n/car-ledger/helm -f my-values.yaml
```

Pass your values file instead of `--reuse-values`, which keeps the old values and drops new defaults of the chart (e.g.
the Infinispan 15.2 image). Remove `carLedger.image.tag` from your values: the chart runs the image of its own build
now, a tag set before would keep the old image.

Use an image built from the current `main` (or a git tag of it). On the first start, Flyway migrates the database:

| Migration | Change                                                                                                               |
|-----------|----------------------------------------------------------------------------------------------------------------------|
| 1.1.0     | Settings per account (VAT country `DE`, currency `EUR`, language of the browser), created and last seen date        |
| 1.2.0     | Table for the reminders                                                                                              |
| 1.3.0     | Date of the odometer reading of the cars, see below                                                                  |
| 1.4.0     | Optional plate, VIN, purchase date and price of the cars (0.3.0)                                                     |
| 1.5.0     | Tables for the attachments of the entries and their content (0.3.0)                                                  |
| 1.6.0     | Picture of the cars (0.3.0)                                                                                          |

An older image refuses to start on the migrated database, as Flyway finds migrations it does not know. To go back,
restore the backup. Without the backup, the older image runs on the migrated database (the new columns have defaults)
when it ignores these migrations:

```bash
kubectl set env deployment/car-ledger QUARKUS_FLYWAY_IGNORE_FUTURE_MIGRATIONS=true
```

### After

- **Odometer**: the odometer of a car is now a reading on a date, the current odometer adds the distances of the
  refuels after it. Existing odometers were "the mileage when the tracking started", so 1.3.0 dates them to the day
  before the first entry of the car and all refuels are added. Check the mileage of your cars; if one is off, enter
  the actual mileage in *Edit car*, it becomes a new reading as of today.
- **Settings**: all accounts start with VAT country Germany and the currency Euro. Users with another currency change
  it in *Settings*, existing amounts are not converted.
- **Uploads** (0.3.0): receipts and car pictures need requests up to `carLedger.attachments.maxRequestSize` (12 MB). The
  nginx ingress of the chart gets the annotation automatically; a Gateway or a proxy in front needs the same limit,
  otherwise uploads fail with 413. The files are stored in PostgreSQL, the backup above includes them.

## Upgrading from the bitnami based charts (0.1.x)

PostgreSQL and Keycloak use the official images now. The existing PostgreSQL volumes are reused, Keycloak migrates its
database on the first start, which can take a few minutes. As the StatefulSets changed, delete them once before the
upgrade, their volumes are kept:

```bash
# car-ledger chart
kubectl delete statefulset car-ledger-postgresql
helm upgrade car-ledger ...

# keycloak chart: the database of the bitnami Keycloak (26.0/26.1) can not be migrated to 26.7 directly,
# upgrade to 26.6 first and to the default version afterwards
kubectl delete statefulset keycloak keycloak-postgresql
helm upgrade keycloak ... --set keycloak.image.tag=26.6
helm upgrade keycloak ...
```

Wait until Keycloak 26.6 is ready (the startup probe allows 15 minutes for the migration) before the second upgrade.

The values of the keycloak chart changed: `keycloak.ingress.hostname` is now `keycloak.hostname`, the ingress is
configured as in the keycloakx chart (`keycloak.ingress`).

## Operations

- **Health**: the management port 9000 serves `/q/health/started`, `/q/health/live` and `/q/health` (used by the
  probes), it is not exposed by the ingress.
- **Slow start**: the startup probe allows 150 s (`carLedger.startupProbe.failureThreshold` 30 × `periodSeconds` 5)
  before Kubernetes restarts the pod, liveness and readiness only start afterwards. Raise the threshold on slow
  nodes, or the CPU limit (`carLedger.resources`, 350m by default): the start takes most of its time on CPU.
  `carLedger.livenessProbe` and `carLedger.readinessProbe` are configurable the same way.
- **HTTPS only**: the app expects TLS at the ingress, it sends the redirect URIs to the provider with https and
  marks its cookies as secure. Over plain http the login fails.
- **Replicas**: `carLedger.replicas` can be raised, sessions live in Infinispan and the state secret is shared.
- **Passwords**: generated ones are in the secrets `car-ledger-postgresql` (`postgres-password`), `infinispan`,
  `keycloak-postgresql` and `keycloak-admin`.
- **Car limit**: new accounts may have one car, administrators change the limit per account in *Administration*.
