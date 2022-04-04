FROM tomcat:9.0.62-jdk17-openjdk
WORKDIR /usr/local/tomcat
ARG VERSION
ADD target/Secure-Storage-${VERSION}.war webapps
EXPOSE 8080
