#!/bin/bash 

function removeFromRcLocal() {
  NOW=`date +"%Y%m%d%H%M"`
  RC_LOCAL_PATH=/etc/rc.local
  RC_LOCAL_BAK_PATH=/etc/rc.local.bak.$NOW
  cp $RC_LOCAL_PATH $RC_LOCAL_BAK_PATH
  grep -v "$1 restart" $RC_LOCAL_BAK_PATH > $RC_LOCAL_PATH
  rm $RC_LOCAL_BAK_PATH
}

USERADD='useradd'

if [ -f /usr/sbin/useradd ]; then
  USERADD='/usr/sbin/useradd'
fi

if ! (id spmmon 2> /dev/null > /dev/null); then
  if [ -e /etc/SuSE-release ]; then 
    $USERADD -r -s /bin/false spmmon 2> /dev/null > /dev/null
  else
    $USERADD -r -d /opt/spm/spm-monitor spmmon 2> /dev/null > /dev/null
  fi
fi

INIT=`readlink -fn /sbin/init`
if [[ $INIT == */systemd ]]; then
  # Stop existing SPM Monitors
  if [ -f /lib/systemd/system/spm-monitor.service ]; then
    systemctl stop spm-monitor
  fi
  # Some OSes don't have /lib/systemd/...
  if [ ! -d /lib/systemd/ ]; then
    if [ -d /usr/lib/systemd/ ]; then
      ln -s /usr/lib/systemd/ /lib/
    fi
  fi
fi

exit 0
