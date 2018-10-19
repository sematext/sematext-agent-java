FROM ubuntu:latest
LABEL maintainer="Sematext"

ENV BASE_DIR /opt/spm
ENV FPM_PATH scripts/fpm

RUN \
  apt-get update && \
  apt-get -y install \
          software-properties-common \
          unzip \
          bzip2 \
          curl \
	  sudo \
	  locales && \
  curl -skvLO \
       -H "Cookie: oraclelicense=accept-securebackup-cookie;" \
       http://download.oracle.com/otn-pub/java/jdk/8u191-b12/2787e4a523244c269598db4e85c51e0c/jdk-8u191-linux-x64.tar.gz && \
  tar -zxvf jdk-8u191-linux-x64.tar.gz -C /usr/lib && \
  rm jdk-8u191-linux-x64.tar.gz

RUN \
  update-alternatives --install /usr/bin/java java /usr/lib/jdk1.8.0_191/bin/java 1 && \
  update-alternatives --install /usr/bin/javac javac /usr/lib/jdk1.8.0_191/bin/javac 1 && \
  update-alternatives --install /usr/bin/keytool keytool /usr/lib/jdk1.8.0_191/bin/keytool 1

COPY \
    "${FPM_PATH}/../common-scripts/setup-spm" \
    "${FPM_PATH}/../common-scripts/setup-env" \
    "${FPM_PATH}/../common-scripts/env.sh" \
    "${FPM_PATH}/../common-scripts/run-jmxc.sh" \
    "${FPM_PATH}/spm-client-diagnostics.sh" \
    "${FPM_PATH}/spm-diag" \
    "${BASE_DIR}/bin/"

COPY \
     "${FPM_PATH}/../conf/agent.properties" \
     "${FPM_PATH}/../conf/tracing.properties" \
     "${FPM_PATH}/../conf/java.properties" \
     "${FPM_PATH}/../conf/*-region.properties" \
     "${BASE_DIR}/properties/"  

RUN \
   mkdir -p "${BASE_DIR}/spm-monitor" && \
   mkdir -p "${BASE_DIR}/spm-monitor/conf" && \
   mkdir -p "${BASE_DIR}/spm-monitor/run" && \
   curl -L https://github.com/sematext/sematext-agent-integrations/tarball/master > /tmp/configs.tar && \
   tar -xvzf /tmp/configs.tar -C "${BASE_DIR}/spm-monitor/" && \
   mv "${BASE_DIR}"/spm-monitor/sematext-sematext-agent-integrations-* "${BASE_DIR}/spm-monitor/collectors" && \
   rm /tmp/configs.tar

COPY \
   spm-monitor-generic/target/*-withdeps.jar \
   spm-monitor-storm/target/*-withdeps.jar \
   spm-montor-proxy/target/*-withdeps.jar \
   spm-monitor-haproxy/target/*-withdeps.jar \
  "${BASE_DIR}/spm-monitor/lib/"

COPY spm-client-common-libs/target/*-withdeps.jar "${BASE_DIR}/spm-monitor/lib/internal/common/"
COPY "$FPM_PATH/../conf/monitor-template-config.properties" "${BASE_DIR}/spm-monitor/templates/"

COPY \
    ./scripts/docker/entrypoint.sh \
    ./scripts/docker/unver.sh \
    "${FPM_PATH}/../common-scripts/setup-env" \
    /

RUN /unver.sh

VOLUME "${BASE_DIR}"

ENTRYPOINT ["/entrypoint.sh"]
CMD ["spm-monitor"]
