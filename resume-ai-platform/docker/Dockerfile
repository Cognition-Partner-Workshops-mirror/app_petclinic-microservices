# Shared multi-stage Dockerfile for all Resume AI Platform microservices.
# Usage: docker build --build-arg MODULE_NAME=resume-ai-config-server --build-arg PORT=8888 -f docker/Dockerfile .

FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
# Copy Maven wrapper and parent POM first for dependency caching
COPY .mvn .mvn
COPY mvnw .
COPY pom.xml .
# Copy all module POMs for dependency resolution
COPY resume-ai-config-server/pom.xml resume-ai-config-server/pom.xml
COPY resume-ai-discovery-server/pom.xml resume-ai-discovery-server/pom.xml
COPY resume-ai-api-gateway/pom.xml resume-ai-api-gateway/pom.xml
COPY resume-ai-resume-service/pom.xml resume-ai-resume-service/pom.xml
COPY resume-ai-interview-service/pom.xml resume-ai-interview-service/pom.xml
COPY resume-ai-profile-enhancer-service/pom.xml resume-ai-profile-enhancer-service/pom.xml

# Build argument for the target module
ARG MODULE_NAME

# Copy source code for the target module
COPY ${MODULE_NAME}/src ${MODULE_NAME}/src

# Build only the target module and its dependencies
RUN chmod +x mvnw && ./mvnw -pl ${MODULE_NAME} -am clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre
WORKDIR /app
ARG MODULE_NAME
ARG PORT=8080
COPY --from=build /app/${MODULE_NAME}/target/*.jar app.jar
EXPOSE ${PORT}
ENTRYPOINT ["java", "-jar", "app.jar"]
