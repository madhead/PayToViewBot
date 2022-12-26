FROM openjdk:17-alpine

RUN addgroup -S ptvb && \
    adduser -S -G ptvb ptvb
USER ptvb

ADD --chown=ptvb:ptvb ./build/install/app/ /opt/ptvb/

LABEL maintainer="madhead <siarhei.krukau@gmail.com>"

CMD /opt/ptvb/bin/app
