#!/usr/bin/env bash

cd $( cd "$( dirname "$0" )" && pwd )
. env.sh

if [ -z "$SPM_HOME" ]
then
  echo "Variable SPM_HOME not set, using default /opt/spm"
  SPM_HOME="/opt/spm"
fi

# make sure we are using bash                                                                                                                                                
if [ `ps -p $$ | tail -1 | awk '{print $NF}'` != "bash" ]; then
    echo && echo Please run this script using Bash.  Aborting. && echo && exit 1
fi

# ensure we are root
(( EUID )) && echo && echo You need to be root or use sudo.  Aborting. && echo && exit 1

# source agent.properties to read values like receiver URL
SCRIPT_DIR=$( cd "$( dirname "$0" )" && pwd )
. $SCRIPT_DIR/../properties/agent.properties

echo

cd /tmp
BASE_DIAG_DIR_NO_LEADING=tmp/spm-info-$$
BASE_DIAG_DIR=/$BASE_DIAG_DIR_NO_LEADING
ERRORS_DIAG_DIR="$BASE_DIAG_DIR/errors"
mkdir -v $BASE_DIAG_DIR
mkdir -v $ERRORS_DIAG_DIR

echo Capturing system information...
function captureSysInfo() {
    echo "* Date:"
    date
    echo "* Hostname:"
    hostname
    echo "* Kernel:"
    uname -a
    echo "* Distribution:"
    lsb_release -a
    cat /etc/*release
    cat /etc/issue
    echo "* Version:"
    cat /proc/*version
    echo "* Memory:"
    free -m
    echo "* Disk:"
    df -h -T
    echo "* Ulimit:"
    ulimit -a
    echo "* Java:"
    java -version
    echo "* ifconfig:"
    ifconfig
}

DIAG_DIR=$BASE_DIAG_DIR
captureSysInfo &> $DIAG_DIR/sys-info.txt

echo Capturing SPM information...

function versionDebian() {
  dpkg -s spm-client | grep "Version" | sed -E s/"Version:( *)(.*)"/"\2"/g
}

function versionRedhat() {
  yum list spm-client | grep "Installed Packages" -A 1 | grep "spm-client" | awk '{ print $2 }'
}

if [ -f /etc/debian_version ]; then
  versionDebian > $DIAG_DIR/VERSION
elif [ -f /etc/redhat-release ]; then
  versionRedhat > $DIAG_DIR/VERSION
fi

# properties dir
cp -r $SPM_HOME/properties $DIAG_DIR

#
du -h --max-depth=2 $SPM_HOME &> $DIAG_DIR/spm-du2.txt
sudo lsof 2> /dev/null | grep spm &> $DIAG_DIR/lsof-spm.txt

ls -alR $SPM_HOME/ &> $DIAG_DIR/ls-alR-opt-spm.txt

# read receiver location from the config
spmReceiver=$server_base_url
tmpIndex=`expr index "$spmReceiver" ://`
if [ $tmpIndex -gt 0 ]; then
  ((tmpIndex+=2))
  spmReceiver="${spmReceiver:tmpIndex}"
fi

indexOfSlash=`expr index "$spmReceiver" /`
if [ $indexOfSlash -gt 0 ]; then
  ((indexOfSlash--))
  spmReceiver="${spmReceiver:0:indexOfSlash}"
fi
spmReceiverPort=443
indexOfColon=`expr index "$spmReceiver" :`
if [ $indexOfColon -gt 0 ]; then
  spmReceiverPort="${spmReceiver:indexOfColon}"
  ((indexOfColon--))
  spmReceiver="${spmReceiver:0:indexOfColon}"
fi

echo "Resolved spm receiver to: $spmReceiver $spmReceiverPort" | tee -a $DIAG_DIR/resolved-spm-rec.txt

# check DNS
spmReceiverHosts=$(getent hosts $spmReceiver | wc -l)
if [ $spmReceiverHosts -lt 1 ]; then
  echo "ERROR: Cannot look up $spmReceiver -- check your DNS" | tee -a $ERRORS_DIAG_DIR/connectivity-errors.log
  echo
  echo $spmReceiver:$spmReceiverPort is where agent sends metrics
fi

# check Network
command -v nc -zv -w 20 $spmReceiver $spmReceiverPort > /dev/null 2>&1
if [ $? -eq 0 ]; then
  nc -zv -w 20 $spmReceiver $spmReceiverPort
  if [ $? -ne 0 ]; then
    echo "ERROR: Cannot connect to $spmReceiver:$spmReceiverPort -- check your Network / Firewall / Proxy" | tee -a $ERRORS_DIAG_DIR/connectivity-errors.log
    echo
    echo $spmReceiver:$spmReceiverPort is where agent sends metrics
    echo To configure agent to send metrics through a proxy, run:
    echo "  sudo bash /opt/spm/bin/setup-env --proxy-host \"www.myproxyhost.com\" --proxy-port \"1234\" --proxy-user \"myuser\" --proxy-password \"mypassword123\""
  fi
fi

command -v journalctl > /dev/null 2>&1
if [ $? -eq 0 ]; then
  mkdir -p $DIAG_DIR/spm-monitor/standalone/journalctl
  
  if [ -d /lib/systemd/system ]; then
    cd /lib/systemd/system
    for serviceName in `ls spm-monitor*`; do
      journalctl -u $serviceName -a > $DIAG_DIR/spm-monitor/standalone/journalctl/$serviceName.log
    done
    cd /tmp
  fi
fi

# monitor
DIAG_DIR=$BASE_DIAG_DIR/spm-monitor; mkdir -p $DIAG_DIR

echo

# if spm-monitor/conf is empty, display message about step 2 being skipped
if [ ! "$(ls -A $SPM_HOME/spm-monitor/conf)" ]; then
  echo "ERROR: Directory $SPM_HOME/spm-monitor/conf is empty because agent setup was not completed. Execute setup-spm command to create monitor configuration" | tee -a $ERRORS_DIAG_DIR/config-errors.log
  echo
fi

if [ -d $SPM_HOME/spm-monitor/logs/applications/ ]; then
  for sys in `ls $SPM_HOME/spm-monitor/logs/applications/`; do
    for jvm in `ls $SPM_HOME/spm-monitor/logs/applications/$sys`; do
      if [ ! -z "`ls $SPM_HOME/spm-monitor/logs/applications/$sys/$jvm | grep \".log\" | head -n 1 2>/dev/null`" ]; then
        countSubdirs=$(grep -o "/" <<< "$SPM_HOME/spm-monitor/logs/applications/$sys/$jvm" | wc -l)
        countSubdirsPlusOne=$((countSubdirs+1))
              
        sysID=`echo $SPM_HOME/spm-monitor/logs/applications/$sys/$jvm | cut -d"/" -f$countSubdirs,$countSubdirsPlusOne`
        sysToken=`echo $SPM_HOME/spm-monitor/logs/applications/$sys/$jvm | cut -d"/" -f$countSubdirs`
        sysJvm=`echo $SPM_HOME/spm-monitor/logs/applications/$sys/$jvm | cut -d"/" -f$countSubdirsPlusOne`
        
        mkdir -p $DIAG_DIR/applications/$sysID
        
        ls -al $SPM_HOME/spm-monitor/logs/applications/$sys/$jvm &> $DIAG_DIR/applications/$sysID/spm-monitor-logs-dir.txt
        
        for logfile in `ls $SPM_HOME/spm-monitor/logs/applications/$sys/$jvm/spm-monitor*.log* 2> /dev/null`; do
          logfileName=`basename $logfile`
          head -1000 $SPM_HOME/spm-monitor/logs/applications/$sys/$jvm/$logfileName &> $DIAG_DIR/applications/$sysID/$logfileName-head-1000.txt
          tail -1000 $SPM_HOME/spm-monitor/logs/applications/$sys/$jvm/$logfileName &> $DIAG_DIR/applications/$sysID/$logfileName-tail-1000.txt
        
          # Exceptions
          if [[ $logfile = *".log" ]]; then
            grep Exception $SPM_HOME/spm-monitor/logs/applications/$sys/$jvm/$logfileName | grep -v AmazonClientException | tail -200 &> $DIAG_DIR/applications/$sysID/$logfileName-exceptions.txt
          
            numExceptions=$(grep -c ^ $DIAG_DIR/applications/$sysID/$logfileName-exceptions.txt)
            if [ $numExceptions -gt 0 ]; then
              # show exceptions for self-help and sleep to give person time to read
              echo Found exceptions in $SPM_HOME/spm-monitor/logs/applications/$sys/$jvm/$logfileName
              echo Here are the last few
              tail -5 $DIAG_DIR/applications/$sysID/$logfileName-exceptions.txt
              sleep 2
            fi
          fi
        done

        cp $SPM_HOME/spm-monitor/conf/*$sysToken-$sysJvm*.* $DIAG_DIR/applications/$sysID 2> /dev/null
      else
        for confSubtype in `ls $SPM_HOME/spm-monitor/logs/applications/$sys/$jvm`; do
          countSubdirs=$(grep -o "/" <<< "$SPM_HOME/spm-monitor/logs/applications/$sys/$jvm" | wc -l)
          countSubdirsPlusOne=$((countSubdirs+1))
                
          sysID=`echo $SPM_HOME/spm-monitor/logs/applications/$sys/$jvm | cut -d"/" -f$countSubdirs,$countSubdirsPlusOne`
          sysToken=`echo $SPM_HOME/spm-monitor/logs/applications/$sys/$jvm | cut -d"/" -f$countSubdirs`
          sysJvm=`echo $SPM_HOME/spm-monitor/logs/applications/$sys/$jvm | cut -d"/" -f$countSubdirsPlusOne`
          
          mkdir -p $DIAG_DIR/applications/$sysID-$confSubtype
          
          ls -al $SPM_HOME/spm-monitor/logs/applications/$sys/$jvm/$confSubtype &> $DIAG_DIR/applications/$sysID-$confSubtype/spm-monitor-logs-dir.txt
          
          for logfile in `ls $SPM_HOME/spm-monitor/logs/applications/$sys/$jvm/$confSubtype/spm-monitor*.log* 2> /dev/null`; do
            logfileName=`basename $logfile`
            head -1000 $SPM_HOME/spm-monitor/logs/applications/$sys/$jvm/$confSubtype/$logfileName &> $DIAG_DIR/applications/$sysID-$confSubtype/$logfileName-head-1000.txt
            tail -1000 $SPM_HOME/spm-monitor/logs/applications/$sys/$jvm/$confSubtype/$logfileName &> $DIAG_DIR/applications/$sysID-$confSubtype/$logfileName-tail-1000.txt
          
            # Exceptions
            if [[ $logfile = *".log" ]]; then
              grep Exception $SPM_HOME/spm-monitor/logs/applications/$sys/$jvm/$confSubtype/$logfileName | tail -200 &> $DIAG_DIR/applications/$sysID-$confSubtype/$logfileName-exceptions.txt

              numExceptions=$(grep -c ^ $DIAG_DIR/applications/$sysID-$confSubtype/$logfileName-exceptions.txt)
              if [ $numExceptions -gt 0 ]; then
                # show exceptions for self-help and sleep to give person time to read
                echo Found exceptions in $SPM_HOME/spm-monitor/logs/applications/$sys/$jvm/$confSubtype/$logfileName
                echo Here are the last few
                tail -5 $DIAG_DIR/applications/$sysID-$confSubtype/$logfileName-exceptions.txt
                sleep 2
              fi
            fi
          done
  
          cp $SPM_HOME/spm-monitor/conf/*$confSubtype*$sysToken-$sysJvm*.* $DIAG_DIR/applications/$sysID-$confSubtype 2> /dev/null
        done
      fi
    done
  done
fi

if [ -d $SPM_HOME/spm-monitor/logs/standalone/ ]; then
  cp -r $SPM_HOME/spm-monitor/logs/standalone $DIAG_DIR
fi

mkdir $DIAG_DIR/conf
cp $SPM_HOME/spm-monitor/conf/* $DIAG_DIR/conf 2> /dev/null

DIAG_DIR=$BASE_DIAG_DIR
# java
sudo su - spmmon -c "java -version" &> $DIAG_DIR/spm-monitor-java-version.txt

# spm-monitor service
# note: this will work only if systemd is present, but we're running it anyway because we capture both stdout and stderr
sudo status 'spm-monitor*' &> $DIAG_DIR/spm-monitor-status1.txt

sudo service spm-monitor status &> $DIAG_DIR/spm-monitor-status2.txt

# processes
ps -ef | grep spm &> $DIAG_DIR/ps-ef-spm.txt
ps -efT | grep spm &> $DIAG_DIR/ps-efT-spm.txt
ps -ef | grep java &> $DIAG_DIR/ps-java-spm.txt
ps -ef | egrep 'ntpd|chrony' &> $DIAG_DIR/ps-ntpd.txt

top -c -b -d 3 -n 5 &> $DIAG_DIR/top.txt

dmesg | tail -n 1000 &> $DIAG_DIR/dmesg.txt

# rc.local
if [ -f /etc/rc.local ]; then
  cp /etc/rc.local $DIAG_DIR/rc.local.txt
fi

if [ -f /etc/rc.d/rc.local ]; then
  cp /etc/rc.d/rc.local $DIAG_DIR/rcd.rc.local.txt
fi

# spm-cron
if [ -d $SPM_HOME/spm-cron ]; then
  cp -a $SPM_HOME/spm-cron $DIAG_DIR/
else
  mkdir -p $DIAG_DIR/spm-cron/
fi

cp /etc/cron.daily/spm-cron $DIAG_DIR/spm-cron/

# Capture stats from spm receiver
DIAG_DIR=$BASE_DIAG_DIR/receiver-system-stats

ls -lrt $SPM_HOME/spm-monitor &> $BASE_DIAG_DIR/spm-monitor/spm-monitor-ls.txt
ls -lrt $SPM_HOME/spm-monitor/conf &> $BASE_DIAG_DIR/spm-monitor/spm-monitor-conf-ls.txt
ls -lrt $SPM_HOME/spm-monitor/lib &> $BASE_DIAG_DIR/spm-monitor/spm-monitor-lib-ls.txt
ls -lrt $SPM_HOME/spm-monitor/logs &> $BASE_DIAG_DIR/spm-monitor/spm-monitor-logs-ls.txt
ls -lrt $SPM_HOME/spm-monitor/logs/standalone &> $BASE_DIAG_DIR/spm-monitor/spm-monitor-logs-standalone-ls.txt
ls -lrt $SPM_HOME/spm-monitor/logs/applications &> $BASE_DIAG_DIR/spm-monitor/spm-monitor-logs-applications-ls.txt

# jmxc
bash $SPM_HOME/bin/run-jmxc.sh $BASE_DIAG_DIR $SPM_HOME

# systemd
INIT=`readlink -fn /sbin/init`
if [[ $INIT == */systemd ]]; then
  mkdir -p $BASE_DIAG_DIR/systemd/
  cp /lib/systemd/system/spm-monitor* $BASE_DIAG_DIR/systemd/
  cp /lib/systemd/system-generators/spm-monitor.sh $BASE_DIAG_DIR/systemd/spm-monitor-generator.sh
  cp /opt/spm/spm-monitor/bin/spm-monitor-generator.py $BASE_DIAG_DIR/systemd/
  /opt/spm/spm-monitor/bin/spm-monitor-generator.py --debug &> $BASE_DIAG_DIR/systemd/spm-monitor-generator.txt
  /bin/systemctl status 'spm-monitor*' &> $BASE_DIAG_DIR/systemd/status.txt
fi

# archive
DIAG_DIR=$BASE_DIAG_DIR_NO_LEADING
ARCHIVE_NAME=spm-info-$$.tar.gz
tar -C / -cf spm-info-$$.tar $DIAG_DIR && gzip -9 spm-info-$$.tar
EXIT_CODE="$?"
if [ "0" -ne "$EXIT_CODE" ]
then
    echo "[ERROR]: Can't tar \$DIAG_DIR.  Trying zip..."
    ARCHIVE_NAME=spm-info-$$.zip
    zip -r $ARCHIVE_NAME $DIAG_DIR
    EXIT_CODE="$?"
    if [ "0" -ne "$EXIT_CODE" ]
    then
  echo "[ERROR]: Can't tar/gzip \$DIAG_DIR.  Please archive this dir and email it to support@sematext.com"
  exit 1
    fi
fi

# finale
echo
echo = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =
echo
echo SPM diagnostics info is in /tmp/$ARCHIVE_NAME
echo
echo Please email the /tmp/$ARCHIVE_NAME file to support@sematext.com
echo
echo Thank you for using SPM!
echo
echo = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =
echo
