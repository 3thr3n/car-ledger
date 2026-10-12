{{/* http or https, for the URLs of CarLedger */}}
{{- define "carLedger.scheme" -}}
{{- if .Values.tls }}https{{ else }}http{{ end }}
{{- end }}

{{/* Service of the authentik server, named like the authentik chart does */}}
{{- define "authentik.serverService" -}}
{{- if contains "authentik" .Release.Name }}{{ .Release.Name }}{{ else }}{{ .Release.Name }}-authentik{{ end }}-server
{{- end }}
