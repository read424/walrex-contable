# ──────────────────────────────────────────────────────────────────────────────
# Stage 1 — Build
# Se descarta al finalizar; no añade peso a la imagen final.
# ──────────────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

# 1. Copiar wrapper y pom primero → capa cacheable mientras pom no cambie
COPY mvnw .
COPY .mvn/ .mvn/
COPY pom.xml .

# 2. Descargar dependencias (se reutiliza en builds siguientes si pom no cambia)
RUN ./mvnw dependency:go-offline -q

# 3. Copiar fuentes y compilar
COPY src/ src/
RUN ./mvnw package -DskipTests -q

# ──────────────────────────────────────────────────────────────────────────────
# Stage 2 — Runtime
# Solo JRE Alpine (~85 MB base). Sin JDK, sin Maven, sin fuentes.
# ──────────────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

# Usuario no-root para ejecutar la app
RUN addgroup -S walrex && adduser -S walrex -G walrex

WORKDIR /app

# Directorio para uploads temporales (configurado en application.yml)
RUN mkdir -p tmp/uploads && chown -R walrex:walrex /app

# Capas del fast-jar de Quarkus ordenadas de menos a más cambiante:
#   lib/     → dependencias externas  (cambia solo si cambia pom.xml)
#   quarkus/ → framework Quarkus      (cambia solo si cambia la versión)
#   app/     → código de la aplicación (cambia en cada build)
COPY --from=builder --chown=walrex:walrex /build/target/quarkus-app/lib/     lib/
COPY --from=builder --chown=walrex:walrex /build/target/quarkus-app/quarkus/ quarkus/
COPY --from=builder --chown=walrex:walrex /build/target/quarkus-app/app/     app/
COPY --from=builder --chown=walrex:walrex /build/target/quarkus-app/quarkus-run.jar quarkus-run.jar

USER walrex

EXPOSE 8089

ENV JAVA_OPTS="-Dquarkus.http.host=0.0.0.0 \
               -Djava.util.logging.manager=org.jboss.logmanager.LogManager \
               -XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar quarkus-run.jar"]
