FROM maven:3-jdk-8

WORKDIR /usr/src/app
COPY . /usr/src/app

RUN mvn package -Dmaven.test.skip=true

VOLUME /usr/src/app/indexdbpedia_en_2014

EXPOSE 8080

CMD ["mvn",  "tomcat:run",  "-Dmaven.tomcat.port=8080"]
