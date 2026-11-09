# One container serves the PWA and the API (used by Render).
# ---- 1. build the PWA ----
FROM node:22-alpine AS web
WORKDIR /web
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY frontend/ ./
# Render passes service env vars as build args when declared here.
ARG VITE_DEV_AUTH=false
ARG VITE_FIREBASE_API_KEY=
ARG VITE_FIREBASE_AUTH_DOMAIN=
ARG VITE_FIREBASE_PROJECT_ID=
ARG VITE_FIREBASE_STORAGE_BUCKET=
ARG VITE_FIREBASE_MESSAGING_SENDER_ID=
ARG VITE_FIREBASE_APP_ID=
ARG VITE_FIREBASE_VAPID_KEY=
# Same origin as the API, so no API URL is needed.
ENV VITE_API_URL=
RUN npm run build

# ---- 2. build the API jar with the PWA inside ----
FROM maven:3.9-eclipse-temurin-21 AS api
WORKDIR /app/backend
COPY backend/pom.xml .
RUN mvn -q -B -Pweb dependency:go-offline
COPY backend/src src
COPY --from=web /web/dist /app/frontend/dist
RUN mvn -q -B -Pweb package -DskipTests

# ---- 3. run ----
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=api /app/backend/target/servicebay-api-*.jar app.jar
# Tuned for small instances (e.g. Render free: 512 MB). Override with JAVA_OPTS.
ENV JAVA_OPTS="-Duser.timezone=UTC -XX:MaxRAMPercentage=70 -XX:+UseSerialGC -Xss512k -XX:TieredStopAtLevel=1 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
