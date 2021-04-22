#!/usr/bin/env bash

TOKEN=$1
JVMNAME=$2

function support {
    echo
    echo "If you are having difficulties removing SPM app, we want to help you!"
    echo " * email spm-support@sematext.com"
    echo " * ping @sematext on Twitter"
    echo " * call +1 347 480 1610"
    echo
}

# make sure we are using bash                                                                                                                                                
if [ `ps -p $$ | tail -1 | awk '{print $NF}'` != "bash" ]; then
    echo && echo Please run this script using Bash.  Aborting. && echo && exit 1
fi

cd $( cd "$( dirname "$0" )" && pwd ) 
. env.sh

if [ -z "$SPM_HOME" ]
then
  echo "Variable SPM_HOME not set, using default /opt/spm"
  SPM_HOME="/opt/spm"
fi

(( EUID )) && echo You need to be root. && exit 1

if [ -z "$TOKEN" ]; then
  tput setab 1 >/dev/null 2>&1
  tput setaf 7 >/dev/null 2>&1
  echo "Token parameter missing"
  tput op >/dev/null 2>&1
  support
  exit 1
fi

if [ -z "$JVMNAME" ]; then
  service spm-monitor stop

  rm -R $SPM_HOME/spm-monitor/logs/standalone/*$TOKEN* > /dev/null 2>&1
  rm -R $SPM_HOME/spm-monitor/logs/applications/*$TOKEN* > /dev/null 2>&1
  rm -R $SPM_HOME/spm-monitor/logs/telegraf/*$TOKEN* > /dev/null 2>&1
  rm -R $SPM_HOME/spm-monitor/conf/*$TOKEN* > /dev/null 2>&1
  rm -R $SPM_HOME/spm-monitor/telegraf/*$TOKEN* > /dev/null 2>&1
  rm -R $SPM_HOME/spm-sender/conf/*$TOKEN* > /dev/null 2>&1
  rm -R $SPM_HOME/spm-monitor/flume/*/dataDirs/*$TOKEN* > /dev/null 2>&1
  rm -R $SPM_HOME/spm-monitor/flume/*/checkpointDir/*$TOKEN* > /dev/null 2>&1

  service spm-monitor start
else
  # make sure the service exists before stopping it
  systemctl daemon-reload
  service spm-monitor-config-$TOKEN-$JVMNAME stop

  rm -R $SPM_HOME/spm-monitor/logs/standalone/*$TOKEN-$JVMNAME* > /dev/null 2>&1
  rm -R $SPM_HOME/spm-monitor/logs/applications/$TOKEN/$JVMNAME/* > /dev/null 2>&1
  rm -R $SPM_HOME/spm-monitor/logs/telegraf/*$TOKEN*$JVMNAME* > /dev/null 2>&1
  rm -R $SPM_HOME/spm-monitor/conf/*$TOKEN-$JVMNAME* > /dev/null 2>&1
  rm -R $SPM_HOME/spm-monitor/telegraf/*$TOKEN-$JVMNAME* > /dev/null 2>&1
  rm -R $SPM_HOME/spm-sender/conf/*$TOKEN-$JVMNAME* > /dev/null 2>&1
  rm -R $SPM_HOME/spm-monitor/flume/*/dataDirs/$TOKEN/$JVMNAME/* > /dev/null 2>&1
  rm -R $SPM_HOME/spm-monitor/flume/*/checkpointDir/$TOKEN/$JVMNAME/* > /dev/null 2>&1

  # regenerate services after the script file is removed
  systemctl daemon-reload
fi


echo
echo "Done"
echo
tput setab 3 >/dev/null 2>&1
tput setaf 0 >/dev/null 2>&1
echo "NOTE: If you are using javaagent (in-process) monitor with token $TOKEN, please manually remove javaagent definition from your Java process arguments"
tput op >/dev/null 2>&1
echo
