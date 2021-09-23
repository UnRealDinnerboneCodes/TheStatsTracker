FROM gradle:7.0.0-jdk16 as builder

WORKDIR /build

COPY build.gradle /build
COPY src /build/src

RUN gradle shadowJar

FROM openjdk:16
COPY --from=builder /build/build/libs/TheStatsTracker-1.0.0-all.jar DockerJar.jar

CMD ["java", "-jar", "DockerJar.jar"]