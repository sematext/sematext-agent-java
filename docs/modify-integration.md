## Modifying Built-in Integrations
Sematext App Agent has built-in support for various application [integrations](https://github.com/sematext/sematext-agent-integrations). 
You can modify the built-in integration YAMLs to suit your needs or fix any issues present in these YAMLs.
These changes can be done by modifying existing YAML files (for example, adding new metric or modifying the description of existing metric) 
or adding a new YAML file. Assuming you have installed Sematext App Agent (by building it yourself or by installing the 
`sematext-agent` package from Sematext Cloud) and set up the agent to monitor your application, following are the steps to 
modify metrics for a built-in integration:

1. The YAML files for the integrations are present in `/opt/spm/spm-monitor/collectors/<integration>` directory.
   To modify existing YAML, edit and save the respective YAML file. To add a new YAML file, create the file under the 
   respective integration directory.
2. Refer to [Metrics Configuration YAML](/docs/metrics-yaml-format.md) documentation to understand the format of the YAML files.
3. The agent periodically (every 1 minute) reads the YAML files and refreshes its internal cache. Any changes in the YAML files 
   will be picked up by the agent during the next refresh. After refresh, the agent uses the latest configuration to fetch the metrics.
   Any errors, during the refresh of YAML files will be logged in `/opt/spm/spm-monitor/logs/applications/<monitoring-token>/default/spm-monitor-0.log`.
4. The agent periodically (every 1 minute) sends the changed metadata (labels, description, type, etc.) to Sematext. 
   Any change in metadata will be reflected in Sematext UI after next periodic refresh. This is not applicable to non-Sematext destinations like InfluxDB.
5. You are welcome to contribute the changes back to Sematext by cloning
   [sematext-agent-integrations](https://github.com/sematext/sematext-agent-integrations) repo and submitting a PR. 
   For more information refer to [Contributing](https://github.com/sematext/sematext-agent-integrations/blob/master/CONTRIBUTING.md)
   section. The changes will be available in the next release of the agent. Upgrading the agent to this release will update the YAML files.
6. If the agent is running on multiple machines and your changes are private or you need them before next release of the agent,
   manually copy the changes to these machines.   
7. In the case of private changes, during the upgrade of the agent, any new YAML files added to integrations will be retained. 
   If you have modified an existing YAML file, you can choose to overwrite the file and manually make the private changes 
   on the upgraded file. If you select to keep your local file, any changes made by Sematext in the YAML won't be available.
