FROM openjdk:8u131-jre-alpine

COPY target/lib/* /app/lib/
COPY target/metochi-client-0.1.jar /app/app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
