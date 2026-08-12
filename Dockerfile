# Build stage
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy Maven wrapper & project files
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
COPY mvnw.cmd .
COPY src ./src
COPY package.json .
COPY tsconfig.json .
COPY types.d.ts .
COPY vite.config.ts .

# Package the application in production mode (skipping test suite for faster deployment)
RUN mvn clean package -Pproduction -DskipTests

# Run stage
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENV PORT=5000
EXPOSE 5000
ENTRYPOINT ["java", "-jar", "app.jar"]
