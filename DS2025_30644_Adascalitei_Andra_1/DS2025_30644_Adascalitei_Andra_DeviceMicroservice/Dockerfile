FROM  maven:3.9.11 AS build

ENV TZ=UTC
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
ENV CATALINA_OPTS="-Duser.timezone=UTC"

COPY ../pom.xml /device/pom.xml
WORKDIR /device
RUN mvn dependency:go-offline
COPY ./src/ /device/src
WORKDIR /device
RUN mvn clean install -DskipTests

FROM eclipse-temurin:21-jdk
WORKDIR /device
COPY --from=build /device/target/DS2025_30644_Adascalitei_Andra_DeviceMicroservice-1.0-SNAPSHOT.jar /device/device.jar
EXPOSE 8082
CMD ["java", "-jar", "device.jar"]
