#!/bin/bash

SPM_HOME=/opt/spm

#Root

chown root.root /etc/cron.daily/spm-cron
chmod 755 /etc/cron.daily/spm-cron

if [ -e /etc/init.d/spm-monitor ]; then
  chown root.root /etc/init.d/spm-monitor
fi
chown -f root.root /lib/systemd/system-generators/spm-monitor.sh
chmod -f 755 /lib/systemd/system-generators/spm-monitor.sh

chown -R root.root $SPM_HOME/bin
chmod 755 $SPM_HOME/bin/spm-client-diagnostics.sh
chown -R spmmon $SPM_HOME/properties

# executes before migrate-from-old, here we will just ensure agent.properties is present regardless of possibly
# spm-sender.properties also being present (in which case migration is needed, but migrate-from-old will do that)
if [ -e $SPM_HOME/properties/agent.properties ]; then
  echo "Keeping existing agent.properties"
  rm $SPM_HOME/properties/agent.base.properties
else
  echo "Preparing new agent.properties"
  mv $SPM_HOME/properties/agent.base.properties $SPM_HOME/properties/agent.properties
fi
chmod 644 $SPM_HOME/properties/agent.properties

if [ -e $SPM_HOME/properties/tracing.properties ]; then
  echo "Keeping existing tracing.properties"
  rm $SPM_HOME/properties/tracing.base.properties
else
  echo "Preparing new tracing.properties"
  mv $SPM_HOME/properties/tracing.base.properties $SPM_HOME/properties/tracing.properties
fi
chmod 644 $SPM_HOME/properties/tracing.properties

#Spm monitor
chown -R spmmon $SPM_HOME/spm-monitor
chmod 755 $SPM_HOME/spm-monitor/bin/spm-monitor-generator.py
chmod -R 777 $SPM_HOME/spm-monitor/logs/applications
chmod 644 $SPM_HOME/spm-monitor/lib/*.jar
chmod -R 777 $SPM_HOME/spm-monitor/run
chmod -R 777 $SPM_HOME/spm-monitor/conf
chmod -R 777 $SPM_HOME/spm-monitor/tmp
chmod -R 777 $SPM_HOME/spm-monitor/flume/
