# syntax=docker/dockerfile:1.7

FROM gradle:8.12-jdk17 AS build
WORKDIR /app

# Copy root build metadata first so dependency resolution can stay cached.
COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle
COPY Order/build.gradle ./Order/build.gradle

RUN chmod +x gradlew
RUN --mount=type=cache,target=/home/gradle/.gradle \
    ./gradlew :Order:dependencies --no-daemon

COPY Order/src ./Order/src

RUN --mount=type=cache,target=/home/gradle/.gradle \
    ./gradlew :Order:bootJar --no-daemon

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

COPY --from=build /app/Order/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
