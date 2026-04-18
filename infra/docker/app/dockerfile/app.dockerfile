# syntax=docker/dockerfile:1.7

FROM gradle:8.12-jdk17 AS build
WORKDIR /app

# Copy module metadata first so dependency resolution can stay cached.
COPY gradlew ./gradlew
COPY gradle ./gradle
COPY build.gradle ./build.gradle
COPY settings.gradle ./settings.gradle

RUN sed -i 's/\r$//' gradlew && chmod +x gradlew
RUN --mount=type=cache,target=/home/gradle/.gradle \
    ./gradlew dependencies --no-daemon

COPY src ./src

RUN --mount=type=cache,target=/home/gradle/.gradle \
    ./gradlew bootJar --no-daemon \
    && JAR_FILE=$(ls build/libs/*.jar | grep -v -- '-plain\.jar' | head -n 1) \
    && cp "${JAR_FILE}" /app/app.jar

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

COPY --from=build /app/app.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
