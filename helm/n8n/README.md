# n8n for CarLedger

Optional: approve car requests with [n8n](https://n8n.io) instead of on the administration page, see
[docs/N8N.md](../../docs/N8N.md). It deploys n8n via the [official chart](https://github.com/n8n-io/n8n-hosting/tree/main/charts/n8n)
as one pod with SQLite on a volume (standalone mode, no PostgreSQL or Redis), which is enough for a few workflows.

The chart sets up what the [example workflow](../../docs/n8n/car-requests.json) needs:

- the signature key of the webhooks, generated into the secret `car-ledger-webhook` (key `webhook-secret`), which the
  car-ledger chart reads; n8n gets it as `CARLEDGER_WEBHOOK_SECRET`
- `CARLEDGER_URL`, the address of CarLedger for the calls of the admin API
- `NODE_FUNCTION_ALLOW_BUILTIN=crypto` and `N8N_BLOCK_ENV_ACCESS_IN_NODE=false`: the Code node *Verify signature*
  computes the HMAC with `crypto` and reads the secret from `$env`, which n8n 2 blocks by default
- the encryption key of the credentials stored in n8n, generated into the secret `n8n-generated` and kept on upgrades

## Install

Both charts in the same namespace:

```bash
helm dependency update helm/n8n
helm install n8n helm/n8n \
  --set hostname=n8n.example.com \
  --set carLedger.hostname=car-ledger.example.com
```

Then send the webhooks of CarLedger to n8n, in the values file of the car-ledger chart:

```yaml
carLedger:
  webhook:
    url: https://n8n.example.com/webhook/car-ledger
    existingSecret: car-ledger-webhook
    events: [car-request.created]
```

```bash
helm upgrade car-ledger oci://ghcr.io/3thr3n/car-ledger/helm -f my-values.yaml
```

In separate namespaces, set `carLedger.namespace` to the one of CarLedger. The secret `car-ledger-webhook` is then
created there as well, the pods of CarLedger can only read secrets of their own namespace. The namespace has to exist
before.

Then open `https://n8n.example.com`, create the owner account (the first visitor becomes the owner, so do it right
after the install) and continue with [the service account and the workflow](../../docs/N8N.md).

## Values

| Value                  | Default | Description                                                                            |
|------------------------|---------|----------------------------------------------------------------------------------------|
| `hostname`             |         | public host name of n8n, required                                                      |
| `tls`                  | `true`  | n8n and CarLedger are reached via https                                                |
| `carLedger.hostname`   |         | public host name of CarLedger, required                                                |
| `carLedger.namespace`  |         | namespace of CarLedger, if it is not the one of n8n                                    |
| `ingress.enabled`      | `true`  | ingress for `hostname`                                                                 |
| `ingress.className`    | `nginx` | ingress class                                                                          |
| `ingress.certManager`  |         | cluster issuer of cert-manager, e.g. `lets-encrypt`                                    |
| `httpRoute.enabled`    | `false` | HTTPRoute of the Gateway API instead of the ingress                                    |
| `n8n.*`                |         | values of the [n8n chart](https://github.com/n8n-io/n8n-hosting/tree/main/charts/n8n) |

The editor and the webhooks share the host name. The links of *Send and Wait for Response* in the messages to the
administrators point to it as well, so n8n has to be reachable from where they open them.

## Backup

Workflows, credentials and executions are in the SQLite database on the volume `n8n`. The credentials are encrypted with
`N8N_ENCRYPTION_KEY` from the secret `n8n-generated`: back up the secret together with the volume, without it the
credentials can not be read. Alternatively export the workflow from the editor; the credentials are then entered again.
