#build app
FROM openjdk:11-jdk

ADD ./ /opt/money-transfer/

ADD docker-build.sh /
RUN chmod ugo+x /docker-build.sh

ENTRYPOINT ["/docker-build.sh"]

# run app
FROM openjdk:11-jre-slim

COPY --from=0 /opt/money-transfer/build/distributions/money-transfer-0.0.1.tar .
ADD docker-entrypoint.sh /
RUN chmod ugo+x /docker-entrypoint.sh

EXPOSE 8080

ENTRYPOINT ["/docker-entrypoint.sh"]
