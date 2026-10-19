#!/usr/bin/env bash
# A local cluster to try the charts with a real login: kind with Cilium (CNI without kube-proxy, enforces the
# NetworkPolicies) and its ingress controller or ingress-nginx, the storage local-path, cert-manager with a self-signed
# CA (like a cluster whose certificates no one trusts) and the stack chart with Keycloak or authentik. Longhorn does not
# work in kind: iscsid needs the network namespace of the host, a kind node has its own.
#
#   hack/kind/up.sh [--idp keycloak|authentik|none] [--ingress cilium|nginx] [--image <repository:tag>]
#                   [--no-build]
#
# The app is https://car-ledger.localhost, the identity provider https://keycloak.localhost or
# https://authentik.localhost: ports 80 and 443 of this machine, *.localhost is 127.0.0.1 without a DNS server. Inside
# the cluster the same names point to the ingress, so the app reaches the identity provider under its public address.
# The CA is written to target/kind/ca.crt.
# Without --image the image is built from the working copy. Running it again upgrades the stack; hack/kind/down.sh
# deletes the cluster. Needs docker, kind, kubectl and helm.
set -euo pipefail
cd "$(dirname "$0")/../.."

CLUSTER=car-ledger
HOST=localhost
NAMESPACE=car-ledger
NODE_IMAGE=kindest/node:v1.37.0@sha256:a1ed56cfb0e7b93589bdf97c8cd566405a265939e3620fc4f5de89adff580ae5
CILIUM_VERSION=1.20.2
CERT_MANAGER_VERSION=v1.21.2
INGRESS_NGINX_VERSION=4.15.1
OUT=target/kind

idp=keycloak ingress=cilium image="" build=true
while [ $# -gt 0 ]; do
  case "$1" in
    --idp) idp="$2"; shift ;;
    --ingress) ingress="$2"; shift ;;
    --image) image="$2"; build=false; shift ;;
    --no-build) build=false ;;
    *) sed -n '2,13p' "$0" >&2; exit 1 ;;
  esac
  shift
done
case "$idp" in keycloak | authentik | none) ;; *) echo "unknown --idp $idp" >&2; exit 1 ;; esac
case "$ingress" in cilium | nginx) ;; *) echo "unknown --ingress $ingress" >&2; exit 1 ;; esac
image="${image:-car-ledger:kind}"

step() { echo "==> $*" >&2; }
mkdir -p "$OUT"

if ! kind get clusters 2>/dev/null | grep -qx "$CLUSTER"; then
  step "Cluster $CLUSTER"
  # the ingress is a NodePort service, ports 80 and 443 of this machine lead to it
  kind create cluster --name "$CLUSTER" --image "$NODE_IMAGE" --wait 0s --config - <<EOF
kind: Cluster
apiVersion: kind.x-k8s.io/v1alpha4
networking:
  disableDefaultCNI: true
  kubeProxyMode: none
nodes:
  - role: control-plane
    extraPortMappings:
      - containerPort: 30080
        hostPort: 80
        listenAddress: 127.0.0.1
      - containerPort: 30443
        hostPort: 443
        listenAddress: 127.0.0.1
EOF
fi
kubectl config use-context "kind-$CLUSTER" >/dev/null
kc() { kubectl --context "kind-$CLUSTER" "$@"; }
hc() { helm --kube-context "kind-$CLUSTER" "$@"; }

step "Cilium $CILIUM_VERSION ($ingress ingress)"
cilium_ingress=()
[ "$ingress" = cilium ] && cilium_ingress=(
  --set ingressController.enabled=true --set ingressController.loadbalancerMode=shared
  --set ingressController.service.type=NodePort
  --set ingressController.service.insecureNodePort=30080 --set ingressController.service.secureNodePort=30443
)
hc upgrade --install cilium cilium --repo https://helm.cilium.io --version "$CILIUM_VERSION" \
  --namespace kube-system --wait --timeout 10m \
  --set kubeProxyReplacement=true --set k8sServiceHost="$CLUSTER-control-plane" --set k8sServicePort=6443 \
  --set ipam.mode=kubernetes --set operator.replicas=1 "${cilium_ingress[@]}"
kc wait --for=condition=Ready node --all --timeout=5m >/dev/null

if [ "$ingress" = nginx ]; then
  step "ingress-nginx $INGRESS_NGINX_VERSION"
  hc upgrade --install ingress-nginx ingress-nginx --repo https://kubernetes.github.io/ingress-nginx \
    --version "$INGRESS_NGINX_VERSION" --namespace ingress-nginx --create-namespace --wait --timeout 10m \
    --set controller.service.type=NodePort \
    --set controller.service.nodePorts.http=30080 --set controller.service.nodePorts.https=30443
  ingress_service=ingress-nginx/ingress-nginx-controller
else
  ingress_service=kube-system/cilium-ingress
fi

# the public names of the stack point to the ingress inside the cluster as well
step "DNS of *.$HOST inside the cluster"
ingress_ip=$(kc -n "${ingress_service%/*}" get service "${ingress_service#*/}" -o jsonpath='{.spec.clusterIP}')
kc -n kube-system get configmap coredns -o jsonpath='{.data.Corefile}' |
  sed '/# car-ledger kind {/,/# car-ledger kind }/d' |
  awk -v host="$HOST" -v ip="$ingress_ip" '
    /^\.:53 \{/ && !done {
      print
      print "    # car-ledger kind {"
      print "    template IN A " host " {"
      print "        match \"\\." host "\\.$\""
      print "        answer \"{{ .Name }} 60 IN A " ip "\""
      print "        fallthrough"
      print "    }"
      print "    template IN AAAA " host " {"
      print "        match \"\\." host "\\.$\""
      print "        rcode NOERROR"
      print "        fallthrough"
      print "    }"
      print "    # car-ledger kind }"
      done = 1
      next
    }
    { print }' >"$OUT/Corefile"
kc -n kube-system create configmap coredns --from-file=Corefile="$OUT/Corefile" --dry-run=client -o yaml |
  kc replace -f - >/dev/null
kc -n kube-system rollout restart deployment coredns >/dev/null
kc -n kube-system rollout status deployment coredns --timeout=2m >/dev/null

step "cert-manager $CERT_MANAGER_VERSION and the CA"
hc upgrade --install cert-manager cert-manager --repo https://charts.jetstack.io --version "$CERT_MANAGER_VERSION" \
  --namespace cert-manager --create-namespace --wait --timeout 10m --set crds.enabled=true
kc apply -f - >/dev/null <<EOF
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: self-signed
spec:
  selfSigned: {}
---
apiVersion: cert-manager.io/v1
kind: Certificate
metadata:
  name: car-ledger-ca
  namespace: cert-manager
spec:
  isCA: true
  commonName: CarLedger kind CA
  secretName: car-ledger-ca
  privateKey:
    algorithm: ECDSA
    size: 256
  issuerRef:
    kind: ClusterIssuer
    name: self-signed
---
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: car-ledger-ca
spec:
  ca:
    secretName: car-ledger-ca
EOF
kc -n cert-manager wait --for=condition=Ready certificate/car-ledger-ca --timeout=2m >/dev/null
kc -n cert-manager get secret car-ledger-ca -o jsonpath='{.data.ca\.crt}' | base64 -d >"$OUT/ca.crt"

if $build; then
  step "Image $image"
  ./mvnw -B -q package -DskipTests -Djacoco.skip=true
  docker build -q -f src/main/docker/Dockerfile.jvm -t "$image" . >/dev/null
fi
if docker image inspect "$image" >/dev/null 2>&1; then
  kind load docker-image "$image" --name "$CLUSTER" >/dev/null
fi

step "Stack (identity provider: $idp)"
helm dependency update helm/car-ledger >/dev/null
helm dependency update helm/stack >/dev/null
values=(
  --set global.host="$HOST"
  --set app.carLedger.image.repository="${image%:*}" --set app.carLedger.image.tag="${image##*:}"
  --set app.ingress.className="$ingress" --set app.ingress.certManager=car-ledger-ca
)
case "$idp" in
  keycloak) values+=(
    --set global.keycloak.enabled=true
    --set keycloak.keycloak.ingress.ingressClassName="$ingress"
    --set keycloak.keycloak.ingress.annotations.cert-manager\\.io/cluster-issuer=car-ledger-ca
  ) ;;
  authentik) values+=(
    --set global.authentik.enabled=true
    --set authentik.ingress.className="$ingress" --set authentik.ingress.certManager=car-ledger-ca
  ) ;;
esac
hc upgrade --install car-ledger helm/stack --namespace "$NAMESPACE" --create-namespace \
  --wait --timeout 15m "${values[@]}"

if [ "$idp" = authentik ]; then
  # the worker applies the blueprint with the application after the start
  step "Application car-ledger in authentik"
  until curl -skfo /dev/null "https://authentik.$HOST/application/o/car-ledger/.well-known/openid-configuration"; do
    sleep 5
  done
fi
if [ "$idp" = keycloak ]; then
  # a user of the app (role user by default) and an administrator, kept if they exist
  step "Users alice and admin in the realm car-ledger"
  admin_password=$(kc -n "$NAMESPACE" get secret car-ledger-keycloak-admin -o jsonpath='{.data.password}' | base64 -d)
  kc -n "$NAMESPACE" exec -i car-ledger-keycloak-0 -- env "ADMIN_PASSWORD=$admin_password" bash -s >/dev/null <<'EOF2'
set -e
kcadm() { /opt/keycloak/bin/kcadm.sh "$@" --config /tmp/kcadm.json; }
kcadm config credentials --server http://localhost:8080 --realm master --user car-ledger --password "$ADMIN_PASSWORD"
for user in alice admin; do
  if [ -z "$(kcadm get users -r car-ledger -q "username=$user" -q exact=true --fields id --format csv)" ]; then
    kcadm create users -r car-ledger -s "username=$user" -s "email=$user@example.com" -s "firstName=${user^}" \
      -s lastName=Kind -s emailVerified=true -s enabled=true
    kcadm set-password -r car-ledger --username "$user" --new-password "$user"
  fi
done
kcadm add-roles -r car-ledger --uusername admin --rolename admin
EOF2
fi

cat >&2 <<EOF

CarLedger:   https://car-ledger.$HOST
EOF
case "$idp" in
  keycloak) cat >&2 <<EOF
Users:       alice (password alice) and admin (password admin, role admin)
Keycloak:    https://keycloak.$HOST (user car-ledger, password:
             kubectl -n $NAMESPACE get secret car-ledger-keycloak-admin -o jsonpath='{.data.password}' | base64 -d)
EOF
  ;;
  authentik) cat >&2 <<EOF
authentik:   https://authentik.$HOST (user akadmin, group admin, password:
             kubectl -n $NAMESPACE get secret authentik-bootstrap -o jsonpath='{.data.password}' | base64 -d)
EOF
  ;;
esac
cat >&2 <<EOF
CA:          $OUT/ca.crt (import it into the browser, or accept the warning)
EOF
