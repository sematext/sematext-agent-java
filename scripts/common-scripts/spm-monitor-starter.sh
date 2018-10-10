#!/bin/bash
#
# Starts SPM Monitor, logging to a logfile. We have to do this because
# there's no way to call SPM Monitor directly and have
# start-stop-daemon redirect stdout to a logfile.
#

SPM_MONITOR_PROPERTIES=$1
. $SPM_MONITOR_PROPERTIES
LOGFILE=$SPM_MONITOR_STOUTERR_LOG
PIDFILE=$SPM_MONITOR_PID_FILE

# Configuration variables
#
# JAVA
#   Command to invoke Java. If not set, java (from the PATH) will be used.
#
# JAVA_OPTIONS
#   Extra options to pass to the JVM
#
# SPM_MONITOR_HOME
#   Where SPM Monitor is installed.
#
# SPM_MONITOR_JMX_PARAMS
#   Params related to JMX settings (so monitor can connect to monitored service)
#
# SPM_MONITOR_JAR
#   Full path to monitor jar

SPM_MONITOR_STANDALONE_CLASS="com.sematext.spm.client.StandaloneMonitorAgent"

cd $( cd "$( dirname "$0" )" && pwd )
. ../../bin/env.sh

if [ -z "$SPM_HOME" ]
then
  echo "Variable SPM_HOME not set, using default /opt/spm"
  SPM_HOME="/opt/spm"
fi

##################################################
# Setup JAVA if unset
##################################################
if [ -z "$JAVA" ]
then
  JAVA=$(command -v java)
fi

if [ -z "$JAVA" ]
then
  echo "Cannot find Java. Please either set JAVA_HOME variable in $SPM_HOME/properties/java.properties or put java (>=1.6) in your PATH." 2>&2
  exit 1
fi

#####################################################
# Include all the jars and add other JVM params
#####################################################
if [ -z "$JAVA_DEFAULTS" ]
then
  JAVA_OPTIONS="$JAVA_OPTIONS -server "
  JAVA_OPTIONS="$JAVA_OPTIONS -Xmx320m -Xms64m -Xss256k"
else
  JAVA_OPTIONS="$JAVA_OPTIONS $JAVA_DEFAULTS"
fi

if [[ ! "$JAVA_OPTIONS" =~ "-XX:OnOutOfMemoryError" ]];
then
   JAVA_OPTIONS="$JAVA_OPTIONS -XX:OnOutOfMemoryError=\"kill -9 %p\""
fi


if [ -z "$SPM_MONITOR_JAR" ]; then
  SPM_MONITOR_JAR="$SPM_HOME/spm-monitor/lib/spm-monitor-generic.jar"
fi

#####################################################
# This is how SPM Monitor will be started
#####################################################

# TODO: also calculate SPM_MONITOR_JMX_PARAMS argument
# TODO: Can we expose whole JAVA_OPTS in the conf file?
# Then we would not need to calculate anything here
# And those working with Java know how to adjust Java command line args
# They just need to be reminded about JMX options, and we can do that in the comments in the conf itself

BASE_RUN_CMD="$JAVA $SPM_MONITOR_JMX_PARAMS $JAVA_OPTIONS -Dspm.home=$SPM_HOME -cp $SPM_MONITOR_JAR $SPM_MONITOR_STANDALONE_CLASS $SPM_MONITOR_PROPERTIES_CONF_FILE"
USE_IONICE=$(command -v ionice)
if [ -z "$USE_IONICE" ]; then
  RUN_CMD="nice -n 19 $BASE_RUN_CMD"
else
  RUN_CMD="nice -n 19 ionice -c2 -n7 $BASE_RUN_CMD"
fi


echo
echo START COMMAND:
echo $RUN_CMD
echo
echo LOG FILE:
echo $LOGFILE
echo
echo PID FILE:
echo $PIDFILE
echo

# Fork off SPM Monitor into the background and log to a file
$RUN_CMD >>${LOGFILE} 2>&1 </dev/null &

# Capture the child process PID
CHILD="$!"

echo "CHILD (Java process) PID:" $CHILD
echo $CHILD > $PIDFILE

if [ "$USE_START_STOP_DAEMON" == "true" ]; then
  # Kill the child process when start-stop-daemon sends us a kill signal
  trap "kill $CHILD 2>/dev/null" exit INT TERM
  
  # Wait for child process to exit
  wait
fi
