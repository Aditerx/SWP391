# ─── Stage 1: Build Backend ───────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17 AS be-build
WORKDIR /app

# Resolve dependencies first to cache the layer
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Copy backend sources
COPY src/ src/

# Build the fat JAR (skip unit tests — tests run in CI before deploy)
RUN mvn package -DskipTests -q

# ─── Stage 2: Runtime ────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Limit heap for Render Free (512 MB RAM) & force IPv4 to avoid Network unreachable errors
ENV JAVA_OPTS="-Xmx400m -Xms200m -Djava.net.preferIPv4Stack=true"

COPY --from=be-build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
