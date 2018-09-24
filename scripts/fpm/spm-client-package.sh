#!/bin/bash

if [ "$#" -lt 2 ]; then
   echo
   echo "Usage: $0 dest_dir version <src_java_dir>"
   echo
   exit 1
fi

OUTDIR=$1
VERSION=$2
SRC_JAVA_DIR="../sematext-agent-java"

if [ "$3" ]; then
  SRC_JAVA_DIR=$3
fi

if [ ! -d ${SRC_JAVA_DIR} ]; then
  echo "src_java_dir ${SRC_JAVA_DIR} does not exist"
  exit 1
fi

test -d $OUTDIR || mkdir -p $OUTDIR

IMAGE_PATH=${OUTDIR}/image
FPM_PATH=scripts/fpm

. $FPM_PATH/spm-client-env.sh

#copy image
rm -rf $OUTDIR/image
cp -r $FPM_PATH/skel $OUTDIR/image
find $OUTDIR/image -name '.git*' | xargs rm -rf

#prepare static files for image
cp $FPM_PATH/../common-scripts/setup-spm $IMAGE_PATH/opt/spm/bin
cp $FPM_PATH/../common-scripts/setup-env $IMAGE_PATH/opt/spm/bin
cp $FPM_PATH/../common-scripts/spm-cron.sh $IMAGE_PATH/etc/cron.daily/spm-cron

cp $FPM_PATH/../common-scripts/spm-monitor.sh $IMAGE_PATH/etc/init.d/spm-monitor
cp $FPM_PATH/../common-scripts/spm-monitor.service $IMAGE_PATH/lib/systemd/system/spm-monitor.service

curl -L https://github.com/sematext/sematext-agent-integrations/tarball/master > /tmp/configs.tar

tar -xvzf /tmp/configs.tar &> /dev/null
mv sematext-sematext-agent-integrations-*/* $IMAGE_PATH/opt/spm/spm-monitor/collectors
rm -R sematext-sematext-agent-integrations-*
rm /tmp/configs.tar

cp $FPM_PATH/spm-client-diagnostics.sh $IMAGE_PATH/opt/spm/bin
cp $FPM_PATH/spm-diag $IMAGE_PATH/opt/spm/bin
cp $FPM_PATH/../common-scripts/env.sh $IMAGE_PATH/opt/spm/bin
cp $FPM_PATH/../common-scripts/move-spm-home-dir.sh $IMAGE_PATH/opt/spm/bin
cp $FPM_PATH/../common-scripts/move-spm-home-dir-soft.sh $IMAGE_PATH/opt/spm/bin
cp $FPM_PATH/../common-scripts/spm-remove-application.sh $IMAGE_PATH/opt/spm/bin
cp $FPM_PATH/../conf/agent.properties $IMAGE_PATH/opt/spm/properties/agent.base.properties
cp $FPM_PATH/../conf/tracing.properties $IMAGE_PATH/opt/spm/properties/tracing.base.properties
cp $FPM_PATH/../conf/java.properties $IMAGE_PATH/opt/spm/properties
cp $FPM_PATH/../conf/*-region.properties $IMAGE_PATH/opt/spm/properties
cp $FPM_PATH/../common-scripts/run-jmxc.sh $IMAGE_PATH/opt/spm/bin
cp $FPM_PATH/../lib/jmxc.jar $IMAGE_PATH/opt/spm/bin/lib

chmod +x $IMAGE_PATH/opt/spm/bin/*.sh
chmod +x $IMAGE_PATH/opt/spm/bin/spm-diag
chmod +x $IMAGE_PATH/opt/spm/bin/setup-spm
chmod +x $IMAGE_PATH/opt/spm/bin/setup-env
chmod -R a+rwX $IMAGE_PATH/opt/spm/spm-monitor/collectors/

cp $FPM_PATH/../conf/monitor-template-config.properties $IMAGE_PATH/opt/spm/spm-monitor/templates/

#copy monitors artifacts
cp $FPM_PATH/../common-scripts/spm-monitor-starter.sh $IMAGE_PATH/opt/spm/spm-monitor/bin
cp $FPM_PATH/../common-scripts/spm-monitor-generator.sh $IMAGE_PATH/lib/systemd/system-generators/spm-monitor.sh
cp $FPM_PATH/../common-scripts/spm-monitor-generator.py $IMAGE_PATH/opt/spm/spm-monitor/bin

CURRENT_DIR=`pwd`

cd ${SRC_JAVA_DIR}

if [ "$DEBUG" != "true" ]; then
  mvn clean install
fi

for CLIENT_TYPE in $SPM_CLIENT_PACKED_MONITORS; do
  MONITOR_MODULE=spm-monitor-$CLIENT_TYPE
  JAR_WITH_VERSION_NAME=`find $MONITOR_MODULE/target -name '*-withdeps.jar' | head -n 1 | xargs basename`
  JAR_WITHOUT_VERSION_NAME=`echo $JAR_WITH_VERSION_NAME | sed -E s/"(.*)(\-[[:digit:]]+\.[[:digit:]]+\.[[:digit:]])(\-SNAPSHOT){0,1}(\-withdeps)(\.jar)"/"\1\5"/g`

  cp $MONITOR_MODULE/target/*-withdeps.jar $IMAGE_PATH/opt/spm/spm-monitor/lib/$JAR_WITHOUT_VERSION_NAME
done

SPM_CLIENT_COMMON_LIBS_MODULE_JAR_WITH_VERSION_NAME=`find $SPM_CLIENT_COMMON_LIBS_MODULE/target -name '*-withdeps.jar' | head -n 1 | xargs basename`
SPM_CLIENT_COMMON_LIBS_MODULE_JAR_WITHOUT_VERSION_NAME=`echo $SPM_CLIENT_COMMON_LIBS_MODULE_JAR_WITH_VERSION_NAME | sed -E s/"(.*)(\-[[:digit:]]+\.[[:digit:]]+\.[[:digit:]])(\-SNAPSHOT){0,1}(\-withdeps)(\.jar)"/"\1\5"/g`
cp $SPM_CLIENT_COMMON_LIBS_MODULE/target/*-withdeps.jar $IMAGE_PATH/opt/spm/spm-monitor/lib/internal/common/$SPM_CLIENT_COMMON_LIBS_MODULE_JAR_WITHOUT_VERSION_NAME

cd ${CURRENT_DIR}

#create packages
for PACKAGE_TYPE in $SPM_PACKAGE_TYPES; do
  echo "Processing $PACKAGE_TYPE"
  $FPM_PATH/$PACKAGE_TYPE/pkg.sh $OUTDIR $VERSION
done
