{{/*
Values of the stack chart (helm/stack), empty when this chart is installed on its own: global.host is the base
domain, global.authentik.enabled switches the defaults of the login to the chart deployed next to the app.
*/}}

{{/* Base domain, the app is reachable as car-ledger.<host> */}}
{{- define "carLedger.host" -}}
{{- .Values.ingress.host | default ((.Values.global).host) | default "" -}}
{{- end }}

{{- define "carLedger.withAuthentik" -}}
{{- if ((.Values.global).authentik).enabled }}true{{ end -}}
{{- end }}

{{/* Issuer: carLedger.auth.url, else authentik or Keycloak at <name>.<host> */}}
{{- define "carLedger.authUrl" -}}
{{- if .Values.carLedger.auth.url }}
{{- .Values.carLedger.auth.url }}
{{- else if include "carLedger.withAuthentik" . }}
{{- printf "https://authentik.%s/application/o/car-ledger/" (include "carLedger.host" .) }}
{{- else }}
{{- printf "https://keycloak.%s/realms/car-ledger" (include "carLedger.host" .) }}
{{- end }}
{{- end }}

{{- define "carLedger.authClientId" -}}
{{- .Values.carLedger.auth.clientId | default (ternary "car-ledger" "carLedger" (eq (include "carLedger.withAuthentik" .) "true")) -}}
{{- end }}

{{/* Secret with the client secret and the state secret: car-ledger-oidc of the authentik chart, else keycloak */}}
{{- define "carLedger.authSecret" -}}
{{- .Values.carLedger.auth.existingSecret | default (ternary "car-ledger-oidc" "keycloak" (eq (include "carLedger.withAuthentik" .) "true")) -}}
{{- end }}

{{/* Sources of the projected volume with the certificates to trust (carLedger.trust), empty without any */}}
{{- define "carLedger.trustSources" -}}
{{- if and .Values.carLedger.trust.ingressCA .Values.ingress.enabled }}
- secret:
    name: car-ledger-ingress-tls
    # created by cert-manager after the ingress, without a CA (e.g. a public issuer) it has no ca.crt
    optional: true
    items:
      - key: ca.crt
        path: ingress-ca.crt
{{- if not .Values.carLedger.auth.url }}
# the ingress of the identity provider of the charts (keycloak.<host> or authentik), maybe of another issuer
- secret:
    name: {{ ternary "authentik-ingress-tls" (printf "keycloak.%s-tls" (include "carLedger.host" .)) (eq (include "carLedger.withAuthentik" .) "true") }}
    optional: true
    items:
      - key: ca.crt
        path: login-ca.crt
{{- end }}
{{- end }}
{{- range $index, $certificate := .Values.carLedger.trust.certificates }}
{{- $key := $certificate.key | default "ca.crt" }}
{{- if $certificate.secret }}
- secret:
    name: {{ $certificate.secret }}
{{- else }}
- configMap:
    name: {{ required "carLedger.trust.certificates needs a secret or a configMap" $certificate.configMap }}
{{- end }}
    items:
      - key: {{ $key }}
        path: {{ printf "%d-%s" $index $key }}
{{- end }}
{{- end }}
