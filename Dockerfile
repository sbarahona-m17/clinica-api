FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline
COPY src ./src
RUN mvn -B -ntp clean verify

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN groupadd --system spring && useradd --system --gid spring spring && mkdir /app/data && chown spring:spring /app/data
COPY --from=build --chown=spring:spring /app/target/clinica-api.jar app.jar
USER spring:spring
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70.0"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
