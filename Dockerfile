FROM gradle:8.10.0-jdk21 as builder

WORKDIR /build

COPY build.gradle /build
COPY gradle.properties /build
COPY src /build/src

RUN gradle shadowJar
RUN ls -l /build/build/libs/

FROM openjdk:21-alpine
COPY --from=builder "/build/build/libs/build-3.0.0-all.jar" "ApolloStats-1.0.0-all.jar"


CMD ["java", "-jar", "ApolloStats-1.0.0-all.jar"]
