## Metrics Definition YAML format
The metrics to be collected by Sematext App Agent are defined in YAML file. The metrics for an integration are grouped 
in individual YAML files based on the metric sources. 

Each YAML file consists of the following fields:

* `type`: Data source type. Valid values are `jmx`, `db` and `json`
* `data`: Contains additional parameters that have to be passed to the data source for metrics collection. `data` field
    is supported for `db` and `json` types.
    * DB data source
        * `query`: The SQL query be executed to collect metrics.
        * `dbUrl`: The DB Connection string. e.g. `jdbc:clickhouse://${SPM_MONITOR_CLICKHOUSE_DB_HOST_PORT}/system` where
        `SPM_MONITOR_CLICKHOUSE_DB_HOST_PORT` is placeholder for variable passed from `setup-spm` script.  
        * `dbDriverClass`: Fully qualified DB driver class name. Refer to 
        [Adding custom classes to the agent](#adding-custom-classes-to-the-agent) for adding driver jar to agent.
        * `dbUser`: DB User name (typically passed from setup script)
        * `dbPassword`: DB Password (typically passed from setup script)
        * `dbVerticalModel`: `true` if the each metric is present in its own row. The default value is `false`, which means 
        all metrics are present in single row, where each column represents a metric. For more info refer to 
        [DB Vertical Model](#db-vertical-model)
        * `dbAdditionalConnectionParams` - Any additional connection params to be passed to the DB connection.
    * JSON data source
        * `server` - The protocol, hostname and port sections of the HTTP URL to query for metrics  (typically passed from setup script)
        * `url`: The path to query metrics. For example, if the metrics are exposed via `http://localhost:9090/node/stats`
         then `server` should be `http://localhost:9090` and `url` should be `/node/stats`.
        * `basicHttpAuthUsername`: Username for HTTP Basic authentication  (typically passed from setup script)
        * `basicHttpAuthPassword`: Password for HTTP Basic authentication  (typically passed from setup script)
        * `smileFormat`: `true` if response is in [smile format](https://github.com/FasterXML/smile-format-specification).
         The default value is `false`.
        * `async`: Set to true to fetch metrics asynchronously. The default value is `false`.
        * `jsonHandlerClass`: Optional JSON handler class to parse the JSON output. The class should extend `CustomJsonHandler`. 
 * `require`: This section specifies the condition that should be true to fetch the metrics defined in this YAML. 
    You can specify multiple require sections to match multiple conditions. Only when all the conditions are matched the metrics will be collected. 
    Typically these are version checks or checks based on custom logic. You can specify the condition class and pass 
    values to the Condition class from YAML using
    * `className` - Fully qualified condition class. This class should extend from 
    `com.sematext.spm.client.observation.BeanConditionCheck`. In the case of version check, the custom class should extend from
    `BaseVersionConditionCheck`. For example, refer to [EsVersionCheck](../spm-monitor-generic/src/main/java/com/sematext/spm/client/es/EsVersionCheck.java)
    * `value` - Value to be passed to the condition class. In the case of version check, you can specify individual version or ranges. 
    Example values are `7`, `23.1.16`, `0.17-1.33.9` (match any version between specified range), 
    `*-1.0` (any version till 1.0), `1.0-*` (any version greater than 1.0)
* `accept`: Specifies the condition based on tag value for the observation to be collected. 
    For more info refer [here](#how-to-process-or-skip-metrics-from-selected-metric-sources)
    * `name`: Name of the tag whose value has to be compared.
    * `value`: The value to compare. This could be a literal value, `func`, `json`, `jmx`, or variable placeholder (`${}`)
* `ignore`: Specifies the condition based on tag value, which when true the observation will be ignored. Opposite of `accept`.
   Similar to `accept`, you can specify `name` and `value`.
* `observation`: This section contains the list of metric sources and the metrics/tags to be collected from these sources. 
    For each observation, we can specify the following parameters:
    * `name`: Name for identifying the observation. Should be unique within an integration
    * `metricNamespace`: Namespace for all the metrics collected under this integration. This should be unique across all integrations.
    For example, if you are monitoring Jetty metrics, then the namespace will be `jetty` for the Jetty related metrics.
    * `objectName`: JMX ObjectName pattern to query. You can extract tags from the key properties of the object name. For more info,
     refer to [Extracting tags from JMX ObjectName](#extracting-tags-from-jmx-objectname). Applicable for JMX data source.
    * `path`: JSONPath-like expression for the objects that should be queried in the response. You can extract tags from the path using placeholders.
     For more info refer to [Extracting tags from JSON Path](#extracting-tags-from-json-path). Applicable for JSON data source.
    
    Each observation has a list of metrics and tags under `metric` and `tag` sections.
    * Each `metric` can have following fields:
        * `name`: Name of the metric. Use dot-separated hierarchical naming.
        * `source`: The attribute name to query in the metric source. The metric source can also be a function. Refer to
         [Derived Metrics](#derived-metrics) 
        * `label`: Short description of the metric
        * `type`: Metric data type. Refer to [Metric Data Types](#metric-data-types) for possible values
        * `description`: Long description of the metric
        * `send`: `false` if this metric has to be sent to the receiver. The default value is `true`. Will be false for metrics
         that are extracted as tags or used in the calculation of other metrics. 
        * `stateful`: `true` if the collected metric values have to be preserved across collections. The default value is `false`.
        For example, can be set to true in the case of derived metrics where we need to compare with old values.
        * `pctls`: Comma separated percentile values to be calculated from this metric. e.g. `99,95,50`. For more info
        refer to [Percentiles](#percentiles)
        * `agentAggregation`: Used to override the default aggregation function. When some of the extracted tags are 
         omitted in the definition, the agent aggregates the metric on the omitted tag using the aggregation function 
         based on `type`. Counters are aggregated by `SUM` and gauges by `AVG`. This could be overridden by using this field.
         Valid values are `SUM`, `AVG`, `MAX`, `MIN`, `DISCARD`.
        * `unit`: Unit of measurement for this metric e.g. `ms`, `bytes`, etc.
    * Each `tag` can have name and value fields:
        * `name`: Name of the tag. Make sure the tag name is unique. Prefix metric namespace to the tag name.  
        * `value`: Reference to the name from where this tag has to be extracted. This could be a metric name defined 
        under the observation or the placeholder in `path` or `objectName`. For metric, use `eval` function
        to refer to metric. You can also use [built-in functions](./built-in-functions.md) to modify the values before ]
        sending to output.

## Metric Data Types

Sematext App Agent supports the following metric data types:

* `gauge`, `long_gauge`: Gauge data type. Default aggregation is `AVG`
* `counter`, `long_counter`: Counter data type. Default aggregation is `SUM` 
* `text`: Textual data type. Typically used for metrics that have to be extracted as tags

## Derived metrics

Derived metrics are calculated by applying built-in or custom functions on other metrics. Derived metrics can be used to
manipulate the values of metrics before sending to the receiver. Some of the use cases are to extract a number from a string 
value or derive a metric as a function of 2 attributes. 

For derived metrics, the `source` field of the metric has to be of the format: `func:<function-name>(<params>)`. 

* `func`: Denotes that metric has to be derived by applying the function specified.
* `<function-name>`: Function to invoked. In the case of [built-in functions](./built-in-functions.md), 
just function name is enough. For custom functions, specify the function name with fully qualified classname. e.g.
`func:com.sematext.spm.client.solr.CalculateWarmupTime(warmupTime,searcherName)`. The function class should extend 
`com.sematext.spm.client.observation.CalculationFunction`
* `<params>`: Optional params to function. All params are of type String and will be interpreted and converted 
accordingly in the function implementation.

By default, the result of the function will be interpreted as a gauge. If the result is counter, then prepend the function
reference with `as_counter`. For example,

```yaml
  - name: cache.lookups
    source: as_counter:func:ExtractLongFromMapString(Value,cumulative_lookups)
    type: counter
    label: cache lookups
    description: lookups count
    stateful: true
```

Some of the built-in functions can be applied to tags also. 

## Percentiles

Sematext App Agent, can automatically calculate percentiles for a metric and send to the receiver. The percentiles to be
calculated can be specified using `pctls` field. Below example will calculate 99th, 95th and 50th percentile of metric 
`requestCount` and send it in attributes `request.count.pctl.99`, `request.count.pctl.95` and `request.count.pctl.50` 
respectively.

```yaml
  - name: request.count
    source: requestCount
    type: double_gauge
    stateful: true
    pctls: 99,95,50
    label: request count
```

## DB Vertical Model
In the case of DB source, the metrics to be collected can be present in a single row or in multiple rows, 
with each row containing the metric name and value. In such cases, you can set `dbVerticalModel` to `true`. For example,
for the table below, `dbVerticalModel` will be set to true.
```
┌─event───────────────────────────────────┬─────────value─┐
│ Query                                   │        335485 │
│ SelectQuery                             │        335485 │
│ FileOpen                                │           149 │
│ ReadBufferFromFileDescriptorRead        │           257 │
│ ReadBufferFromFileDescriptorReadBytes   │         23956 │
│ WriteBufferFromFileDescriptorWrite      │             1 │
│ WriteBufferFromFileDescriptorWriteBytes │            59 │
```

## Specifying variables in YAML
The App Agent allows specifying set up time variables in YAML. The variable names can be referred in the values using 
`${VARIABLE_NAME}` placeholders. The placeholders will be replaced by the agent with values from the setup script while parsing the YAML.
It is recommended to use uppercase for the setup time placeholders. For example, in the below YAML, the variables will be 
replaced accordingly when passed from `setup-spm` script. If the variable is not specified, it will be replaced by an empty string.

```yaml
type: db
data:
  query: SELECT * FROM metrics;
  dbUrl: jdbc:clickhouse://${SPM_MONITOR_CLICKHOUSE_DB_HOST_PORT}/system
  dbDriverClass: ru.yandex.clickhouse.ClickHouseDriver
  dbUser: ${SPM_MONITOR_CLICKHOUSE_DB_USER}
  dbPassword: ${SPM_MONITOR_CLICKHOUSE_DB_PASSWORD}
``` 

```bash
sudo bash /opt/spm/bin/setup-spm  --app-token d0add288-0a0f-46bb-9e1a-4928db5200e7  --app-type clickhouse   \
    --agent-type standalone      --SPM_MONITOR_CLICKHOUSE_DB_USER ''      --SPM_MONITOR_CLICKHOUSE_DB_PASSWORD '' \
    --SPM_MONITOR_CLICKHOUSE_DB_HOST_PORT 'localhost:8123'
```

## Extracting tags from JMX ObjectName
JMX object name consists of two parts, the domain and the key properties. For e.g. In the case of JVM memory pool, 
each pool has its own JMX object name instance, where the key `name` refers to pool name. The instances are 
`java.lang:type=MemoryPool,name=Code Cache`, `java.lang:type=MemoryPool,name=Metaspace`, etc. The pool name can be
extracted as tag by specifying the `objectName` pattern as `java.lang:type=MemoryPool,name=${poolName}` and mapping `poolName`
placeholder to tag in `tag` definition of observation as shown below.
```yaml
- name: jvmMemoryPool
    metricNamespace: jvm
    objectName: java.lang:type=MemoryPool,name=${poolName}
...
    tag:
      - name: jvm.memory.pool
        value: ${poolName}
```
Typically the keys for a given ObjectName pattern is static. In the case of dynamic keys add `*` at the end of pattern for matching 
all object names. For example, refer to [Tomcat Datasource YAML](https://github.com/sematext/sematext-agent-integrations/blob/master/tomcat/jmx-datasource.yml)

## Extracting tags from JSON Path
For JSON data source, `path` specifies set of objects which should be monitored. For each placeholder (defined
with ${...}) any value will be accepted (while also storing the value of that placeholder). In the example below, if some setup has
3 indices (say 'A', 'B' and 'C), each with 2 shards, meaning a total of 6 shards, there will be a total of 6
matching objects found by this path expression (regardless of on which nodes which shards are, since the total number of
shards is 6 anyway). For each of those 6 objects, placeholder values will be available for use in tag definitions 
(notice how the value of "es.node.id" and "es.index" tags are specified - they will be resolved to exact values matching
one of those 6 objects).
    
```yaml
path: $.indices.${indexName}.shards.${shard}[?(@.routing.node=${nodeId})].merges
...
tag:
  
  - name: es.node.id
    value: ${nodeId}

  - name: es.index
    value: ${indexName}
```

## Adding custom classes to the agent
The App Agent allows loading custom classes (driver libraries or classes for custom functions/conditions). The jars can 
be placed under `/opt/spm/spm-monitor/collectors/<integration>/lib` directory. Jars placed in this location will be 
loaded during agent startup.

## How to process or skip metrics from selected metric sources
For a given metric source specified in an observation, you can skip or accept metric values from selected tag values. This 
could be done by specifying the ignore/accept conditions. Each condition takes the tag name to compare and the value. The
value can be a literal string, `jmx` (JMX attribute), `json` (JSONPath), or `${}` setup variable placeholder. For example,
in the case of JVM Memory Pool config, you can skip metrics from a specific pool ( say `Metaspace`) using the following YAML:

```yaml
- name: jvmMemoryPool
    metricNamespace: jvm
    objectName: java.lang:type=MemoryPool,name=${poolName}
...
    tag:
      - name: jvm.memory.pool
        value: ${poolName}
    ignore:
      name: jvm.memory.pool
      value: Metaspace
```
