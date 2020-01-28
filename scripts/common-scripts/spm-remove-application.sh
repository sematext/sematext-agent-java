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

service spm-monitor stop

if [ -z "$JVMNAME" ]; then
  rm -R $SPM_HOME/spm-monitor/logs/standalone/*$TOKEN*
  rm -R $SPM_HOME/spm-monitor/logs/applications/*$TOKEN*
  rm -R $SPM_HOME/spm-monitor/conf/*$TOKEN*
  rm -R $SPM_HOME/spm-monitor/flume/*/dataDirs/*$TOKEN*
  rm -R $SPM_HOME/spm-monitor/flume/*/checkpointDir/*$TOKEN*  
else
  rm -R $SPM_HOME/spm-monitor/logs/standalone/*$TOKEN-$JVMNAME*
  rm -R $SPM_HOME/spm-monitor/logs/applications/$TOKEN/$JVMNAME/*
  rm -R $SPM_HOME/spm-monitor/conf/*$TOKEN-$JVMNAME*
  rm -R $SPM_HOME/spm-monitor/flume/*/dataDirs/$TOKEN/$JVMNAME/*
  rm -R $SPM_HOME/spm-monitor/flume/*/checkpointDir/$TOKEN/$JVMNAME/*
fi

service spm-monitor start

echo
echo "Done"
echo
tput setab 3 >/dev/null 2>&1
tput setaf 0 >/dev/null 2>&1
echo "NOTE: If you are using javaagent (in-process) monitor with token $TOKEN, please manually remove javaagent definition from your Java process arguments"
tput op >/dev/null 2>&1
echo
