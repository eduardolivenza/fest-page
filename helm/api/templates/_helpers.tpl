{{/*
Database connection for the api and its migrate initContainer (Spring Boot / Flyway env vars).
The password stays in the Secret; nothing is put into a URL.
*/}}
{{- define "api.datasourceEnv" -}}
- name: SPRING_DATASOURCE_URL
  value: "jdbc:postgresql://{{ .Release.Name }}-postgresql:5432/{{ .Values.postgresql.auth.database }}"
- name: SPRING_DATASOURCE_USERNAME
  value: {{ .Values.postgresql.auth.username | quote }}
- name: SPRING_DATASOURCE_PASSWORD
  valueFrom:
    secretKeyRef:
      name: {{ .Release.Name }}
      key: password
{{- end }}
