#build app
FROM openjdk:11-jdk

ADD ./ /opt/money-transfer/

ADD docker-build.sh /
RUN chmod ugo+x /docker-build.sh

EXPOSE 8080

ENTRYPOINT ["/docker-build.sh"]
