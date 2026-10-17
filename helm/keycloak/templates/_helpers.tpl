{{/*
Prefix of the PostgreSQL and the admin secret: the release name, or <release>-keycloak in the stack chart
(helm/stack), whose release holds the PostgreSQL of the app as well. Also used in the values of keycloakx.
*/}}
{{- define "carLedgerKeycloak.prefix" -}}
{{- if ((.Values.global).keycloak).enabled }}{{ .Release.Name }}-keycloak{{ else }}{{ .Release.Name }}{{ end -}}
{{- end }}

{{/* Public host name, used in the values of keycloakx: hostname, else keycloak.<global.host> of the stack chart */}}
{{- define "carLedgerKeycloak.hostname" -}}
{{- .Values.hostname | default (and ((.Values.global).host) (printf "keycloak.%s" ((.Values.global).host))) | default "" -}}
{{- end }}

{{/* Public host name of CarLedger: carLedger.hostname, else car-ledger.<global.host> of the stack chart */}}
{{- define "carLedgerKeycloak.carLedgerHostname" -}}
{{- .Values.carLedger.hostname | default (and ((.Values.global).host) (printf "car-ledger.%s" ((.Values.global).host))) | default "" -}}
{{- end }}
