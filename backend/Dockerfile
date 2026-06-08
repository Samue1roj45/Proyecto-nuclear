FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn -q -B dependency:go-offline

COPY src ./src
RUN mvn -q -B -DskipTests package

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd -r spring \
    && useradd -r -g spring spring \
    && mkdir -p /data /app/uploads \
    && chown -R spring:spring /data /app/uploads

USER spring:spring

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=90s --retries=5 \
  CMD curl -f http://localhost:8080/api/auth/login -X OPTIONS || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
