#!/usr/bin/env bash

BASE_DIAG_DIR=$1
SPM_HOME=$2

if [ -z "$BASE_DIAG_DIR" ]
then
  echo "Parameter BASE_DIAG_DIR not set, exiting"
  exit 1
fi

cd $( cd "$( dirname "$0" )" && pwd )
. env.sh

if [ -z "$SPM_HOME" ]
then
  echo "Parameter SPM_HOME not set, exiting"
  exit 1
fi

# ensure we are root
(( EUID )) && echo && echo You need to be root or use sudo.  Aborting. && echo && exit 1


mkdir $BASE_DIAG_DIR/jmxc

echo "Capturing jmxc information..."

for JAVA_PID in `ps -ef | grep -v grep | grep java | awk '{print $2}'`; do
  java -jar $SPM_HOME/bin/lib/jmxc.jar $JAVA_PID > $BASE_DIAG_DIR/jmxc/jmxc-$JAVA_PID.log 2>&1
done

