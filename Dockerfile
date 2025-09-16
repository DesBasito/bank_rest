FROM maven:3.9.8-amazoncorretto-17 AS build
WORKDIR /build

COPY pom.xml ./

RUN mvn dependency:go-offline -B

COPY src ./src

RUN mvn clean package -e -DskipTests

FROM amazoncorretto:17
WORKDIR /app

COPY --from=build /build/target/bank-cards*jar ./bank-cards.jar
COPY ./config /app/config

EXPOSE 9778

CMD ["java", "-jar", "bank-cards.jar", "--spring.config.location=file:/app/config/application-prod.yaml"]