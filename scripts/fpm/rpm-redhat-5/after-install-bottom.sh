/etc/init.d/spm-monitor restart

if [ -e /usr/sbin/update-rc.d ]; then
  update-rc.d spm-monitor start 20 2 3 4 5 . stop 80 0 1 6 .
elif [ -e /sbin/chkconfig ]; then
  chkconfig --add spm-monitor
  chkconfig spm-monitor on
fi


echo
echo -------------------------------------------------------------------------------
echo
echo SPM client installer done
echo
tput setab 2 >/dev/null 2>&1
tput setaf 7 >/dev/null 2>&1
echo "                                                                                                                   "
echo "IMPORTANT: Prepare monitor configuration by running setup-spm command.                                             "
echo "           For more info refer to https://github.com/sematext/sematext-agent-java/docs/                            "
echo "                                                                                                                   "
tput op >/dev/null 2>&1
tput setab 3 >/dev/null 2>&1
tput setaf 0 >/dev/null 2>&1
echo
echo "WARN: If your server accesses Internet via proxy, set proxy host, port, username by running:"
echo "  sudo bash /opt/spm/bin/setup-env --proxy-host \"www.myproxyhost.com\" --proxy-port \"1234\" --proxy-user \"myuser\" --proxy-password \"mypassword123\""
tput op >/dev/null 2>&1
echo
echo When done with all steps, go to the configured destination to see your performance metrics
echo If you do not see your metrics in a few minutes, run: sudo bash /opt/spm/bin/spm-client-diagnostics.sh
echo
echo -------------------------------------------------------------------------------
echo

exit 0
