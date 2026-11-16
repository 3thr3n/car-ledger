# authentik for CarLedger

Deploys [authentik](https://goauthentik.io) via the [official chart](https://github.com/goauthentik/helm) with its own
PostgreSQL and sets up CarLedger with a blueprint:

- the OpenID provider `CarLedger` (confidential client `car-ledger`, RS256, scopes `openid`, `email`, `profile`,
  `offline_access`, grant types authorization code and refresh token)
- the application `car-ledger`
- the groups `user` and `admin`, the roles of CarLedger (`admin` starts with `akadmin`)

The client secret and the state secret are generated into the secret `car-ledger-oidc` for the car-ledger chart.

## Install

With the [stack chart](../stack/README.md): `global.authentik.enabled=true`. On its own, in the namespace of the app:

```bash
helm install authentik oci://ghcr.io/3thr3n/car-ledger/authentik \
  --set hostname=authentik.example.com \
  --set carLedger.hostname=car-ledger.example.com

helm install car-ledger oci://ghcr.io/3thr3n/car-ledger/car-ledger \
  --set ingress.host=example.com \
  --set carLedger.auth.url=https://authentik.example.com/application/o/car-ledger/ \
  --set carLedger.auth.clientId=car-ledger \
  --set carLedger.auth.existingSecret=car-ledger-oidc
```

In its own namespace, set `carLedger.namespace` to the existing namespace of the app, `car-ledger-oidc` is created
there:

```bash
kubectl create namespace car-ledger
helm install authentik oci://ghcr.io/3thr3n/car-ledger/authentik -n authentik --create-namespace \
  --set hostname=authentik.example.com \
  --set carLedger.hostname=car-ledger.example.com \
  --set carLedger.namespace=car-ledger

helm install car-ledger oci://ghcr.io/3thr3n/car-ledger/car-ledger -n car-ledger \
  --set ingress.host=example.com \
  --set carLedger.auth.url=https://authentik.example.com/application/o/car-ledger/ \
  --set carLedger.auth.clientId=car-ledger \
  --set carLedger.auth.existingSecret=car-ledger-oidc
```

From the repository, run `helm dependency update helm/authentik` and install `helm/authentik` instead.

Uninstalling authentik deletes that secret; a `car-ledger-oidc` created by hand there has to be deleted before the
install.

The admin is `akadmin`:

```bash
kubectl get secret authentik-bootstrap -o jsonpath='{.data.password}' | base64 -d
```

Create the users and add them to the group `user` (or `admin`), without one CarLedger answers with 403.

## Values

| Value                                        | Default                  | Description                                                                      |
|----------------------------------------------|--------------------------|----------------------------------------------------------------------------------|
| `hostname`                                   |                          | Public host name of authentik, required                                          |
| `tls`                                        | `true`                   | authentik and CarLedger are reached via https (scheme of the redirect URIs)      |
| `carLedger.hostname`                         |                          | Public host name of CarLedger, required for the redirect URIs                    |
| `carLedger.clientId`                         | `car-ledger`             | Client id, `carLedger.auth.clientId` of the car-ledger chart                     |
| `carLedger.slug`                             | `car-ledger`             | Slug of the application, part of the issuer URL                                  |
| `carLedger.namespace`                        | namespace of the release | Namespace of CarLedger, `car-ledger-oidc` is created there as well               |
| `bootstrap.email` / `bootstrap.password`     | generated password       | `akadmin`, only used on the first start                                          |
| `ingress.enabled` / `className` / `certManager` | `true` / `nginx` /    | Ingress for `hostname`, TLS secret `authentik-ingress-tls`                       |
| `httpRoute.enabled` / `parentRefs`           | `false`                  | Gateway API HTTPRoute instead of the ingress                                     |
| `postgresql.*`                               |                          | Image, persistence and resources of the PostgreSQL                               |
| `authentik.*`                                |                          | Values of the [authentik chart](https://github.com/goauthentik/helm/tree/main/charts/authentik), e.g. `authentik.authentik.email` for mails |

Generated secrets, kept on upgrades:

| Secret                | Keys                             |                                                                          |
|-----------------------|----------------------------------|--------------------------------------------------------------------------|
| `authentik-generated` | `secret-key`, `postgres-password` | `secret-key` signs cookies and derives the user ids, never change it     |
| `authentik-bootstrap` | `email`, `password`              | `akadmin`                                                                |
| `car-ledger-oidc`     | `secret`, `state-secret`         | For the car-ledger chart, also in `carLedger.namespace`                  |

The secret names are fixed, install the chart once per namespace.

## Notes

- The blueprint is applied on every start and when it changes. The provider is kept in sync with the values; the
  groups are only created, memberships changed in the UI are kept.
- Logging out of CarLedger logs out of authentik as well (`default-invalidation-flow`), like with Keycloak.
- Accounts of CarLedger are bound to the user name (`preferred_username`). Renaming a user in authentik starts with an
  empty account in CarLedger.
- Idle, the server uses about 730 MiB and the worker about 350 MiB, plus the PostgreSQL.
