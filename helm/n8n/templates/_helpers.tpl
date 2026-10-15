{{/* http or https, for the URLs of n8n and CarLedger */}}
{{- define "n8n.scheme" -}}
{{- if .Values.tls }}https{{ else }}http{{ end }}
{{- end }}

{{/* Service of n8n, named by n8n.fullnameOverride */}}
{{- define "n8n.service" -}}
{{ .Values.n8n.fullnameOverride }}-main
{{- end }}

{{/* Public host name of n8n: hostname, else n8n.<global.host> of the stack chart */}}
{{- define "n8n.hostname" -}}
{{- .Values.hostname | default (and ((.Values.global).host) (printf "n8n.%s" ((.Values.global).host))) | default "" -}}
{{- end }}

{{/* Public host name of CarLedger: carLedger.hostname, else car-ledger.<global.host> of the stack chart */}}
{{- define "n8n.carLedgerHostname" -}}
{{- .Values.carLedger.hostname | default (and ((.Values.global).host) (printf "car-ledger.%s" ((.Values.global).host))) | default "" -}}
{{- end }}
