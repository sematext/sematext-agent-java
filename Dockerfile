FROM ubuntu:18.04
LABEL maintainer="Sematext"

ENV BASE_DIR /opt/spm
ENV FPM_PATH scripts/fpm

RUN \
  apt-get update && \
  apt-get -y install \
       openjdk-11-jre-headless \
       software-properties-common \
       unzip \
       bzip2 \
       curl \
       sudo \
       locales

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
  spm-monitor-redis/target/*-withdeps.jar \
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
CMD ["spm-monitor-generic"]
