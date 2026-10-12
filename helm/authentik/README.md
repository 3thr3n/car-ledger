# authentik for CarLedger

An alternative to the keycloak chart that needs no manual setup. It deploys
[authentik](https://goauthentik.io) via the [official chart](https://github.com/goauthentik/helm) with its own
PostgreSQL (official image, the bitnami one of the authentik chart is not used), and sets up CarLedger with a
blueprint:

- the OpenID provider `CarLedger` (confidential client `car-ledger`, RS256, scopes `openid`, `email`, `profile`,
  `offline_access`, grant types authorization code and refresh token)
- the application `car-ledger`
- the groups `user` and `admin`, the roles of CarLedger (`admin` starts with `akadmin`)

The client secret and the state secret of CarLedger are generated into the secret `car-ledger-oidc`, which the
car-ledger chart reads. Nothing has to be configured in the authentik UI.

## Install

Both charts in the same namespace:

```bash
helm dependency update helm/authentik
helm install authentik helm/authentik \
  --set hostname=authentik.example.com \
  --set carLedger.hostname=car-ledger.example.com

helm install car-ledger helm/car-ledger \
  --set ingress.host=example.com \
  --set carLedger.image.tag=<image tag> \
  --set carLedger.auth.url=https://authentik.example.com/application/o/car-ledger/ \
  --set carLedger.auth.clientId=car-ledger \
  --set carLedger.auth.existingSecret=car-ledger-oidc
```

The admin of authentik is `akadmin`, its password is generated and kept in the secret `authentik-bootstrap`:

```bash
kubectl get secret authentik-bootstrap -o jsonpath='{.data.password}' | base64 -d
```

Log in at `https://authentik.example.com`, create the users of CarLedger and add them to the group `user` (or
`admin`). Users without one of the groups can log in at authentik, but CarLedger answers with 403.

## Values

| Value                                        | Default                  | Description                                                                      |
|----------------------------------------------|--------------------------|----------------------------------------------------------------------------------|
| `hostname`                                   |                          | Public host name of authentik, required                                          |
| `tls`                                        | `true`                   | authentik and CarLedger are reached via https (scheme of the redirect URIs)      |
| `carLedger.hostname`                         |                          | Public host name of CarLedger, required for the redirect URIs                    |
| `carLedger.clientId`                         | `car-ledger`             | Client id, `carLedger.auth.clientId` of the car-ledger chart                     |
| `carLedger.slug`                             | `car-ledger`             | Slug of the application, part of the issuer URL                                  |
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
| `car-ledger-oidc`     | `secret`, `state-secret`         | For the car-ledger chart                                                 |

The secret names are fixed (the authentik chart does not template its environment), so install the chart once per
namespace.

## Notes

- The blueprint is applied on every start and when it changes. The provider is kept in sync with the values; the
  groups are only created, memberships changed in the UI are kept.
- Logging out of CarLedger logs out of authentik as well (`default-invalidation-flow`), like with Keycloak.
- Accounts of CarLedger are bound to the user name (`preferred_username`). Renaming a user in authentik starts with an
  empty account in CarLedger.
- authentik is not lighter than Keycloak: idle, the server uses about 730 MiB and the worker about 350 MiB (measured
  with 2026.8.3), plus the PostgreSQL. It needs no manual setup though, the keycloak chart needs the realm, the client
  and the secret set up by hand.
