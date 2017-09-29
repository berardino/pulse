FROM openjdk:8-jdk-alpine

ENV SERVICE_NAME     pulse
ENV USER             pulse
ENV PORT             8080
ENV LC_ALL           C.UTF-8

COPY maven /

WORKDIR /opt/$SERVICE_NAME
RUN mkdir -p /etc/$SERVICE_NAME /var/log/$SERVICE_NAME
RUN addgroup pulse && adduser -D -G pulse pulse
RUN chown 8080:5000 /etc/$SERVICE_NAME /var/log/$SERVICE_NAME

VOLUME ["/etc/pulse"]
VOLUME ["/var/log/pulse"]

USER $USER:pulse
EXPOSE $PORT

CMD ["java","-jar","bin/pulse.jar","-server","-Xmx256m","-Xms256m","-XX:+AlwaysPreTouch","-XX:+AggressiveOpts","-XX:+UseCompressedOops"]
