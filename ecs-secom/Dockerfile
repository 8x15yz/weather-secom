# ── Stage 1: Build ───────────────────────────────────
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn package -DskipTests -B

# ── Stage 2: Runtime ─────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

RUN addgroup -S secom && adduser -S secom -G secom
USER secom

COPY --from=builder /build/target/optimal-ecs-secom-*.jar app.jar

EXPOSE 8767

ENTRYPOINT ["java", "-Xms256m", "-Xmx512m", "-jar", "app.jar"]