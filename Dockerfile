FROM eclipse-temurin:22-jdk-alpine
WORKDIR /app
COPY backend/.mvn/ .mvn
COPY backend/mvnw backend/pom.xml ./
RUN chmod +x ./mvnw
COPY backend/src ./src
RUN ./mvnw package -DskipTests -B
EXPOSE 8080
CMD ["java", "-jar", "target/backend-0.0.1-SNAPSHOT.jar"]
