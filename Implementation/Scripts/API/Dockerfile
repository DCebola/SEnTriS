FROM tomcat:jdk17-openjdk
WORKDIR /usr/local/tomcat
RUN echo "org.apache.tomcat.util.digester.PROPERTY_SOURCE=org.apache.tomcat.util.digester.EnvironmentPropertySource" >> conf/catalina.properties
ADD Secure-Storage.war webapps