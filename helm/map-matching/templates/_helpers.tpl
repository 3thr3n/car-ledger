{{/*
Name of the service and its workload: the release name, or <release>-map-matching in the stack chart (helm/stack),
where the app finds it under this name
*/}}
{{- define "mapMatching.name" -}}
{{- if ((.Values.global).mapMatching).enabled }}{{ .Release.Name }}-map-matching{{ else }}{{ .Release.Name }}{{ end -}}
{{- end }}
