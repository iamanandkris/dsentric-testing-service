FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml ./
COPY vendor ./vendor
COPY src ./src

RUN mvn -q -Dmaven.repo.local=/tmp/dsentric-m2 package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /workspace/target/dsentric-testing-service-0.0.1-SNAPSHOT.jar /app/service.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/service.jar"]
