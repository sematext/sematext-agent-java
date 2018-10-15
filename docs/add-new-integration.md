##Steps to add new integration in Sematext App Agent
Adding a new integration that collects data from one of the available sources is easy and involves no coding. 
To add a new integration only a set of configuration (YML) files are needed. These config files specify the data source,
as well as individual metrics to collect. Following are the setups to add & verify new integration in Sematext App Agent:

1. Make sure you have the latest version of Sematext App Agent installed in your system. There are two ways to install 
Sematext App Agent
    * Build and install the agent from [sematext-agent-java](https://github.com/sematext/sematext-agent-java) repo 
    * Install the agent directly from Sematext public repo
2. The YAML files for the integrations are present in `/opt/spm/spm-monitor/collectors` directory. Create a directory 
for the new integration under `/opt/spm/spm-monitor/collectors`. For example, lets assume you need to add support for 
monitoring Jetty Web server. Jetty metrics are exposed via JMX. Create a directory `jetty` under  `/opt/spm/spm-monitor/collectors`. All the YAML 
files specifying the Jetty metric sources and definitions will go under `/opt/spm/spm-monitor/collectors/jetty`.
    * For info on how to configure JMX in Jetty refer to [http://www.eclipse.org/jetty/documentation/current/jmx-chapter.html](http://www.eclipse.org/jetty/documentation/current/jmx-chapter.html)
3. Create YAML files under the respective integration directory. You can group all metrics from a single source in one 
YAML file. For example, in the case of Jetty all ThreadPool related metrics in Jetty can be grouped in single YAML file `jmx-thread-pool.yml`.
Typically all metrics from a single source are grouped under single YAML file. The metric source could be a JMX ObjectName pattern or a database table. The recommended format to name the YAML files is
 `<data-source-type>-<metric-source-name>-<integration-version>.yaml`. `integration-version` is optional. Following are a few examples:
    * `jmx-executor.yml` - Executor Service related metrics from JMX endpoint 
    * `db-system-events.yml` - All metrics from System Events table from DB endpoint
    * `jmx-searcher-solr7plus.yml` - Searcher metrics from JMX endpoint for Solr version 7+
4. For more info. on YAML file format and field definitions refer to [Metrics YAML Format](metrics-yaml-format.md)
5. Below is a sample YAML file to collect Jetty ThreadPool metrics
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
6. Once you have all the YAML files are the new integration ready, you need to enable monitoring in the application to be monitored.
This depends on the data source type.
    * For JMX, in case of standalone monitoring, make sure the appropriate JMX startup arguments are passed to 
      the Java process to be monitored.
    * For DB, make sure proper permissions are set up for the monitoring user for the table to be queried.
    * For JSON, make sure the URL is accessible and proper permissions are set up.
      
    For Jetty, add the below arguments to Java startup process to enable standalone agent to query JMX metrics:
    
    ```bash
    -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=3000 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false
    ``` 
    Setup new monitor in standalone using below `setup-spm` command:
    ```bash
    sudo bash /opt/spm/bin/setup-spm  --app-token <app-token>  --app-type jetty  --agent-type standalone
    ```
    The above command will start a standalone monitor and will request for JMX metrics from the Jetty server running on localhost in port 3000.
    <app-token> - App Token should point to Sematext App Token if you are sending metrics to Sematext. 
    For other Influx endpoints you can specify a hexadecimal value with format xxxxxxxx--xxxx-xxxx-xxxx-xxxxxxxxxxxx 
    e.g. d0add28a-0a0f-46b2-9e1e-4928db5200e7.
    
    You can also pass custom arguments from `setup-spm` command and use them in YAML definitions. For more info refer to
    [How to pass custom arguments to my integration](faq.md)
7. By default, the agent sends the collected metrics to Sematext. You can configure a different Influx compatible destination
   by changing the following properties in `/opt/spm/properties/agent.properties` file:
   * `server_base_url` - Base URL of the destination server. e.g. `http://192.168.0.4:8086`
   * `metrics_endpoint` - Path to send the metrics. This will be appended with `server_base_url` to form the complete URL.
       Default value is `/write?db=metrics`. You can update this property to send metrics to different endpoint 
       or to specify username/password for InfluxDB. e.g. `/write?db=mydb&u=user&p=pass`
8. For each monitor setup, the agent generates 2 log files under `/opt/spm/spm-monitor/logs/applications/<app-token>/default`
    directory.
        * `spm-monitor-*.log` - Contains the logs generated by the agent during monitoring. You can check this logs for any errors during monitoring like invalid YAML, cannot fetch metrics, etc.
        * `spm-monitor-stats-*.log` - Contains the Influx compatible metrics that are sent to the receiver. 
        By default 1 in every 10 requests sent will logged here. You can refer to this file to verify the list of metrics sent to the receiver and their values.  
