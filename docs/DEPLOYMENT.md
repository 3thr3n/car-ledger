# Deployment

CarLedger runs on Kubernetes. It needs an OpenID Connect provider for the login, the repository has charts for two:

| Chart             | Deploys                                                                                                                                 |
|-------------------|-----------------------------------------------------------------------------------------------------------------------------------------|
| `helm/stack`      | All of the charts below in one release, each switched on with one value and set up to work together, see [its README](../helm/stack/README.md) |
| `helm/car-ledger` | The app, its PostgreSQL and Infinispan (keeps the login sessions, shared by all replicas)                                                |
| `helm/keycloak`   | Keycloak 26.7 (via the [keycloakx chart](https://github.com/codecentric/helm-charts/tree/master/charts/keycloakx)) with its own PostgreSQL |
| `helm/authentik`  | authentik 2026.8 with its own PostgreSQL, CarLedger is set up by a blueprint, see [its README](../helm/authentik/README.md)              |

The app is reachable as `car-ledger.<host>`.

A release publishes the charts at `oci://ghcr.io/3thr3n/car-ledger/<chart>`: `car-ledger` (the app) and `stack` in the
version of the release, `keycloak` and `authentik` in their own versions (raised when they change, the release notes
name them). From the repository, run `helm dependency update helm/<chart>` before the install.

- [Everything in one release](#everything-in-one-release)
- [Identity provider](#identity-provider)
- [Fresh install with Keycloak](#fresh-install-with-keycloak)
- [Upgrading to the current version](#upgrading-to-the-current-version)
- [Upgrading from the bitnami based charts (0.1.x)](#upgrading-from-the-bitnami-based-charts-01x)
- [Operations](#operations)

## Everything in one release

The stack chart deploys the app and, as chosen, Keycloak or authentik. It sets the host names, the login of the app
and the generated secrets up between them, e.g. with authentik:

```bash
helm install car-ledger oci://ghcr.io/3thr3n/car-ledger/stack \
  --set global.host=example.com \
  --set global.authentik.enabled=true
```

See [its README](../helm/stack/README.md) for the values and the steps after the install. The sections below install
the charts one by one, which also works with the identity provider in another namespace.

## Identity provider

CarLedger uses standard OpenID Connect, nothing is specific to Keycloak:

- **Keycloak** (`helm/keycloak`): the default of the car-ledger chart. With `carLedger.hostname`, it imports the realm
  with the client and the roles and generates the secret for the app, see [below](#fresh-install-with-keycloak). In
  separate namespaces, set `carLedger.namespace`, so the secret is created in the namespace of the app as well.
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
helm install keycloak oci://ghcr.io/3thr3n/car-ledger/keycloak \
  --set keycloak.hostname=keycloak.example.com \
  --set carLedger.hostname=car-ledger.example.com
```

The admin user is `car-ledger`, its password is generated and kept in the secret `keycloak-admin`:

```bash
kubectl get secret keycloak-admin -o jsonpath='{.data.password}' | base64 -d
```

The first start creates the database and can take a few minutes.

### 2. Realm and client

With `carLedger.hostname`, Keycloak imports the realm `car-ledger` on its first start:

- the client `carLedger`: confidential, standard flow with PKCE `S256`, the redirect URIs of CarLedger
- the realm roles `user` and `admin`; `user` is a default role, every new user gets it. `admin` opens the
  administration, give it to the administrators.

Create the users of CarLedger in the realm `car-ledger` (*Users → Add user*, then a password under *Credentials*).

An existing realm `car-ledger` is kept as it is, the import only creates a missing one. Changes in the admin console
are kept as well. Another realm name or client id: set up the realm by hand.

Without `carLedger.hostname`, nothing is imported; set the realm up once in the Keycloak admin console:

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

The app reads the client secret and the state secret from the secret `keycloak` (`carLedger.auth.existingSecret`).
With `carLedger.hostname`, the keycloak chart generates it (key `secret` is the client secret of the imported realm)
and keeps it on upgrades; a secret `keycloak` created by hand before is kept and not taken over. Without the import,
create it with the client secret of `carLedger`:

```bash
kubectl create secret generic keycloak \
  --from-literal=secret='<client secret of carLedger>' \
  --from-literal=state-secret="$(openssl rand -hex 32)"
```

The state secret encrypts the PKCE state cookie during the login. It must be at least 16 characters (Quarkus refuses
shorter ones), 32 or more are recommended, and all replicas need the same one.

### 4. CarLedger

```bash
helm install car-ledger oci://ghcr.io/3thr3n/car-ledger/car-ledger --set ingress.host=example.com
```

Without `--version` the latest release is installed. A release (e.g. `0.5.0`, see the
[releases](https://github.com/3thr3n/car-ledger/releases)) publishes the image `ghcr.io/3thr3n/car-ledger:0.5.0` and the
chart `oci://ghcr.io/3thr3n/car-ledger/car-ledger` in version `0.5.0`, which runs that image (up to 0.3.x the chart was
`oci://ghcr.io/3thr3n/car-ledger/helm`).

Every build of `develop` publishes the image `ghcr.io/3thr3n/car-ledger:<run number>` and the chart as the prerelease
`<next release>-dev.<run number>`, e.g. `0.6.0-dev.123`, which runs the image of the same build: `--version
0.6.0-dev.123` installs build 123, `--devel` the latest build. Leave `carLedger.image.tag` empty, it would override
the image of the chart.

From the repository (`helm dependency update helm/car-ledger`, `helm install car-ledger helm/car-ledger ...`) the chart
runs the image `main`, the latest build of `main`, which holds the released states. The image `develop` is the latest
dev build.

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
- **Backups**: users download all of their data as ZIP in the settings and restore it into an account without cars.
  The restore is an upload as well and limited by `carLedger.attachments.maxRequestSize`: a backup with many receipts
  can be larger, raise the limit (and the one of the proxies) for the restore.
- **Webhooks**: `carLedger.webhook.url` sends events like a new car request as signed HTTP requests; the key of the
  signature comes from `carLedger.webhook.existingSecret`. See [WEBHOOKS.md](WEBHOOKS.md).
- **Service accounts**: besides the login of the browser, the API accepts access tokens of the identity provider
  (`Authorization: Bearer …`), e.g. of a script deciding car requests. See [Service accounts](#service-accounts).
- **E-mail**: `carLedger.mail.host` sends notifications by e-mail, see [E-mail](#e-mail).
- **Push**: the notifications reach the browsers as push messages, on by default, see [Push](#push).
- **Network policies**: only the app reaches its PostgreSQL and Infinispan, only Keycloak and authentik their
  PostgreSQL (`networkPolicy.enabled`, on by default in all charts; needs a network plugin that enforces them). Further
  sources, e.g. a backup tool: `networkPolicy.extraFrom`. `kubectl exec … pg_dump` (see [Operations](#operations))
  is not affected.
- **Installable app**: the app has a web app manifest (`/manifest.json`) and a service worker (`/sw.js`), which keeps
  the files of the app (not the data) for a fast start. Both need HTTPS, which the ingress provides. A proxy or CDN in
  front must not cache `/sw.js` for long, otherwise new versions arrive late; the app asks the user to reload when a
  new version is ready.

### E-mail

With an SMTP server, CarLedger mails the administrators about new car requests, the user about the decision, and
every user once a day about the reminders of the cars that became due:

```yaml
carLedger:
  mail:
    host: smtp.example.com
    port: 587                 # STARTTLS (startTls: REQUIRED), or 465 with tls: true
    from: "CarLedger <car-ledger@example.com>"
    username: car-ledger@example.com
    existingSecret: car-ledger-mail   # key "password", e.g.:
    # kubectl create secret generic car-ledger-mail --from-literal=password='...'
```

- **Recipients**: the address is the claim `email` of the login (scope `email`, requested by default), stored at every
  visit. Users without an address at the identity provider get no mails. The administrators are the accounts whose
  last login had the role `admin`: a role taken away counts after the next login.
- **Language**: the language of the settings, else the one of the browser at the last visit (English or German).
- **Settings**: every user turns the mails off in *Settings → Notifications by e-mail*, administrators also the
  ones about new requests.
- **Reminders**: at 07:00 UTC (`carLedger.mail.remindersCron`, a Quartz cron, `off` for never) every user gets one
  mail with the reminders that are due soon (30 days or 1,000 km before) or overdue, the most urgent first, in the units
  of the account. A reminder is mailed once per due date and odometer: done or changed, it is mailed again when it is
  due the next time. With several replicas only one sends it.
- **Links**: the mails link to `carLedger.mail.appUrl`, by default `https://car-ledger.<ingress.host>`.
- **Delivery**: after the change is saved, in the background, with three retries; a failing mail server never fails
  the request of the user, the error is logged. The count of open requests in the app stays the reliable source.

Without a host nothing is sent, the settings page says so. In dev mode the mails are only written to the log.

### Push

The same notifications (due reminders, new car requests, decisions) reach the browsers as push messages (Web Push),
on every device where the user turns on *Settings → Notifications → Push on this device*. A user can have both, e-mail
and push, or only one of them.

- **Nothing to set up**: the chart generates the key of the server (VAPID, P-256) into the secret `car-ledger-push` and
  keeps it on upgrades. A new key ends all subscriptions, the users turn push on again. An own key:
  `carLedger.push.existingSecret` with the key `private-key`, e.g.
  `openssl ecparam -name prime256v1 -genkey -noout | kubectl create secret generic my-push --from-file=private-key=/dev/stdin`.
- **Push services**: the messages go to the push service of the browser (Google for Chrome and Android, Mozilla for
  Firefox, Apple for Safari), end-to-end encrypted: the service cannot read them. The app needs outgoing HTTPS to
  them. `carLedger.push.subject` is the contact the services see, by default `https://car-ledger.<ingress.host>`.
  Subscriptions and messages are limited to the hosts of these services (`carLedger.push.allowedHosts`), so a
  subscription cannot make the app send requests to other hosts, e.g. inside the cluster.
- **iPhone and iPad**: only the installed app (home screen) receives push messages, the settings say so.
- **Delivery**: after the change is saved, in the background, with three retries; a browser the push service does not
  know anymore (uninstalled, permission withdrawn) is removed.
- `carLedger.push.enabled=false` turns push off, the settings say so.

### Service accounts

A tool or script calls the API as an OpenID Connect client of its own, whose tokens carry the role `admin` (or
`user`). In Keycloak, in the realm `car-ledger`:

1. **Clients → Create client**, e.g. client id `car-ledger-bot`, OpenID Connect.
2. **Capability config**: *Client authentication* on, *Standard flow* and *Direct access grants* off,
   *Service accounts roles* on.
3. **Service accounts roles** tab of the client → *Assign role* → filter by realm roles → `admin`.
4. **Client scopes** tab → `car-ledger-bot-dedicated` → *Configure a new mapper* → **Audience**, name `audience
   carLedger`, *Included Client Audience* `carLedger`, *Add to access token* on. CarLedger only accepts access tokens
   that name it as audience, tokens of other clients of the realm are rejected (`carLedger.auth.audience`, `any`
   turns the check off).
5. Copy the client secret from the **Credentials** tab.

```bash
TOKEN=$(curl -s https://keycloak.example.com/realms/car-ledger/protocol/openid-connect/token \
  -d grant_type=client_credentials -d client_id=car-ledger-bot -d client_secret="$SECRET" | jq -r .access_token)
curl -H "Authorization: Bearer $TOKEN" "https://car-ledger.example.com/api/admin/car-requests?state=OPEN"
```

Requests decided this way show `service-account-car-ledger-bot` as the deciding administrator. authentik issues such
tokens with the `client_credentials` grant of the provider *CarLedger*, the user name of a service account in the group
`admin` and an app password, see the
[authentik documentation](https://docs.goauthentik.io/add-secure-apps/providers/oauth2/client_credentials/).

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
helm upgrade car-ledger oci://ghcr.io/3thr3n/car-ledger/car-ledger -f my-values.yaml
```

Pass your values file instead of `--reuse-values`, which keeps the old values and drops new defaults of the chart (e.g.
the Infinispan 15.2 image). Remove `carLedger.image.tag` from your values: the chart runs the image of its own build
now, a tag set before would keep the old image.

Use the image of a release (a git tag of `main`) or a dev build of `develop`. On the first start, Flyway migrates the database:

| Migration | Change                                                                                                               |
|-----------|----------------------------------------------------------------------------------------------------------------------|
| 1.1.0     | Settings per account (VAT country `DE`, currency `EUR`, language of the browser), created and last seen date        |
| 1.2.0     | Table for the reminders                                                                                              |
| 1.3.0     | Date of the odometer reading of the cars, see below                                                                  |
| 1.4.0     | Optional plate, VIN, purchase date and price of the cars (0.3.0)                                                     |
| 1.5.0     | Tables for the attachments of the entries and their content (0.3.0)                                                  |
| 1.6.0     | Picture of the cars (0.3.0)                                                                                          |
| 1.7.0     | Energy type of the cars (existing cars become petrol), optional tank size and battery capacity (0.4.0)               |
| 1.8.0     | Table for the charges of electric cars and plug-in hybrids (0.4.0)                                                   |
| 1.9.0     | Unit system of the accounts (metric, US, UK), existing accounts stay metric (0.4.0)                                  |
| 1.10.0    | Table for the requests of the users, first: more cars (0.4.0)                                                        |
| 1.11.0    | Address, role and browser language of the accounts, notification settings, all on by default (0.5.0)                |
| 1.12.0    | Which due of a reminder was mailed, the mails of the reminders on by default (0.5.0)                                 |
| 1.13.0    | The browsers that receive push messages, the switch of the e-mails (on) (0.5.0)                                      |

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
- **E-mail** (0.5.0, optional): with `carLedger.mail.host` the app mails new car requests, decisions and due
  reminders (on the first day all reminders that are due already, in one mail per user), see
  [E-mail](#e-mail). The addresses and the role `admin` are stored at the next visit of each user, until then
  nobody gets mails.
- **Network policies** (0.5.0; keycloak chart 0.5.0, authentik chart 0.4.0): only the app reaches its databases. A
  tool that connects to a PostgreSQL over the network (backup, monitoring) needs `networkPolicy.extraFrom`.
- **Audience** (0.5.0): access tokens (`Authorization: Bearer`) must name CarLedger as audience (`aud`, the client
  id), tokens of other clients of the same realm are rejected; the login of the browser is not affected. Add the
  audience mapper to the clients of service accounts ([Service accounts](#service-accounts), step 4); the realm of
  the keycloak chart has it for `carLedger` itself. authentik sets the audience by itself. `carLedger.auth.audience=any`
  keeps the old behaviour.
- **Push** (0.5.0): on by default, the chart creates the secret `car-ledger-push` with the key of the server. Each user
  turns it on per device in the settings, see [Push](#push). The app needs outgoing HTTPS to the push services.
- **Installable app and headers** (0.5.0): CarLedger can be installed as an app and asks to reload when the server
  runs a newer version. A proxy or CDN in front must not cache `/sw.js` for long (see [Options](#options)). The app
  sends its own security headers (CSP, HSTS, …); a proxy that sets them as well must not set stricter ones.
- **Energy type** (0.4.0): existing cars become petrol cars. Owners of another car (diesel, LPG, CNG, electric,
  plug-in hybrid) change it in *Edit car*; only electric cars and plug-in hybrids record charges.
- **Car requests** (0.4.0): users who reached their car limit request more cars in the garage, the role `admin` approves
  or rejects them on the administration page; the entry *Administration* shows the number of open requests. Nothing
  to set up.
- **Webhooks and service accounts** (0.4.0, optional): without `carLedger.webhook.url` no events are sent, see
  [WEBHOOKS.md](WEBHOOKS.md). Scripts can call the API with the access token of a service account, see
  [Service accounts](#service-accounts).
- **Chart name** (0.4.0): the app chart is named `car-ledger` now, published at
  `oci://ghcr.io/3thr3n/car-ledger/car-ledger`. The old path `oci://ghcr.io/3thr3n/car-ledger/helm` ends with 0.3.0,
  switch the path in `helm upgrade` (the release and its resources stay the same) and in the dependencies of your
  charts (`name: car-ledger`).
- **Charts** (0.4.0): `keycloak` and `authentik` are published as well, upgrade them from
  `oci://ghcr.io/3thr3n/car-ledger/<chart>`. Installed on their own, they deploy the same resources as before; only in
  the stack chart do they get other names. The values `carLedger.auth.clientId` and `carLedger.auth.existingSecret` of
  the app chart are empty by default now, they still default to `carLedger` and `keycloak`.
- **Stack chart** (0.4.0, new): `oci://ghcr.io/3thr3n/car-ledger/stack` deploys the app with Keycloak or authentik in
  one release, see [Everything in one release](#everything-in-one-release). It is meant for new installations: an
  existing app release named `car-ledger` can switch to it without Keycloak or authentik in the stack (its values move
  under `app:`), an existing Keycloak or authentik is not taken over.
- **Keycloak realm** (keycloak chart 0.4.0): with `carLedger.hostname`, the chart imports the realm `car-ledger` and
  generates the secret `keycloak`. Existing installations can set it as well: the existing realm and a secret
  `keycloak` created by hand are kept. Keycloak starts with `--import-realm` and mounts the ConfigMap
  `keycloak-realm` in any case (empty without `carLedger.hostname`), so its pod is restarted by the upgrade.
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
