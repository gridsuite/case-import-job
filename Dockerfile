FROM openjdk:11
WORKDIR /
ADD target/case-import-job-1.0-SNAPSHOT-jar-with-dependencies.jar case-import-job.jar
EXPOSE 8080
CMD java -jar case-import-job.jar