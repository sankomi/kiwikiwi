FROM maven:3-eclipse-temurin-17 as build
WORKDIR /kiwi
COPY pom.xml pom.xml
RUN mvn verify --fail-never
COPY src src
RUN mvn package -Dmaven.test.skip

FROM ubuntu:lunar
WORKDIR /kiwi
RUN apt-get update && apt-get install -y openjdk-17-jre
COPY --from=build /kiwi/target/kiwi-0.0.1.jar kiwi.jar
EXPOSE 8080
CMD ["java", "-jar", "kiwi.jar"]
