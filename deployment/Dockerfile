# HU5: imagen de CrediYa en dos etapas.
# 1) build: compila el jar con el Gradle Wrapper del proyecto (no hace falta tener Java en la máquina).
# 2) runtime: solo el JRE y el jar, con un usuario sin privilegios.
# Se construye desde la raíz del repositorio: docker compose build (o docker build -f deployment/Dockerfile .)

FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /workspace

# Primero el wrapper y la configuración de Gradle, para que la descarga de Gradle quede en caché de capas.
COPY gradlew settings.gradle build.gradle gradle.properties lombok.config ./
COPY gradle gradle
# Quita CRLF por si el repositorio se clonó en Windows con core.autocrlf=true.
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew && ./gradlew --version --no-daemon -q

COPY domain domain
COPY infrastructure infrastructure
COPY applications applications
# Las pruebas (JUnit + pitest + jacoco) se corren fuera de la imagen; aquí solo se empaqueta.
RUN ./gradlew :app-service:bootJar --no-daemon -q -x test

FROM eclipse-temurin:17-jre-alpine AS runtime
RUN addgroup -S appuser && adduser -S -G appuser appuser
WORKDIR /app
COPY --from=build --chown=appuser:appuser /workspace/applications/app-service/build/libs/CrediYa.jar CrediYa.jar

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=70 -Djava.security.egd=file:/dev/./urandom"
USER appuser
EXPOSE 8080

# /actuator/health es público en SecurityConfig; wget viene en la imagen alpine (busybox).
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 \
    CMD wget -q -O /dev/null http://localhost:8080/actuator/health || exit 1

ENTRYPOINT [ "/bin/sh", "-c", "exec java $JAVA_OPTS -jar /app/CrediYa.jar" ]
