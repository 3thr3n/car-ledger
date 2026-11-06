#!/usr/bin/env bash
# Renders the charts with the combinations of values of the docs and checks every resource against the schemas of
# Kubernetes (kubeconform, strict: also unknown fields), e.g. a field at the wrong place, which helm lint lets through.
# The HTTPRoute of the Gateway API is checked with the schema of the CRDs catalog. Needs the dependencies of the
# charts (helm dependency update).
set -euo pipefail
cd "$(dirname "$0")"

KUBERNETES_VERSION="${KUBERNETES_VERSION:-1.34.0}"
CRDS='https://raw.githubusercontent.com/datreeio/CRDs-catalog/main/{{.Group}}/{{.ResourceKind}}_{{.ResourceAPIVersion}}.json'

render() {
  echo "# helm template $*" >&2
  helm template car-ledger "$@"
  echo "---"
}

{
  # the app on its own, with everything that can be turned on
  render car-ledger --set ingress.host=example.com
  render car-ledger --set ingress.host=example.com \
    --set ingress.enabled=false --set httpRoute.enabled=true \
    --set carLedger.mail.host=smtp.example.com --set carLedger.mail.username=car-ledger \
    --set carLedger.mail.existingSecret=mail \
    --set 'carLedger.trust.certificates[0].secret=company-ca' \
    --set 'carLedger.trust.certificates[1].configMap=other-ca' \
    --set 'networkPolicy.extraFrom[0].podSelector.matchLabels.app=pg-backup'
  render car-ledger --set ingress.host=example.com \
    --set carledgerpostgres.enabled=false --set infinispan.enabled=false --set carLedger.push.enabled=false \
    --set networkPolicy.enabled=false --set carLedger.trust.ingressCA=false
  # the identity providers on their own
  render keycloak --set keycloak.hostname=keycloak.example.com --set carLedger.hostname=car-ledger.example.com
  render authentik --set hostname=authentik.example.com --set carLedger.hostname=car-ledger.example.com
  render authentik --set hostname=authentik.example.com --set carLedger.hostname=car-ledger.example.com \
    --set ingress.enabled=false --set httpRoute.enabled=true
  # the map matching on its own
  render map-matching
  # the stack without, with Keycloak and with authentik
  render stack --set global.host=example.com
  render stack --set global.host=example.com --set global.keycloak.enabled=true
  render stack --set global.host=example.com --set global.authentik.enabled=true
  # with the map matching, the backups and the metrics
  render stack --set global.host=example.com --set global.mapMatching.enabled=true \
    --set app.backup.enabled=true --set app.backup.s3.bucket=backups --set app.backup.s3.existingSecret=s3 \
    --set app.carLedger.metrics.serviceMonitor.enabled=true \
    --set 'app.networkPolicy.managementFrom[0].podSelector.matchLabels.app=prometheus'
} | kubeconform -strict -summary -kubernetes-version "$KUBERNETES_VERSION" \
  -schema-location default -schema-location "$CRDS"
