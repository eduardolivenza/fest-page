# Java 25 (LTS). Keep the Java major in sync with "java" in apps/api/gradle/libs.versions.toml and
# `java-version` in the workflows (scripts/check-versions.mjs), and both stages on the same exact tag.
FROM eclipse-temurin:25.0.5_8-jdk-alpine AS builder
WORKDIR /build
COPY apps/api .
# Tests run in CI (PR checks / ci-api.yml) against an embedded PostgreSQL; the image only needs the jar.
RUN --mount=type=cache,id=gradle,target=/root/.gradle \
    ./gradlew --no-daemon --console=plain :bootstrap:bootJar \
 && java -Djarmode=tools -jar bootstrap/build/libs/app.jar extract --destination /build/extracted

FROM eclipse-temurin:25.0.5_8-jre-alpine AS runner
RUN addgroup -g 1001 -S app && adduser -S appuser -u 1001 -G app
WORKDIR /app
# Dependencies first: they change less often than the application classes.
COPY --from=builder /build/extracted/lib /app/lib
COPY --from=builder /build/extracted/app.jar /app/app.jar
ENV TZ=UTC
USER appuser
EXPOSE 3001
# Sized for the 512Mi pod limit: heap at 60% of the container memory, serial GC (1 small pod, least overhead).
# The migrate initContainer runs `java -cp app.jar com.festdescans.api.MigrateDatabase` instead (helm/api).
CMD ["java", "-XX:MaxRAMPercentage=60", "-XX:+UseSerialGC", "-Xss512k", "-XX:ReservedCodeCacheSize=64m", \
     "-XX:MaxMetaspaceSize=160m", "-XX:+ExitOnOutOfMemoryError", "-jar", "app.jar"]
