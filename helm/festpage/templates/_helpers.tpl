{{/*
Chart label: name-version
*/}}
{{- define "festpage.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels applied to every resource.
*/}}
{{- define "festpage.labels" -}}
helm.sh/chart: {{ include "festpage.chart" . }}
app.kubernetes.io/name: {{ .Chart.Name }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels for a component.
Usage: {{ include "festpage.selectorLabels" (dict "root" . "component" "web") }}
*/}}
{{- define "festpage.selectorLabels" -}}
app.kubernetes.io/name: {{ .root.Chart.Name }}
app.kubernetes.io/instance: {{ .root.Release.Name }}
app.kubernetes.io/component: {{ .component }}
{{- end }}

{{/*
Full image reference with optional global registry prefix.
Usage: {{ include "festpage.image" (dict "registry" .Values.global.imageRegistry "repo" .Values.web.image.repository "tag" .Values.web.image.tag) }}
*/}}
{{- define "festpage.image" -}}
{{- if .registry -}}
{{- printf "%s/%s:%s" .registry .repo .tag }}
{{- else -}}
{{- printf "%s:%s" .repo .tag }}
{{- end }}
{{- end }}

{{/*
PostgreSQL host — matches the service name created by the bitnami subchart.
*/}}
{{- define "festpage.postgresHost" -}}
{{- printf "%s-postgresql" .Release.Name }}
{{- end }}
