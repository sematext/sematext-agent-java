#!/bin/bash

# usage: sudo bash move-spm-home-dir-soft.sh /mnt/somedir

# read one input params
NEW_LOCATION=$1

# make sure we are using bash                                                                                                                                                
if [ `ps -p $$ | tail -1 | awk '{print $NF}'` != "bash" ]; then
    echo && echo Please run this script using Bash.  Aborting. && echo && exit 1
fi

# ensure we are root
(( EUID )) && echo && echo You need to be root or use sudo.  Aborting. && echo && exit 1

if [ -z "$NEW_LOCATION" ]; then
  echo "Usage: sudo bash move-spm-home-dir-soft.sh /mnt/somedir"
  exit 1
fi

if [ ! -d "$NEW_LOCATION" ]; then
  echo "New location $NEW_LOCATION doesn't exist, creating dir"
  mkdir -p $NEW_LOCATION
fi

sudo service spm-monitor stop
sudo mv /opt/spm $NEW_LOCATION
sudo ln -s $NEW_LOCATION/spm /opt/spm
sudo service spm-monitor start
