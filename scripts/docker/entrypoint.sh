#!/bin/bash

SPM_MONITOR_STANDALONE_CLASS="com.sematext.spm.client.StandaloneMonitorAgent"

if [ -z "$JAVA" ]
then
  JAVA=$(command -v java)
fi

if [ -z "$SPM_HOME" ]; then
  SPM_HOME=/opt/spm
fi

if [ -z "$JAVA_DEFAULTS" ]
then
  JAVA_OPTIONS="$JAVA_OPTIONS -server "
  JAVA_OPTIONS="$JAVA_OPTIONS -Xmx320m -Xms64m -Xss256k"
else
  JAVA_OPTIONS="$JAVA_OPTIONS $JAVA_DEFAULTS"
fi

#if [[ ! "$JAVA_OPTIONS" =~ "-XX:OnOutOfMemoryError" ]];
#then
   #JAVA_OPTIONS="$JAVA_OPTIONS -XX:OnOutOfMemoryError=\"kill -9 %p\""
#fi

if [ -z "$SPM_MONITOR_JAR" ]; then
  SPM_MONITOR_JAR="$SPM_HOME/spm-monitor/lib/spm-monitor-generic.jar"
fi

if [ "$1" = 'spm-monitor' ]; then
  if [ -z "$JVM_NAME" ]; then
     JVM_NAME=default
  fi	  
  bash \
     $SPM_HOME/bin/setup-spm  \
     --app-token $APP_TOKEN   \
     --app-type $APP_TYPE  \
     --agent-type $AGENT_TYPE \
     --jvm-name $JVM_NAME
  
  if [ $? -ne 0 ]; then
     exit 1
  if

  exec \
     $JAVA \
     $SPM_MONITOR_JMX_PARAMS \
     $JAVA_OPTIONS \
     -Dspm.home=$SPM_HOME \
     -cp $SPM_MONITOR_JAR $SPM_MONITOR_STANDALONE_CLASS \
     $SPM_HOME/spm-monitor/conf/spm-monitor-config-$APP_TOKEN-$JVM_NAME.properties 
fi
