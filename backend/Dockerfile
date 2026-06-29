ARG MAVEN_IMAGE=maven:3.9.9-eclipse-temurin-17
ARG JRE_IMAGE=eclipse-temurin:17-jre

FROM ${MAVEN_IMAGE} AS backend-build
WORKDIR /workspace
COPY pom.xml ./
COPY src/main ./src/main
RUN mvn -q -DskipTests package

FROM ${JRE_IMAGE}
WORKDIR /app
COPY --from=backend-build /workspace/target/*.jar /app/api2api.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/api2api.jar"]
