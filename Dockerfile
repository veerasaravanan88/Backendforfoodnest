# Stage 1: Build application
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

# Copy Maven wrapper and POM configurations
COPY mvnw mvnw.cmd pom.xml ./
COPY .mvn .mvn
COPY backend/pom.xml backend/pom.xml

# Ensure execution rights on wrapper
RUN chmod +x ./mvnw

# Cache dependencies
RUN ./mvnw dependency:go-offline -B -DskipTests || true

# Copy backend source code and build executable jar
COPY backend/src backend/src
RUN ./mvnw clean package -DskipTests -B

# Stage 2: Minimal runtime image
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Copy executable jar from build stage
COPY --from=build /app/backend/target/*.jar app.jar

ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
