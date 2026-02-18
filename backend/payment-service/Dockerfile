FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN apk add --no-cache maven && mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S eventhub && adduser -S eventhub -G eventhub
ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.4.0/opentelemetry-javaagent.jar /app/opentelemetry-javaagent.jar
RUN chmod 644 /app/opentelemetry-javaagent.jar
COPY --from=build /app/target/*.jar app.jar
USER eventhub
EXPOSE 8086
HEALTHCHECK --interval=30s --timeout=3s CMD wget -qO- http://localhost:8086/actuator/health || exit 1
ENV OTEL_SERVICE_NAME=payment-service
ENV OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-collector:4317
ENV OTEL_METRICS_EXPORTER=prometheus
ENV OTEL_TRACES_EXPORTER=otlp
ENTRYPOINT ["java", "-javaagent:/app/opentelemetry-javaagent.jar", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
