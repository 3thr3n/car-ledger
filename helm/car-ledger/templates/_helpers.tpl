{{/*
Values of the stack chart (helm/stack), empty when this chart is installed on its own: global.host is the base
domain, global.authentik.enabled and global.n8n.enabled switch the defaults of the login and the webhooks to the
charts deployed next to the app.
*/}}

{{/* Base domain, the app is reachable as car-ledger.<host> */}}
{{- define "carLedger.host" -}}
{{- .Values.ingress.host | default ((.Values.global).host) | default "" -}}
{{- end }}

{{- define "carLedger.withAuthentik" -}}
{{- if ((.Values.global).authentik).enabled }}true{{ end -}}
{{- end }}

{{- define "carLedger.withN8n" -}}
{{- if ((.Values.global).n8n).enabled }}true{{ end -}}
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
