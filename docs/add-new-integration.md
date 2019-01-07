## Adding a New Agent Integration
Adding a new integration that collects data from one of the available sources is easy and involves no coding. 
To add a new integration only a set of configuration YAML files are needed. These config files specify the data source,
as well as individual metrics to collect. The steps to add and verify a new Sematext App Agent integration are as follows:

1. Make sure you have the latest version of Sematext App Agent installed in your system. There are two ways to install 
Sematext App Agent:
    * Build and install the agent from [source code](../README.md#build)
    * Install the agent directly from [Sematext public repo](https://sematext.com/docs/monitoring/spm-client/#installation)
2. The YAML files for the integrations are in `/opt/spm/spm-monitor/collectors` directory. Create a directory 
for the new integration under `/opt/spm/spm-monitor/collectors`. For example, if you were adding an integration for monitoring Jetty Web Server you would create a directory `jetty` under  `/opt/spm/spm-monitor/collectors`. All the YAML 
files specifying Jetty metric sources and definitions would go under `/opt/spm/spm-monitor/collectors/jetty`.
3. Create YAML files under the respective integration directory. We recommend you group metrics by source and 
create multiple YAML files, one for each source group. The metric source could be a JMX ObjectName pattern or 
a DB SQL query or a HTTP URL. For JSON & DB data source, each YAML can have a single data source (URL or SQL query). For JMX data source, it is possible to have multiple object name patterns in a single file. 
For example, in case of Jetty, all ThreadPool related metrics in Jetty can be grouped in a single YAML file `jmx-thread-pool.yml`.
The recommended format to name the YAML files is
 `<data-source-type>-<metric-source-name>-<integration-version>.yaml`. `integration-version` is optional. The following are a few examples:
    * `jmx-executor.yml` - Executor Service related metrics from JMX endpoint 
    * `db-system-events.yml` - All metrics from System Events table from DB endpoint
    * `jmx-searcher-solr7plus.yml` - Searcher metrics from JMX endpoint for Solr version 7+
4. For more info, on YAML file format and field definitions refer to [Metrics YAML Format](metrics-yaml-format.md)
5. Below is a sample YAML file to collect Jetty ThreadPool metrics. Here we are collecting 3 metrics 
(threads, idle threads, busy threads) and extracting 2 tags (type and id).
    ```yaml
    type: jmx
    
    observation:
      - name: thread.pool.jetty
        metricNamespace: jetty
        objectName: org.eclipse.jetty.util.thread:type={type},id={id}
        metric:
          - name: threads
            source: threads
            type: long_gauge
            label: threads
            description: number of threads in the pool
        metric:
          - name: threads.busy
            source: busyThreads
            type: long_gauge
            label: busy threads
            description: number of busy threads in the pool
        metric:
          - name: threads.idle
            source: idleThreads
            type: long_gauge
            label: idle threads
            description: number of idle threads in the pool
        tag:
          - name: jetty.threadpool.type
            value: ${type}
          - name: jetty.instance.id
            value: ${id}
    ```
6. Once you have all YAML files ready you need to enable monitoring in the application to be monitored.
How that is done depends on the data source type.
    * For JMX, in case of [standalone agent](https://sematext.com/docs/monitoring/spm-monitor-standalone/), 
      make sure the appropriate [JMX startup arguments](https://sematext.com/docs/monitoring/spm-monitor-standalone/#jmx-setups-ie-how-to-configure-the-monitored-appserver)
      are passed to the Java process to be monitored.
    * For DB, make sure proper permissions are set up for the monitoring user for the table to be queried.
    * For JSON, make sure the URL is accessible and proper permissions are set up.
      
    For example, for Jetty you would add the below arguments to Java startup process to enable standalone agent to query JMX metrics:
    
    ```bash
    -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=3000 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false
    ``` 
    Setup new monitor in standalone using below `setup-spm` command:
    ```bash
    sudo bash /opt/spm/bin/setup-spm  --monitoring-token <monitoring-token>  --app-type jetty  --agent-type standalone
    ```
    The above command will start a standalone monitor and will fetch metrics from JMX exposed by Jetty server running on localhost on port 3000.
    <monitoring-token> - Monitoring Token should point to Sematext App Token if you are sending metrics to Sematext. 
    For other Influx endpoints you can specify a hexadecimal value with format xxxxxxxx--xxxx-xxxx-xxxx-xxxxxxxxxxxx 
    e.g. d0add28a-0a0f-46b2-9e1e-4928db5200e7.
    
    You can also pass custom arguments from `setup-spm` command and use them in YAML configuration. For more info refer to
    [How to pass custom arguments to my integration](metrics-yaml-format.md#specifying-variables-in-yaml)
7. By default, the agent sends the collected metrics to Sematext. You can configure a different Influx compatible destination by changing the following properties in `/opt/spm/properties/agent.properties` file:
   * `server_base_url` - Base URL of the destination. e.g. `http://192.168.0.4:8086`
   * `metrics_endpoint` - Path to send the metrics. This will be appended to `server_base_url` to form the complete URL.
       Default value is `/write?db=metrics`. You can update this property to send metrics to a different endpoint 
       or to specify username/password for InfluxDB. e.g. `/write?db=mydb&u=user&p=pass`
8. For each monitor setup, the agent generates 2 log files under `/opt/spm/spm-monitor/logs/applications/<monitoring-token>/default`
    directory.
    * `spm-monitor-*.log` - Contains the logs generated by the agent during monitoring. You can check these logs for any 
    errors during monitoring like invalid YAML, exceptions during fetching of metrics, etc.
    * `spm-monitor-stats-*.log` - Contains the metrics data sent to the destination. By default 1 in every 10 requests sent will be logged here. 
    You can refer to this file to verify the list of metrics sent and their values.
9. Before submitting a PR for a new integration, follow the above steps to test the integration and 
   make sure the metrics are collected without any errors.
10. Once you are happy with your new integration please send the PR to have your integration included in the next agent release.  To start using your new integration without waiting for the next agent release simply rebuild the agent (by running `build.sh` or `sudo mvn clean install dockerfile:build`) on the machine where you've added new integration YAMLs.  This will include your new integration YAMLs in the new agent build which you can then install on the rest of your infrastructure.
