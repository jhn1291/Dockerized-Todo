FROM openjdk:11-jre-slim

WORKDIR /

COPY target/challenge-0.1.0-SNAPSHOT-standalone.jar challenge.jar

ENV PORT 3001

EXPOSE 3001

CMD java -jar challenge.jar 
