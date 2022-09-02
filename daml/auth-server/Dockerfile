FROM amazoncorretto:17-alpine-jdk as build

ADD gradle gradle
ADD src src
ADD gradlew gradlew
ADD build.gradle.kts build.gradle.kts
ADD settings.gradle.kts settings.gradle.kts
ADD gradle.properties gradle.properties

RUN ./gradlew installDist

FROM amazoncorretto:17-alpine

COPY --from=build build/install/auth-server /app

ENTRYPOINT ["/app/bin/auth-server"]
