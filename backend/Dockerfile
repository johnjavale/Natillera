# syntax=docker/dockerfile:1.7
# ---------- 1) Build con Maven + JDK 21 ----------
FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q package -DskipTests

# ---------- 2) Jar extraído + archivo CDS (arranque en frío más rápido) ----------
FROM eclipse-temurin:21-jre-alpine AS cds
WORKDIR /app
COPY --from=build /src/target/natillera-backend-*.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --destination extraido
WORKDIR /app/extraido
# Entrenamiento: levanta el contexto y sale (spring.context.exit=onRefresh) SIN base de datos.
# Mismas opciones de JVM que en ejecución para que el archivo CDS sea compatible.
RUN SPRING_PROFILES_ACTIVE=prod \
    JWT_SECRET=entrenamiento-cds-no-es-un-secreto-real-0123456789 \
    DATABASE_URL=jdbc:mysql://localhost:1/entrenamiento \
    java -Xmx256m -Xss512k -XX:+UseSerialGC -XX:MaxMetaspaceSize=128m -XX:TieredStopAtLevel=1 \
         -XX:ArchiveClassesAtExit=app.jsa -Dspring.context.exit=onRefresh \
         -Dspring.flyway.enabled=false -Dspring.jpa.hibernate.ddl-auto=none \
         -Dspring.jpa.database-platform=org.hibernate.dialect.MySQLDialect \
         -Dspring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=false \
         -Dspring.datasource.hikari.initialization-fail-timeout=-1 \
         -jar app.jar

# ---------- 3) Runtime mínimo, usuario no root ----------
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S natillera && adduser -S natillera -G natillera
WORKDIR /app
COPY --from=cds --chown=natillera:natillera /app/extraido/lib ./lib
COPY --from=cds --chown=natillera:natillera /app/extraido/app.jar ./app.jar
COPY --from=cds --chown=natillera:natillera /app/extraido/app.jsa ./app.jsa
USER natillera

ENV SPRING_PROFILES_ACTIVE=prod \
    TZ=America/Bogota \
    JAVA_TOOL_OPTIONS="-Xmx256m -Xss512k -XX:+UseSerialGC -XX:MaxMetaspaceSize=128m -XX:+ExitOnOutOfMemoryError -XX:TieredStopAtLevel=1 -XX:SharedArchiveFile=/app/app.jsa -Xshare:auto"

EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget -qO- http://localhost:${PORT:-8080}/actuator/health | grep -q UP || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
