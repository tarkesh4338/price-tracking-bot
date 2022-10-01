FROM openjdk:8-alpine
WORKDIR /opt/app

COPY target/lib /opt/app/lib
COPY target/price-tracker-bot-1.0.jar /opt/app/

ENTRYPOINT [ "java","-jar","price-tracker-bot-1.0.jar"]