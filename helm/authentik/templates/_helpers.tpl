{{/* http or https, for the URLs of CarLedger */}}
{{- define "carLedgerAuthentik.scheme" -}}
{{- if .Values.tls }}https{{ else }}http{{ end }}
{{- end }}

{{/* Service of the authentik server, named like the authentik chart does */}}
{{- define "authentik.serverService" -}}
{{- if contains "authentik" .Release.Name }}{{ .Release.Name }}{{ else }}{{ .Release.Name }}-authentik{{ end }}-server
{{- end }}

{{/*
Prefix of the PostgreSQL: the release name, or <release>-authentik in the stack chart (helm/stack), whose release
holds the PostgreSQL of the app as well. Also used in the values of the authentik chart.
*/}}
{{- define "carLedgerAuthentik.prefix" -}}
{{- if ((.Values.global).authentik).enabled }}{{ .Release.Name }}-authentik{{ else }}{{ .Release.Name }}{{ end -}}
{{- end }}

{{/* Public host name of authentik: hostname, else authentik.<global.host> of the stack chart */}}
{{- define "carLedgerAuthentik.hostname" -}}
{{- .Values.hostname | default (and ((.Values.global).host) (printf "authentik.%s" ((.Values.global).host))) | default "" -}}
{{- end }}

{{/* Public host name of CarLedger: carLedger.hostname, else car-ledger.<global.host> of the stack chart */}}
{{- define "carLedgerAuthentik.carLedgerHostname" -}}
{{- .Values.carLedger.hostname | default (and ((.Values.global).host) (printf "car-ledger.%s" ((.Values.global).host))) | default "" -}}
{{- end }}
