{{/* http or https, for the URLs of n8n and CarLedger */}}
{{- define "n8n.scheme" -}}
{{- if .Values.tls }}https{{ else }}http{{ end }}
{{- end }}

{{/* Service of n8n, named by n8n.fullnameOverride */}}
{{- define "n8n.service" -}}
{{ .Values.n8n.fullnameOverride }}-main
{{- end }}
