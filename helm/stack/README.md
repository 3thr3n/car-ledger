# CarLedger stack

CarLedger and the charts next to it in one release: the app (`helm/car-ledger`), the identity provider
(`helm/keycloak` or `helm/authentik`). Every part is switched on with one value, the host names, URLs and secrets
between them are set up by the stack:

| Enabled                         | Set up                                                                                                        |
|---------------------------------|---------------------------------------------------------------------------------------------------------------|
| always                          | CarLedger at `car-ledger.<global.host>`                                                                       |
| `global.authentik.enabled=true` | authentik at `authentik.<global.host>` with CarLedger as application; the app logs in there with the client `car-ledger` and the generated secret `car-ledger-oidc` |
| `global.keycloak.enabled=true`  | Keycloak at `keycloak.<global.host>` with the realm `car-ledger` imported; the app logs in there with the client `carLedger` and the generated secret `keycloak` |

Without an identity provider, the app uses an existing one, see `app.carLedger.auth.*` and
[DEPLOYMENT.md](../../docs/DEPLOYMENT.md#identity-provider).

## Install

Nothing has to be set up by hand besides the users, e.g. with authentik:

```bash
helm install car-ledger oci://ghcr.io/3thr3n/car-ledger/stack \
  --set global.host=example.com \
  --set global.authentik.enabled=true
```

The stack has the version of the app: the release `0.9.0` runs the image `0.9.0` and contains the other charts of the
same commit. Builds of `develop` are published as prereleases like the app chart (`--devel`).

Name the release `car-ledger` and install it once per namespace: the names of the resources are fixed (e.g. the
PostgreSQL of the app `car-ledger-postgresql`, the secrets `car-ledger-oidc` and `car-ledger-push`). The
PostgreSQL of Keycloak and authentik are `car-ledger-keycloak-postgresql` and `car-ledger-authentik-postgresql`.

After the install, the notes of the release name the admin passwords and the next steps:

- **authentik**: log in as `akadmin` (`kubectl get secret authentik-bootstrap -o jsonpath='{.data.password}' | base64 -d`)
  and add the users of CarLedger to the group `user` (or `admin`).
- **Keycloak**: log in as `car-ledger` (secret `car-ledger-keycloak-admin`), create the users of CarLedger in the realm
  `car-ledger` (they get the role `user`) and give the administrators the realm role `admin`.

## Values

| Value                      | Default | Description                                                                         |
|----------------------------|---------|-------------------------------------------------------------------------------------|
| `global.host`              |         | Base domain, required                                                               |
| `global.keycloak.enabled`  | `false` | Deploy Keycloak, at most one of Keycloak and authentik                              |
| `global.authentik.enabled` | `false` | Deploy authentik                                                                    |
| `app.*`                    |         | Values of the [car-ledger chart](../car-ledger/values.yaml)                         |
| `keycloak.*`               |         | Values of the [keycloak chart](../keycloak/values.yaml)                             |
| `authentik.*`              |         | Values of the [authentik chart](../authentik/README.md)                             |

Values set for a chart win over the ones of the stack, e.g. `authentik.hostname=login.example.com` for another host
name. The app does not see the values of the other
charts: with another host name or slug of authentik, set `app.carLedger.auth.url` as well.

A values file for the stack with cert-manager, e.g. `my-values.yaml`:

```yaml
global:
  host: example.com
  authentik:
    enabled: true

app:
  ingress:
    certManager: lets-encrypt
authentik:
  ingress:
    certManager: lets-encrypt
```

With the Gateway API instead of an ingress, switch each chart: `app.ingress.enabled=false`,
`app.httpRoute.enabled=true`, the same for `authentik.*`, and for Keycloak
`keycloak.keycloak.ingress.enabled=false`, `keycloak.keycloak.httpRoute.enabled=true` and
`keycloak.keycloak.httpRoute.hostnames`.

## From the repository

```bash
for chart in car-ledger keycloak authentik stack; do helm dependency update helm/$chart; done
helm install car-ledger helm/stack --set global.host=example.com ...
```

Installed from the repository, the app runs the image `main`, the latest release.

## Moving an existing installation into the stack

The PostgreSQL of the app keeps its name `car-ledger-postgresql` when the stack is named `car-ledger`, but its
resources belong to another release. Keycloak and authentik get new names in the stack (their data is not taken
over). Moving is not supported: keep the separate charts, or start fresh with the stack and restore the backups of the
users (*Settings → Backup*).
