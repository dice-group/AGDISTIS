FROM tomcat:8-jre8

# Copy war file
COPY target/AGDISTIS*.war webapps/AGDISTIS.war
