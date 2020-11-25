FROM adoptopenjdk/maven-openjdk11

ENV MAVEN_OPTS -Xms64m -Xmx128m

RUN mkdir -p /usr/local/src/mvnapp
WORKDIR /usr/local/src/mvnapp
ADD . /usr/local/src/mvnapp

RUN mvn --settings settings.xml -Dmaven.test.skip=true clean install dependency:copy-dependencies

WORKDIR /usr/local/src/mvnapp
RUN chmod +x run.sh


CMD ./run.sh it.unimore.dipi.etis.process.EtisProcess

