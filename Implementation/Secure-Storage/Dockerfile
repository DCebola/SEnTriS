FROM tomcat:jdk17-openjdk
WORKDIR /usr/local/tomcat
COPY server.xml conf
COPY ssl ssl
ADD target/Secure-Storage.war webapps
EXPOSE 8443
