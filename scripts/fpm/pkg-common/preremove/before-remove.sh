#!/bin/bash 

<% if pkg_type == "rpm" then %>

# for rpm package don't stop daemons and remove 
# users after 'upgrade' process
if [ "$1" == "0" ]; then
<% end %>

if [ -e /etc/init.d/spm-monitor ]; then
  /etc/init.d/spm-monitor stop
fi

if [ -e /usr/sbin/update-rc.d ]; then
  if [ -e /etc/init.d/spm-monitor ]; then
    update-rc.d -f spm-monitor remove
  fi
elif [ -e /sbin/chkconfig ]; then
  if [ -e /etc/init.d/spm-monitor ]; then
    chkconfig --del spm-monitor
  fi
fi

INIT=`readlink -fn /sbin/init`
if [[ $INIT == */systemd ]]; then
  if [ -f /lib/systemd/system/spm-monitor.service ]; then
    systemctl stop spm-monitor
    systemctl disable spm-monitor
    rm -f /lib/systemd/system/spm-monitor-*
  fi
fi

if id spmmon 2> /dev/null > /dev/null; then
  userdel spmmon 2> /dev/null
fi

rm -rf /opt/spm/spm-monitor/logs/applications/*
rm -rf /opt/spm/spm-monitor/run/*

<% if pkg_type == "rpm" then %>
fi
<% end %>

exit 0
