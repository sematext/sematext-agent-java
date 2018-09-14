#!/usr/bin/env bash

# usage: sudo bash move-spm-home-dir.sh /opt/spm /mnt/newdir/spm

# read two input params
OLD_LOCATION=$1
NEW_LOCATION=$2

if [ -z "$NEW_LOCATION" ]; then
  echo "Usage: sudo bash move-spm-home-dir.sh /opt/spm /mnt/newdir/spm"
  exit 1
fi

if [ ! -d "$OLD_LOCATION" ]; then
  echo "Old location $OLD_LOCATION doesn't exist!"
  exit 1
fi

if [ ! -d "$NEW_LOCATION" ]; then
  echo "New location $NEW_LOCATION doesn't exist, creating dir"
  mkdir -p $NEW_LOCATION
fi

OLD_LOCATION_ESCAPED="${OLD_LOCATION//\//\\/}"
NEW_LOCATION_ESCAPED="${NEW_LOCATION//\//\\/}"

# make sure we are using bash                                                                                                                                                
if [ `ps -p $$ | tail -1 | awk '{print $NF}'` != "bash" ]; then
    echo && echo Please run this script using Bash.  Aborting. && echo && exit 1
fi

# ensure we are root
(( EUID )) && echo && echo You need to be root or use sudo.  Aborting. && echo && exit 1

function replaceDir() {
  echo "Adjusting paths in $1"
  cp -p $1 $1.bkp
  sed "s/$OLD_LOCATION_ESCAPED/$NEW_LOCATION_ESCAPED/g" $1 > $1.bkp
  rm $1
  mv $1.bkp $1
}

# stop monitor
service spm-monitor stop

# do mv
mv $OLD_LOCATION/* $NEW_LOCATION

# adjust user home dirs
replaceDir "/etc/passwd"

# adjust env.sh
replaceDir "$NEW_LOCATION/bin/env.sh"

# adjust startup scripts
INIT=`readlink -fn /sbin/init`
if [[ $INIT == */systemd ]]; then
  replaceDir "/lib/systemd/system-generators/spm-monitor.sh"
  replaceDir "$NEW_LOCATION/spm-monitor/bin/spm-monitor-generator.py"
else
  replaceDir "/etc/init.d/spm-monitor"
fi

# adjust spm-cron.sh
replaceDir "/etc/cron.daily/spm-cron"

# go through all spm-monitor conf files, adjust them
for conf in `ls $NEW_LOCATION/spm-monitor/conf`; do
  if [ -f $NEW_LOCATION/spm-monitor/conf/$conf ]; then
    replaceDir "$NEW_LOCATION/spm-monitor/conf/$conf"
  fi
done

# start monitor
service spm-monitor start

# print note about using javaagent and what changes; also explain that monitored app has to be restarted
tput setab 2
tput setaf 0
echo
echo "All scripts and configurations were adjusted"
echo ""
echo "If you are using in-process (javaagent) versions of SPM monitor, please adjust agent path(s) which you use"
echo "when starting your application(s) to use $NEW_LOCATION where $OLD_LOCATION was used before. Also add the "
echo "following argument to Java process (you can add it right before -javaagent part):"
echo ""
echo "-Dspm.home=$NEW_LOCATION"
echo ""
echo "and restart your application(s)"
echo ""
echo "Done!"
tput op
echo
