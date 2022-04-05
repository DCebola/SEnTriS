FROM tomcat:9.0.62-jdk17-openjdk
WORKDIR /usr/local/tomcat
COPY server.xml /usr/local/tomcat/conf/
COPY ssl /usr/local/tomcat/ssl
ARG VERSION
ADD target/Secure-Storage-${VERSION}.war webapps
EXPOSE 8443
