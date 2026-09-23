# Build stage
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY . .
RUN ./gradlew --no-daemon installDist

# Runtime stage
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/build/install/truenasmobile-relay /app
EXPOSE 8080
ENTRYPOINT ["/app/bin/truenasmobile-relay"]