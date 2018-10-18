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
  exec \
     $JAVA \
     $SPM_MONITOR_JMX_PARAMS \
     $JAVA_OPTIONS \
     -Dspm.home=$SPM_HOME \
     -cp $SPM_MONITOR_JAR $SPM_MONITOR_STANDALONE_CLASS \
     $SPM_HOME/spm-monitor 
fi

