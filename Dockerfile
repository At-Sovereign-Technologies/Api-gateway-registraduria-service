# syntax=docker/dockerfile:1
# Deprecated: Spring Cloud Gateway — production edge is Caddy (see Caddyfile + docker-compose.yml).
# Kept for transitional builds; use docker-compose.legacy-gateway.yml to run this image.

FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml .
COPY src ./src

RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -g 1000 app && adduser -u 1000 -G app -S app

COPY --from=build /workspace/target/*.jar /app/app.jar
RUN chown app:app /app/app.jar

USER app

EXPOSE 8092

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
