#!/usr/bin/python

import argparse
import logging
import os
import re

JAVA_DEFAULTS = '-server -Xmx192m -Xms64m -Xss256k'
SYSTEMD_HOME = '/lib/systemd'
SPM_HOME = '/opt/spm'
SPM_MONITOR_HOME = '/opt/spm/spm-monitor'
SPM_MONITOR_USER = 'spmmon'
SPM_MONITOR_JMX_PARAMS = ''
SPM_MONITOR_STANDALONE_CLASS = 'com.sematext.spm.client.StandaloneMonitorAgent'
SPM_MONITOR_JAR = '/opt/spm/spm-monitor/lib/spm-monitor-generic.jar'


def gen_systemd_monitor(properties_file, environment):
    logging.info("Generating unit file for properties file: %s" % properties_file)

    monitor_name = re.sub(r'\.properties$', '', properties_file)
    properties_file_path = os.path.join(SPM_MONITOR_HOME, 'conf', properties_file)

    template = """[Unit]
Description=SPM Monitor for %s
After=local-fs.target network-online.target
Requires=local-fs.target network-online.target
SourcePath=%s

[Service]
EnvironmentFile=%s
ExecStart=%s %s %s $JAVA_OPTIONS -Dspm.home=%s -cp %s %s %s
Nice=19
User=%s
StandardOutput=journal
StandardError=journal
SuccessExitStatus=143
Restart=always
RestartSec=30
StartLimitBurst=5
StartLimitInterval=5min
TimeoutStopSec=30

[Install]
WantedBy=multi-user.target
"""

    content = template % (
        monitor_name,
        properties_file_path,
        properties_file_path,
        environment['JAVA'],
        environment['SPM_MONITOR_JMX_PARAMS'],
        environment['JAVA_DEFAULTS'],
        environment['SPM_HOME'],
        environment['SPM_MONITOR_JAR'],
        environment['SPM_MONITOR_STANDALONE_CLASS'],
        properties_file_path,
        environment['SPM_MONITOR_USER'],
    )

    systemd_unit_name = monitor_name + '.service'
    systemd_unit_path = os.path.join(SYSTEMD_HOME, 'system', systemd_unit_name)
    f = open(systemd_unit_path, 'w')
    f.write(content)
    f.close()


def gen_systemd_monitor_starter(properties_files):
    logging.info("Generating unit file for monitor starter")

    exec_start = ''
    for file_name in properties_files:
        unit_file_name = re.sub(r'\.properties$', '', file_name)
        exec_start += "ExecStart=/bin/systemctl start %s\n" % unit_file_name

    exec_stop = ''
    for file_name in properties_files:
        unit_file_name = re.sub(r'\.properties$', '', file_name)
        exec_stop += "ExecStop=-/bin/systemctl stop %s\n" % unit_file_name

    template = """[Unit]
Description=SPM Monitor Starter
After=local-fs.target network-online.target
Requires=local-fs.target network-online.target

[Service]
Type=oneshot
RemainAfterExit=yes
ExecStop=-/bin/systemctl stop spm-monitor*-config*
%s%s
[Install]
WantedBy=multi-user.target
"""

    content = template % (
        exec_stop,
        exec_start
    )

    systemd_unit_name = 'spm-monitor-starter.service'
    systemd_unit_path = os.path.join(SYSTEMD_HOME, 'system', systemd_unit_name)
    f = open(systemd_unit_path, 'w')
    f.write(content)
    f.close()


def list_file_names(directory, pattern):
    file_names = []

    logging.debug("Listing dir: %s - %s" % (directory, pattern))
    for name in os.listdir(directory):
        if re.search(pattern, name):
            logging.debug("Found file: %s" % name)
            file_names.append(name)

    return file_names


def get_properties(file_path):
    properties = {}

    logging.debug("Reading properties file: %s" % file_path)
    f = open(file_path, 'r')
    data = f.readlines()
    f.close()

    logging.info("Parsing properties file: %s" % file_path)
    for line in data:
        match = re.search(r'(^\w+)\s*=\s*"*(.*?)"*$', line)
        if match:
            logging.debug("Found property: %s = %s" % (match.group(1), match.group(2)))
            properties[match.group(1)] = match.group(2)

    return properties


def get_java_bin():
    # Get JAVA properties
    java_properties = {}
    java_properties_file_path = os.path.normpath(os.path.join(SPM_HOME, 'properties', 'java.properties'))
    if os.path.isfile(java_properties_file_path):
        java_properties = get_properties(java_properties_file_path)
    else:
        logging.warning('JAVA properties file not found: %s' % java_properties_file_path)

    # Use JAVA property if set
    if 'JAVA' in java_properties.keys() and java_properties['JAVA'] != '':
        return java_properties['JAVA']

    # Use JAVA_HOME property if set
    if 'JAVA_HOME' in java_properties.keys() and java_properties['JAVA_HOME'] != '':
        return os.path.normpath(os.path.join(java_properties['JAVA_HOME'], 'bin', 'java'))

    # Check for other possible JAVA locations in various places
    if os.path.isfile('/usr/local/bin/java') and os.access('/usr/local/bin/java', os.X_OK):
        return '/usr/local/bin/java'
    elif os.path.isfile('/usr/bin/java') and os.access('/usr/bin/java', os.X_OK):
        return '/usr/bin/java'
    elif os.path.isfile('/bin/java') and os.access('/bin/java', os.X_OK):
        return '/bin/java'
    else:
        return '/not/found/java'


if __name__ == "__main__":
    # Parse arguments
    parser = argparse.ArgumentParser()
    parser.description = 'SPM Monitor Generator'
    parser.add_argument('--verbose', action='store_true', help='verbose logging')
    parser.add_argument('--debug', action='store_true', help='debug logging')
    parser.add_argument('--check', action='store_true', help='check if possible to generate services')
    args = parser.parse_args()

    # Initialise logger
    if args.verbose:
        logging.basicConfig(format='%(asctime)s %(levelname)-7s %(message)s', level=logging.INFO)
    elif args.debug:
        logging.basicConfig(format='%(asctime)s %(levelname)-7s %(message)s', level=logging.DEBUG)
    else:
        logging.basicConfig(format='%(message)s', level=logging.WARNING)

    # Looking for JAVA binary
    logging.debug('Looking for JAVA binary')
    java_bin = get_java_bin()
    logging.debug('Found JAVA binary: ' + java_bin)

    # Get monitor properties file names
    logging.debug('Getting monitor properties file names')
    monitor_conf_dir = os.path.normpath(os.path.join(SPM_MONITOR_HOME, 'conf'))
    properties_file_names = list_file_names(monitor_conf_dir, '.*\.properties$')

    # Generate Systemd unit for each properties file
    standalone_properties_file_names = []
    for properties_file_name in properties_file_names:
        # Set environment vars
        env = {
            'JAVA': java_bin,
            'JAVA_DEFAULTS': JAVA_DEFAULTS,
            'SPM_HOME': SPM_HOME,
            'SPM_MONITOR_HOME': SPM_MONITOR_HOME,
            'SPM_MONITOR_USER': SPM_MONITOR_USER,
            'SPM_MONITOR_JAR': SPM_MONITOR_JAR,
            'SPM_MONITOR_JMX_PARAMS': SPM_MONITOR_JMX_PARAMS,
            'SPM_MONITOR_STANDALONE_CLASS': SPM_MONITOR_STANDALONE_CLASS
        }

        # Add monitor properties to env
        monitor_properties = get_properties(os.path.join(monitor_conf_dir, properties_file_name))
        env.update(monitor_properties)

        # Skip monitor if SPM_MONITOR_ENABLED is False
        if 'SPM_MONITOR_ENABLED' in env:
            if env['SPM_MONITOR_ENABLED'] == 'false':
                logging.warning('Skipping monitor because SPM_MONITOR_ENABLED is false')
                continue
            else:
                logging.info('Ignoring SPM_MONITOR_ENABLED not set to "false"')

        # Skip monitor if SPM_MONITOR_IN_PROCESS doesn't exist
        if 'SPM_MONITOR_IN_PROCESS' not in env:
            logging.warning('Skipping monitor because SPM_MONITOR_IN_PROCESS was not found')
            continue

        # Skip monitor if IN_PROCESS is not False
        if env['SPM_MONITOR_IN_PROCESS'] != 'false':
            logging.info('Skipping IN_PROCESS monitor')
            continue

        # If Exit OOM arg is not present in JAVA_DEFAULTS, add it
        if '-XX:OnOutOfMemoryError' not in env['JAVA_DEFAULTS']:
            env['JAVA_DEFAULTS'] = env['JAVA_DEFAULTS'] + ' -XX:OnOutOfMemoryError="kill -9 %%p"'

        # Generate monitor Systemd Unit
        if not args.check:
            gen_systemd_monitor(properties_file_name, env)

        # Keep name of properties file for main Systemd Unit
        standalone_properties_file_names.append(properties_file_name)

    # Generate main Systemd Unit
    if not args.check:
        gen_systemd_monitor_starter(standalone_properties_file_names)
