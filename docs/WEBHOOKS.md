# Webhooks

CarLedger sends events as signed HTTP `POST` requests to a URL, so other tools (n8n, Home Assistant, your own scripts)
can react to them. Without a URL nothing is sent.

## Configuration

| Property                     | Environment variable          | Helm value                                                  | Default    |
|------------------------------|-------------------------------|-------------------------------------------------------------|------------|
| `carledger.webhook.url`      | `CARLEDGER_WEBHOOK_URL`       | `carLedger.webhook.url`                                     | none       |
| `carledger.webhook.secret`   | `CARLEDGER_WEBHOOK_SECRET`    | `carLedger.webhook.existingSecret` / `.secretKey`           | unsigned   |
| `carledger.webhook.events`   | `CARLEDGER_WEBHOOK_EVENTS`    | `carLedger.webhook.events`                                  | all events |
| `carledger.webhook.retries`  | `CARLEDGER_WEBHOOK_RETRIES`   |                                                             | `3`        |
| `carledger.webhook.backoff`  | `CARLEDGER_WEBHOOK_BACKOFF`   |                                                             | `PT2S`     |
| `carledger.webhook.timeout`  | `CARLEDGER_WEBHOOK_TIMEOUT`   |                                                             | `PT10S`    |

`events` is a comma separated list of the event types below, e.g. `car-request.created,car-request.decided`.

With the chart, the secret comes from an existing Kubernetes secret:

```bash
kubectl create secret generic car-ledger-webhook --from-literal=webhook-secret="$(openssl rand -hex 32)"
```

```yaml
carLedger:
  webhook:
    url: https://n8n.example.com/webhook/car-ledger
    existingSecret: car-ledger-webhook
    secretKey: webhook-secret
```

## Request

```http
POST /webhook/car-ledger HTTP/1.1
Content-Type: application/json
X-CarLedger-Event: car-request.created
X-CarLedger-Delivery: 9b2f6a3e-5c1d-4f7e-8a0b-2d4c6e8f0a1b
X-CarLedger-Signature: sha256=adbde1ce40c89c14215687d5d762a47df6dfaefcfad61e2e86718ffc8498571b
```

| Header                  | Content                                                                    |
|-------------------------|----------------------------------------------------------------------------|
| `X-CarLedger-Event`     | the type of the event, the same as `type` in the body                      |
| `X-CarLedger-Delivery`  | the id of the event, the same as `id` in the body and for every retry      |
| `X-CarLedger-Signature` | `sha256=` and the hex HMAC-SHA256 of the body with the secret; missing without a secret |

Every body has the same envelope:

| Field        | Content                                                                          |
|--------------|----------------------------------------------------------------------------------|
| `version`    | version of the payload, `1`; raised with incompatible changes of an event's data |
| `type`       | the type of the event                                                            |
| `id`         | unique per event; use it to ignore a retry you have already handled               |
| `occurredAt` | when it happened, ISO 8601                                                       |
| `data`       | depends on the type                                                              |

Only what a receiver needs is sent: user names, numbers and texts the users entered, no tokens or internals.

## Events

### `car-request.created`

A user who reached the car limit requested more cars.

```json
{
  "version": 1,
  "type": "car-request.created",
  "id": "9b2f6a3e-5c1d-4f7e-8a0b-2d4c6e8f0a1b",
  "occurredAt": "2026-10-15T09:12:44.512+02:00",
  "data": {
    "requestId": 42,
    "account": "bob",
    "cars": 1,
    "maxCars": 1,
    "reason": "For the trailer",
    "createdAt": "2026-10-15T09:12:44.498+02:00"
  }
}
```

`cars` is the number of further cars, `maxCars` the limit of the account; `reason` is optional. An administrator
decides with `POST /api/admin/car-requests/{requestId}/approve` (body `{ "maxCars": 2 }`, optional, the default is the
current limit plus the requested cars) or `POST /api/admin/car-requests/{requestId}/reject` (body
`{ "comment": "..." }`, optional).

### `car-request.decided`

An administrator approved or rejected a request.

```json
{
  "version": 1,
  "type": "car-request.decided",
  "id": "0c3e5a7b-9d1f-4b2a-8c6e-4f0a2b4d6e8f",
  "occurredAt": "2026-10-15T09:20:03.127+02:00",
  "data": {
    "requestId": 42,
    "account": "bob",
    "state": "APPROVED",
    "cars": 1,
    "grantedMaxCars": 2,
    "comment": null,
    "decidedBy": "alice",
    "decidedAt": "2026-10-15T09:20:03.101+02:00"
  }
}
```

`state` is `APPROVED` or `REJECTED`; `grantedMaxCars` is the new limit of an approval, `comment` the optional comment
of a rejection.

## Verifying the signature

Compute the HMAC-SHA256 of the **raw** body with the secret and compare it with the header, in constant time. Parse the
JSON only afterwards, a re-serialized body has a different signature.

Shell:

```bash
printf '%s' "$BODY" | openssl dgst -sha256 -hmac "$SECRET" | sed 's/^.* /sha256=/'
```

Node.js:

```js
import { createHmac, timingSafeEqual } from 'node:crypto';

function verify(rawBody, signature, secret) {
  const expected = 'sha256=' + createHmac('sha256', secret).update(rawBody).digest('hex');
  return signature?.length === expected.length && timingSafeEqual(Buffer.from(signature), Buffer.from(expected));
}
```

Python:

```python
import hashlib, hmac

def verify(raw_body: bytes, signature: str, secret: str) -> bool:
    expected = "sha256=" + hmac.new(secret.encode(), raw_body, hashlib.sha256).hexdigest()
    return hmac.compare_digest(signature or "", expected)
```

## Delivery

- An event is sent after the change it describes was saved; a failed change sends nothing.
- It is sent in the background: a slow, failing or missing receiver never delays or fails the request of the user.
- Any `2xx` answer counts as delivered. Otherwise, or without an answer within the timeout, it is sent again up to
  `retries` times, waiting `backoff`, then four times as long each time (2 s, 8 s, 32 s by default). After the last
  attempt the event is dropped with a warning in the log of CarLedger.
- The events are not stored: an event that occurs while CarLedger restarts during the retries is lost. Receivers that
  must not miss a decision can read the state with the API, e.g. `GET /api/admin/car-requests?state=OPEN`.
