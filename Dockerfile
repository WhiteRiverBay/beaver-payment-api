FROM amazonlinux:2023

RUN yum install -y shadow-utils java-17-amazon-corretto
ENV JAVA_HOME=/usr/lib/jvm/java-17-amazon-corretto
RUN java -version

RUN groupadd -r app && useradd -r -g app app

WORKDIR /java/app

COPY target/*.jar app.jar

RUN chown -R app:app /java/app

USER app

CMD ["java", "-jar", "app.jar", "--server.port=8080", "--spring.profiles.active=prod"]

EXPOSE 8080