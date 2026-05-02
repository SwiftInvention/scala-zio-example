FROM sbtscala/scala-sbt:eclipse-temurin-21.0.8_9_1.12.10_2.13.18 AS builder

COPY . . 

# TODO: the naive build with a proper multi-stage build
RUN sbt compile

EXPOSE 8080

CMD ["sbt", "scalaZioExample/run"]

