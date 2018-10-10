#!/usr/bin/env bash  
#
# Startup script for SPM monitor under *nix systems (it works under NT/cygwin too).

# To get the service to restart correctly on reboot, uncomment below (3 lines):
#
# chkconfig: 345 85 15
# description: spm monitor
#
### BEGIN INIT INFO
# Provides:          spm-monitor
# Short-Description: spm-monitor
# Default-Start:     2 3 4 5
# Default-Stop:      0 1 6
# Required-Start:    $syslog $remote_fs
# Required-Stop:     $syslog $remote_fs
# Should-Start:
# Should-Stop:
### END INIT INFO

SPM_HOME="/opt/spm"

# Configuration variables
#
# SPM_MONITOR_HOME
#   Where SPM Monitor is installed.  Do not modify.
SPM_MONITOR_HOME="$SPM_HOME/spm-monitor"

# SPM_MONITOR_USER
#   User to run SPM Monitor
# TODO: Move username to conf
DEFAULT_SPM_MONITOR_USER="spmmon"
SPM_MONITOR_USER="$DEFAULT_SPM_MONITOR_USER"

# SPM_MONITOR_STARTER
# 
SPM_MONITOR_STARTER=$SPM_HOME/spm-monitor/bin/spm-monitor-starter.sh

usage()
{
    echo
    echo "Usage: ${0##*/} [-d] {start|stop|restart|check|list} [ CONFIG ] "
    echo
    exit 1
}

[ $# -gt 0 ] || usage


##################################################
# Some utility functions
##################################################
running()
{
  local PID=$(cat "$1" 2>/dev/null) || return 1
  kill -0 "$PID" 2>/dev/null
}

runningDoubleCheck()
{
  if [ -f "$SPM_MONITOR_PID_FILE" ]
  then
    echo
    if ! running "$SPM_MONITOR_PID_FILE"
    then
      echo "SPM MONITOR IS *NOT* REALLY RUNNING WITH PID=$(< "$SPM_MONITOR_PID_FILE")"
      echo
    else
      echo "SPM MONITOR *IS* RUNNING WITH PID=$(< "$SPM_MONITOR_PID_FILE")"
      ps u -p $(cat $SPM_MONITOR_PID_FILE)
      ps -ef | grep com.sematext.spm.client.MonitorAgent | grep -v grep
      echo
    fi
  fi
}

lowercase() {
  echo "$1" | sed "y/ABCDEFGHIJKLMNOPQRSTUVWXYZ/abcdefghijklmnopqrstuvwxyz/"
}

getPlatformInfo() {
  OS=`lowercase \`uname\``
  KERNEL=`uname -r`
  MACH=`uname -m`
  
  if [ "{$OS}" == "windowsnt" ]; then
    OS=windows
  elif [ "{$OS}" == "darwin" ]; then
    OS=mac
  else
    OS=`uname`
    if [ "${OS}" = "SunOS" ] ; then
      OS=Solaris
      ARCH=`uname -p`
      OSSTR="${OS} ${REV}(${ARCH} `uname -v`)"
    elif [ "${OS}" = "AIX" ] ; then
      OSSTR="${OS} `oslevel` (`oslevel -r`)"
    elif [ "${OS}" = "Linux" ] ; then
      if [ -f /etc/redhat-release ] ; then
        DistroBasedOn='RedHat'
      elif [ -f /etc/SuSE-release ] ; then
        DistroBasedOn='SuSe'
      elif [ -f /etc/mandrake-release ] ; then
        DistroBasedOn='Mandrake'
      elif [ -f /etc/debian_version ] ; then
        DistroBasedOn='Debian'
      elif [ -e /etc/system-release ] && grep -i "amazon linux ami" /etc/system-release > /dev/null; then
        DistroBasedOn='RedHat'
      fi
      if [ -f /etc/UnitedLinux-release ] ; then
        DIST="${DIST}[`cat /etc/UnitedLinux-release | tr "\n" ' ' | sed s/VERSION.*//`]"
      fi
      OS=`lowercase $OS`
      DistroBasedOn=`lowercase $DistroBasedOn`
      readonly OS
      readonly DIST
      readonly DistroBasedOn
      readonly PSUEDONAME
      readonly REV
      readonly KERNEL
      readonly MACH
    fi
  fi
}

getPlatformInfo
export DISTRIB_BASED_ON=`lowercase $DistroBasedOn`
export USE_START_STOP_DAEMON="false"

if [ "$DISTRIB_BASED_ON" == "redhat" ]; then
  USE_START_STOP_DAEMON="false";
elif [ "$DISTRIB_BASED_ON" == "suse" ]; then
  USE_START_STOP_DAEMON="false";
else
  if type start-stop-daemon > /dev/null 2>&1
  then
    USE_START_STOP_DAEMON="true"
  fi
fi

function sourceMonitorConfProperties() {
  # use tmp file with added quotes to avoid breaking the source command
  ORIG_MONITOR_CONF=$1
  TMP_MONITOR_CONF=$ORIG_MONITOR_CONF.tmp

  
  sed -e 's#^\([^=]*\)=\([^"]\)\(.*\)$#\1="\2\3"#g' $ORIG_MONITOR_CONF > $TMP_MONITOR_CONF
  
  # reset some properties which may appear only in some config files to avoid them being applied to all
  SPM_MONITOR_USER="$DEFAULT_SPM_MONITOR_USER"
  unset SPM_MONITOR_COLLECT_INTERVAL
  unset SPM_MONITOR_TRACING_ENABLED
  unset THREAD_INSTRUMENTATION_ENABLED
  unset SPM_MONITOR_TAGS
  unset SPM_SUPPRESS_TAGS
  unset COLLECT_AWS_TAGS
  unset SPM_MONITOR_JMX_PARAMS
  unset spm_monitor_logging_level
  unset SPM_MONITOR_LOGGING_LEVEL
  
  . $TMP_MONITOR_CONF
    
  rm $TMP_MONITOR_CONF 
}

function varDump()
{
  sourceMonitorConfProperties $SPM_MONITOR_CONF

  echo "Checking arguments to SPM Monitor: "
  echo "SPM_MONITOR_HOME                 =  $SPM_MONITOR_HOME"
  echo "SPM_MONITOR_USER                 =  $SPM_MONITOR_USER"
  echo "SPM_MONITOR_CONF                 =  $SPM_MONITOR_CONF"
  echo "SPM_MONITOR_STARTER              =  $SPM_MONITOR_STARTER"
  echo "SPM_MONITOR_STOUTERR_LOG         =  $SPM_MONITOR_STOUTERR_LOG"
  echo "SPM_MONITOR_PID_FILE             =  $SPM_MONITOR_PID_FILE"
  echo "SPM_MONITOR_PROPERTIES_CONF_FILE = $SPM_MONITOR_PROPERTIES_CONF_FILE"
  echo
}

function sourceRedhatFunctions()
{
  ORIG_PATH=$PATH
  . /etc/rc.d/init.d/functions
  export PATH=$PATH:$ORIG_PATH
}

##################################################
# Get the action & configs
##################################################
NO_START=0
DEBUG=0

while [[ $1 = -* ]]; do
  case $1 in
    -d) DEBUG=1 ;;
  esac
  shift
done
ACTION=$1
shift
SPM_MONITOR_CONF=$1
shift

##################################################
# Set tmp if not already set.
##################################################
TMPDIR=${TMPDIR:-/tmp}

##################################################
# No SPM_MONITOR_HOME yet? We're out of luck!
##################################################
if [ -z "$SPM_MONITOR_HOME" ]; then
  echo "** ERROR: SPM_MONITOR_HOME not set, you need to set it or install in a standard location" 
  exit 1
fi

#####################################################
# Are we running on Windows? Could be, with Cygwin/NT.
#####################################################
case "`uname`" in
  CYGWIN*) PATH_SEPARATOR=";";;
  *) PATH_SEPARATOR=":";;
esac

#####################################################
# Comment these out after you're happy with what 
# the script is doing.
#####################################################

if (( DEBUG ))
then
  varDump
  runningDoubleCheck
fi

function resetMonConfVars()
{
  unset SPM_MONITOR_JAR
  unset SPM_MONITOR_PID_FILE
  unset SPM_MONITOR_STOUTERR_LOG
  unset SPM_MONITOR_JAR
  unset SPM_MONITOR_JMX_PARAMS
  unset SPM_MONITOR_PROPERTIES_CONF_FILE
}

function loadMonConfVars()
{
  export SPM_MONITOR_PROPERTIES_CONF_FILE="$1"
  export SPM_MONITOR_PID_FILE=`echo "$SPM_MONITOR_PROPERTIES_CONF_FILE" | sed 's/.*spm-monitor-/\/var\/run\/spm-monitor\/spm-monitor-/' | sed 's/.properties/.pid/'`
  export SPM_MONITOR_STOUTERR_LOG=`echo "$SPM_MONITOR_PROPERTIES_CONF_FILE" | sed "s@.*spm-monitor-@$SPM_HOME/spm-monitor/logs/standalone/spm-monitor-@" | sed 's/.properties/.log/'`  
}

function ensurePidFileDirExists()
{
  SPM_MONITOR_PID_DIR=`dirname $SPM_MONITOR_PID_FILE`
  test -d $SPM_MONITOR_PID_DIR || mkdir -p $SPM_MONITOR_PID_DIR
  chown spmmon $SPM_MONITOR_PID_DIR
}

function startMonitor()
{
  if [ -z "$1" ]; then
    echo "No SPM Monitor conf specified"
    usage
  fi
  
  resetMonConfVars
  monConf=$1
  
  if [[ $monConf != *.properties ]]; then
    echo "Skipping old monitor conf file : $monConf"
    return 1
  fi  
  
  sourceMonitorConfProperties $monConf
  
  if [ "$SPM_MONITOR_IN_PROCESS" != "false" ]; then
    echo "Skipping monitor conf which is not in standalone mode : $monConf"
    return 1
  fi
  
  loadMonConfVars $monConf
  ensurePidFileDirExists
  
  echo Starting SPM Monitor for: $monConf
  echo Standalone SPM Monitor LOG located at: $SPM_MONITOR_STOUTERR_LOG
  
  if [ -f "$SPM_MONITOR_STOUTERR_LOG" ]
  then
    chmod 777 $SPM_MONITOR_STOUTERR_LOG
  fi
  
  case $DistroBasedOn in 
    debian)
      unset CH_USER
      if [ -n "$SPM_MONITOR_USER" ]
      then
        CH_USER="-c$SPM_MONITOR_USER"
      fi
      
      SSD_CMD="start-stop-daemon -v -N 19 -S -p$SPM_MONITOR_PID_FILE $CH_USER -d$SPM_MONITOR_HOME -b -m -a $SPM_MONITOR_STARTER $monConf -- --daemon"
      
      if $SSD_CMD
      then
        sleep 1
        if running "$SPM_MONITOR_PID_FILE"
        then
          echo "OK"
          runningDoubleCheck
        else
          echo "FAILED"
        fi
      fi
      
      ;;

    suse)
      unset CH_USER
      if [ -n "$SPM_MONITOR_USER" ]
      then
        CH_USER="-c$SPM_MONITOR_USER"
      fi
      
      SSD_CMD="start_daemon -f -x -p $SPM_MONITOR_PID_FILE -u $SPM_MONITOR_USER $SPM_MONITOR_STARTER $monConf -- --daemon"
      
      if $SSD_CMD
      then
        sleep 1
        if running "$SPM_MONITOR_PID_FILE"
        then
          echo "OK"
          runningDoubleCheck
        else
          echo "FAILED"
        fi
      fi        
      ;;

    redhat)      
      if [ -f "$SPM_MONITOR_PID_FILE" ]
      then
        if running $SPM_MONITOR_PID_FILE
        then
          echo "Already Running!"
          exit 1
        else
          # dead pid file - remove
          rm -f "$SPM_MONITOR_PID_FILE"
        fi
      fi
      
      if [ "$SPM_MONITOR_USER" ] 
      then
        touch "$SPM_MONITOR_PID_FILE"
        chown "$SPM_MONITOR_USER" "$SPM_MONITOR_PID_FILE"
        
        if [ "$DISTRIB_BASED_ON" == "redhat" ]; then
          sourceRedhatFunctions
          daemon --pidfile $SPM_MONITOR_PID_FILE --user $SPM_MONITOR_USER $SPM_MONITOR_STARTER $monConf $SPM_MONITOR_STOUTERR_LOG $SPM_MONITOR_PID_FILE $SPM_MONITOR_PROPERTIES_CONF_FILE
          
          retval=$?
          if [ $retval -ne 0 ]; then
            echo "SPM Monitor daemon couldn't be started"
            exit 1
          fi
        else
          # TODO: Check for alternative solution
          su - "$SPM_MONITOR_USER" -c "
            $SPM_MONITOR_STARTER --daemon &
            disown \$!
            echo \$! > '$SPM_MONITOR_PID_FILE'"
        fi
      else
        if [ "$DISTRIB_BASED_ON" == "redhat" ]; then
          sourceRedhatFunctions
          daemon --pidfile $SPM_MONITOR_PID_FILE $SPM_MONITOR_STARTER $monConf

          if [ $retval -ne 0 ]; then
            echo "SPM Monitor daemon couldn't be started"
            exit 1
          fi
        else
          "$SPM_MONITOR_STARTER" &
          disown $!
          echo $! > "$SPM_MONITOR_PID_FILE"
        fi
      fi
      
      echo "STARTED SPM Monitor `date`"
      ;;
    *)
      unset CH_USER
      if [ -n "$SPM_MONITOR_USER" ]
      then
        CH_USER="-c$SPM_MONITOR_USER"
      fi
      
      SSD_CMD="start-stop-daemon -v -N 19 -S -p$SPM_MONITOR_PID_FILE $CH_USER -d$SPM_MONITOR_HOME -b -m -a $SPM_MONITOR_STARTER $monConf -- --daemon"
      
      if $SSD_CMD
      then
        sleep 1
        if running "$SPM_MONITOR_PID_FILE"
        then
          echo "OK"
          runningDoubleCheck
        else
          echo "FAILED"
        fi
      fi
      ;;
  esac
}

function stopMonitor()
{
  monConf=$1

  case $DistroBasedOn in 
    debian)
      start-stop-daemon -K -p"$SPM_MONITOR_PID_FILE" -d"$SPM_MONITOR_HOME" -a -s HUP
      
      TIMEOUT=30
      while running "$SPM_MONITOR_PID_FILE"; do
        if (( TIMEOUT-- == 0 )); then
          start-stop-daemon -K -p"$SPM_MONITOR_PID_FILE" -d"$SPM_MONITOR_HOME" -a -s KILL
        fi

        sleep 1
      done

      rm -f "$SPM_MONITOR_PID_FILE"
      echo OK
      
      ;;
    redhat | suse)
      PID=$(cat "$SPM_MONITOR_PID_FILE" 2>/dev/null)
      kill "$PID" 2>/dev/null
      
      TIMEOUT=30
      while running "$SPM_MONITOR_PID_FILE"; do
        if (( TIMEOUT-- == 0 )); then
          kill -KILL "$PID" 2>/dev/null
        fi

        sleep 1
      done

      rm -f "$SPM_MONITOR_PID_FILE"
      echo OK
      ;;
    *)
      start-stop-daemon -K -p"$SPM_MONITOR_PID_FILE" -d"$SPM_MONITOR_HOME" -a -s HUP
      
      TIMEOUT=30
      while running "$SPM_MONITOR_PID_FILE"; do
        if (( TIMEOUT-- == 0 )); then
          start-stop-daemon -K -p"$SPM_MONITOR_PID_FILE" -d"$SPM_MONITOR_HOME" -a -s KILL
        fi

        sleep 1
      done

      rm -f "$SPM_MONITOR_PID_FILE"
      echo OK
      ;;
  esac
  
  # since it is possible that some spm-monitor was left hanging, we will ensure they are killed manually
  sleep 2
  EXISTING_STANDALONE_MON_PROC=`ps -ef | grep com.sematext.spm.client.StandaloneMonitorAgent | grep -v grep | grep $monConf | head -n 1 2>/dev/null`  
  
  # there can be N hanging processes for single spm application, so while...
  while [ ! -z "$EXISTING_STANDALONE_MON_PROC" ]; do
    # kill process
    PROCESS_ID=`echo $EXISTING_STANDALONE_MON_PROC | awk '{print $2}'`
    echo "Killing process with PID: $PROCESS_ID"
    
    # first try softly
    kill "$PROCESS_ID" > /dev/null
    
    TIMEOUT=30
    while ps -p $PROCESS_ID > /dev/null; do
      if (( TIMEOUT-- == 0 )); then
        kill -KILL $PROCESS_ID 2>/dev/null
      fi
      sleep 1
    done
    
    echo "Process with PID: $PROCESS_ID killed"

    # any remaining instances of standalone monitor for this spm application?    
    EXISTING_STANDALONE_MON_PROC=`ps -ef | grep com.sematext.spm.client.StandaloneMonitorAgent | grep -v grep | grep $monConf | head -n 1 2>/dev/null`
  done
}

function checkMonitorStatus() {
  resetMonConfVars
  monConf=$1
  
  if [[ $monConf != *.properties ]]; then
    echo "SPM Monitor conf not in standalone mode : $monConf"
    return 1
  fi  
  
  sourceMonitorConfProperties $monConf
  
  if [ "$SPM_MONITOR_IN_PROCESS" != "false" ]; then
    echo "SPM Monitor conf not in standalone mode : $monConf"
    return 1
  fi
  
  loadMonConfVars $monConf
  ensurePidFileDirExists
  
  echo Checking Standalone SPM Monitor for: $monConf

  if [ -f "$SPM_MONITOR_PID_FILE" ]; then
    PID=`cat "$SPM_MONITOR_PID_FILE"`
    if [ -z "`ps axf | grep ${PID} | grep -v grep`" ]; then
      printf "%s\n" "Process dead but pidfile $SPM_MONITOR_PID_FILE exists"
    else
      echo "Running"
    fi
  else
   printf "%s\n" "Standalone monitor for $monConf not running"
   exit 1
  fi
}

##################################################
# Do the action
##################################################
case "$ACTION" in
  start)
    if (( NO_START )); then 
      echo "Not starting SPM Monitor - NO_START=1";
      exit
    fi

    if [ -z "$SPM_MONITOR_CONF" ]; then
      # start monitors for all apps
      echo Starting all SPM Monitors...
      for monConf in `ls $SPM_HOME/spm-monitor/conf/*.properties 2> /dev/null`; do
        startMonitor $monConf
      done
    else
      # start just the monitor for token specified as param (if not running)
      startMonitor $SPM_MONITOR_CONF
    fi

    ;;

  stop)
    if [ -z "$SPM_MONITOR_CONF" ]; then
      # stop monitors for all apps
      echo Stopping all SPM Monitors...
      for monConf in `ls $SPM_HOME/spm-monitor/conf/*.properties 2> /dev/null`; do
        resetMonConfVars
        sourceMonitorConfProperties $monConf

        loadMonConfVars $monConf
        ensurePidFileDirExists


        echo Stopping SPM Monitor for: $monConf
        stopMonitor $monConf
      done
    else
      # stop just the monitor for token specified as param (if not running)
      resetMonConfVars
      sourceMonitorConfProperties $SPM_MONITOR_CONF
      
      loadMonConfVars $SPM_MONITOR_CONF
      ensurePidFileDirExists
      
      echo Stopping SPM Monitor for: $SPM_MONITOR_CONF
      stopMonitor $SPM_MONITOR_CONF
    fi

    ;;

  restart)
    $0 stop $*
    sleep 1
    $0 start $*

    ;;

  status)
    printf "%-50s" "Checking SPM Monitor..."
    echo
    
    for monConf in `ls $SPM_HOME/spm-monitor/conf/*.properties 2> /dev/null`; do
      checkMonitorStatus $monConf
    done
    
    ;;

  check)
    varDump
    runningDoubleCheck

    exit 1

    ;;

  list)
    ls -1 $SPM_HOME/spm-monitor/conf/*conf
    ;;

  *)
    usage

    ;;
esac

exit 0
