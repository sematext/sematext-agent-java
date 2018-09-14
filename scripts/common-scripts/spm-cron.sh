#!/bin/sh

# -Btime is not built into all versions of find
#find /path/dir -name "*.log" -type f -Btime +30d -delete

SPM_HOME="/opt/spm"

LOG_DIR=$SPM_HOME/spm-cron/logs
mkdir -p $LOG_DIR

NOW=$(date +"%F")

echo `date` > $LOG_DIR/last-run.$NOW.log

CRON_FILE_AGE="-mtime +10"
find $LOG_DIR -type f $CRON_FILE_AGE 2> /dev/null | xargs rm -f -v > $LOG_DIR/deleted-cron-logs.log 2>&1

# check if collectd is installed, leave a message SPM doesn't need it anymore
command -v collectd > /dev/null 2>&1
if [ $? != 0 ]; then
  COLLECTD_INSTALLED="false"
else
  COLLECTD_INSTALLED="true"
fi
if [ "$COLLECTD_INSTALLED" = "true" ]; then
  PARENT_DIR=$SPM_HOME/collectd/logs
  if [ -d $PARENT_DIR ]; then
    ## collectd
    ##
    # move and date the current collectd log
    mv $PARENT_DIR/collectd.log $PARENT_DIR/collectd.log.$NOW
    # restart collectd to force it to create a new collectd.log
    /etc/init.d/collectd restart >> $LOG_DIR/collectd-restart.$NOW.log
    # remove collectd.log.* files older than 2 days
    FILE_AGE="-mtime +2"
    find $PARENT_DIR -type f $FILE_AGE 2> /dev/null | xargs rm -f -v > $LOG_DIR/old-files-collectd.$NOW.log 2>&1
    find $PARENT_DIR -mindepth 1 -type d -empty 2> /dev/null | xargs rm -r -f -v > $LOG_DIR/empty-dirs-collectd.$NOW.log 2>&1
  fi
fi

## monitor
##
## remove all old log files, keep empty directories intact (monitor depends on them being present)
PARENT_DIR=$SPM_HOME/spm-monitor/logs
FILE_AGE="-mtime +2"
find $PARENT_DIR -type f $FILE_AGE 2> /dev/null | xargs rm -f -v > $LOG_DIR/old-files-monitor.$NOW.log 2>&1
