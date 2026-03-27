# Use Java 17
FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app

# Copy Maven wrapper and pom.xml from the nested folder
COPY backend/thecapo/mvnw .
COPY backend/thecapo/.mvn .mvn
COPY backend/thecapo/pom.xml .

# Download dependencies
RUN ./mvnw dependency:go-offline -B

# Copy project source
COPY backend/thecapo/src src

# Build the app
RUN ./mvnw package -DskipTests

# Expose port
ENV PORT 8080
EXPOSE $PORT

# Start the app
CMD java -jar target/*.jar