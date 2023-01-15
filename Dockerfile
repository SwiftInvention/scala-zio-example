FROM sbtscala/scala-sbt:eclipse-temurin-11.0.17_8_1.8.2_2.13.10 as builder

COPY . . 

# TODO: the naive build with a proper multi-stage build
RUN sbt compile

EXPOSE 8080

CMD ["sbt", "scalaZioExample/run"]

