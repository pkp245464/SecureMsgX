# Stage 1: Build the application
FROM maven:3.8.5-openjdk-21 AS build

# Set the working directory
WORKDIR /build

# Copy the project files
COPY . .

# Build the project
RUN mvn clean package -DskipTests

# Stage 2: Package the application into the final image
FROM eclipse-temurin:21-jdk-alpine

# Set the working directory
WORKDIR /app

# Copy the built jar file from the previous stage
COPY --from=build /build/target/logistics-0.0.1-SNAPSHOT.jar app.jar

# Expose the port the application runs on
EXPOSE 8089

# Define the command to run the application
CMD ["java", "-jar", "app.jar"]
