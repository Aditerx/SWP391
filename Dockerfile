# ─── Stage 1: Build Frontend ─────────────────────────────────────────────────
FROM node:20-alpine AS fe-build
WORKDIR /fe

# Install pnpm
RUN npm install -g pnpm

# Install dependencies (leverages Docker layer cache)
COPY SCMS_demo_fe_v1.1/package.json SCMS_demo_fe_v1.1/pnpm-lock.yaml ./
RUN pnpm install --frozen-lockfile

# Copy source and build
COPY SCMS_demo_fe_v1.1/ .
RUN pnpm build

# ─── Stage 2: Build Backend ───────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17 AS be-build
WORKDIR /app

# Resolve dependencies first to cache the layer
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Copy backend sources + compiled frontend static assets
COPY src/ src/
COPY --from=fe-build /fe/dist src/main/resources/static/

# Build the fat JAR (skip unit tests — tests run in CI before deploy)
RUN mvn package -DskipTests -q

# ─── Stage 3: Runtime ────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Limit heap for Render Free (512 MB RAM) & force IPv4 to avoid Network unreachable errors
ENV JAVA_OPTS="-Xmx400m -Xms200m -Djava.net.preferIPv4Stack=true"

COPY --from=be-build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
