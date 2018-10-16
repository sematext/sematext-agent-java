The metrics to be collected by Sematext App Agent are defined in YAML file. The metrics for an integration are grouped in separate YAML file based on the metric sources. 

The App Agent allows specifying set up time variables in YAML. The variable names can be referred in the values using 
`${VARIABLE_NAME}`. The variables will be replaced by the agent with values from the setup script while parsing the YAML.
It is recommended to use uppercase for the setup time variables.

The App Agent allows loading custom classes (driver libraries or classes for custom functions/conditions). The jars can 
be placed under `/opt/spm/spm-monitor/collectors/<integration>/lib` directory. jars placed in this location will be loaded during agent startup.

Each YAML file consists of the following fields:

* `type`: Data source type. Valid values are `jmx`, `db` and `json`
* `data`: Contains additional parameters that need to be passed to the data source for metrics collection. `data` field
    is supported for `db` and `json` types.
    * DB data source
        * `query`: The SQL query be executed to collect metrics.
        * `dbUrl`: The DB Connection string. e.g. `jdbc:clickhouse://${SPM_MONITOR_CLICKHOUSE_DB_HOST_PORT}/system` where
        `SPM_MONITOR_CLICKHOUSE_DB_HOST_PORT` is set up time variable passed from `setup-spm` script.  
        * `dbDriverClass`: Fully qualified DB driver class name
        * `dbUser`: DB User name (typically passed from setup script)
        * `dbPassword`: DB Password (typically passed from setup script)
        * `dbVerticalModel`: `true` if the each metric is present its own row. Default value is `false`, which means 
        all metrics are present in single row, where each column represents a metric. For more info refer to 
        [DB Vertical Model](#db-vertical-model)
        * `dbAdditionalConnectionParams` - Any additional connection params that need to passed to the DB connection.
    * JSON data source
        * `server` - The protocol, hostname and port sections of the HTTP URL to query for metrics. Typically passed from setup script.
        * `url`: The path to query metrics. For example, if the metrics are exposed via `http://localhost:9090/node/stats`
         then `server` should be `http://localhost:9090` and `url` should be `/node/stats`.
        * `basicHttpAuthUsername`: Username for HTTP Basic authentication
        * `basicHttpAuthPassword`: Password for HTTP Basic authentication
        * `smileFormat`: `true` if response is in smile format. The default value is `false`.
        * `async`:
        * `jsonHandlerClass`: Optional JSON handler class to parse the JSON output. The class should extend `CustomJsonHandler`. 
 * `require`: This section specify the condition that should be true to fetch the metrics defined in this YAML. 
    You can specify multiple require sections to match multiple conditions. Only when all the conditions are matched the metrics will be collected. 
    Typically these are version checks or checks based on custom logic. You can specify the condition class and pass 
    values to the Condition class from YAML using
    * `className` - Fully qualified condition class. This class should extend from 
    `com.sematext.spm.client.observation.BeanConditionCheck`. In case of version check, the custom class needs to extend from
    `BaseVersionConditionCheck`. For example, refer to [EsVersionCheck](../spm-monitor-generic/src/main/java/com/sematext/spm/client/es/EsVersionCheck.java)
    * `value` - Value to be passed to the condition class. In case of version check, you can specify individual version or ranges. Example values are `7`, `23.1.16`, `0.17-1.33.9` (match any version between specified range), 
    `*-1.0` (any version till 1.0), `1.0-*` (any version greater than 1.0)
* `observation`: This section contains the list of metric sources and the metrics/tags to be collected from these sources. 
    For each observation, we can specify the following parameters:
    * `name`: Name for identifying the observation. Recommended to be unique within an integration
    * `metricNamespace`: Namespace for all the metrics collected under this integration. This should be unique across all integrations.
    For example, if you are collected Jetty metrics, then the namespace will be `jetty` for the Jetty related metrics.
    * `objectName`: JMX ObjectName pattern. 
    * `path`: JSON path to read the metrics from the response.
    
    Each observation has list of metrics and tags under `metric` and `tag` sections.
    * Each `metric` can have following fields:
        * `name`: Name of the metric. 
        * `source`: The attribute name to query in the metric source. Can also refer to [derived attribute](#derived-attributes). 
        * `label`: Short description of metric
        * `type`: Metric data type. Refer to [Metric Data Types](#metric-data-types) for possible values
        * `description`: Long description of metric
        * `send`: `false` if this metric need to be sent to receiver. Default value is `true`
        * `stateful`: `true` if . Default value is `false`
        * `pctls`: Comma separated percentile values to be calculated from this metric. e.g. `99,95,50`. For more info
        refer to [Percentiles](#percentiles)
        * `agentAggregation`:
        * `unit`: Unit of measurement for this metric e.g `ms`, `bytes`, etc.
    * Each `tag` can have following fields:
        * `name`: Name of the tag. 
        * `value`:
        

## Metric Data Types

Sematext App Agent supports following metric data types:
* `gauge`
* `long_gauge`
* `counter`
* `double_counter`
* `text`

## Derived Attributes

## Percentiles

## DB Vertical Model
