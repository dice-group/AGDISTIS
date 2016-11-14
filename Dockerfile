FROM maven:3-jdk-8-alpine

# Reassign Maven cache folder to /cache/maven
# since maven base images use volumes for cache
# and we want static builds
ENV MAVEN_OPTS "-Dmaven.repo.local=/cache/maven"

# Set main workdir
WORKDIR /usr/src/app

# Copy pom file and download all deps
COPY pom.xml /usr/src/app
RUN mvn dependency:go-offline

# Copy source folder and install app
COPY src /usr/src/app/src
RUN mvn compile -Dmaven.test.skip=true

# Expose default app index folder as volume
VOLUME /usr/src/app/indexdbpedia_en_2014

# Expose port
EXPOSE 8080

# Set default command to run tomcat in offline (-o) mode
CMD ["mvn", "-o",  "tomcat7:run"]
