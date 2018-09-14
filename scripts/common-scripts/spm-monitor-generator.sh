#!/bin/sh

rm -f /lib/systemd/system/spm-monito*-config*
rm -f /lib/systemd/system/spm-monitor-starter.service
/opt/spm/spm-monitor/bin/spm-monitor-generator.py
