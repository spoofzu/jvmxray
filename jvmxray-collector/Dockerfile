FROM openjdk:14

COPY ./target/jvmxray-collector-0.0.1-jar-with-dependencies.jar /usr/app/

WORKDIR /usr/app
EXPOSE 9123

ENTRYPOINT ["java", "-jar", "jvmxray-collector-0.0.1-jar-with-dependencies.jar"]
