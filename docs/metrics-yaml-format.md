## Metrics Configuration YAML Format
The metrics to be collected by Sematext App Agent are defined in YAML file. The metrics for an integration are grouped 
in individual YAML files based on the metric sources. Each YAML file has the following sections:

```yaml
type: <data source type>
data: 
    <data source specific properties>
require:
    <list of require conditions like version checks>
observation:
    <list of Observations>
    metric:
      <list of metrics>
    tag:
      <list of tags>
    accept:
      <list of accept conditions>
    ignore:
      <list of ignore conditions>
```

## Configuration Fields

* `type`: Data source type. Valid values are `jmx`, `db` and `json`
* `data`: Contains additional parameters that have to be passed to the data source for metrics collection. `data` field
    is supported for `db` and `json` types.
    * DB data source
        * `query`: The SQL query be executed to collect metrics.
        * `dbUrl`: The DB Connection string. e.g. `jdbc:clickhouse://${SPM_MONITOR_CLICKHOUSE_DB_HOST_PORT}/system` where
        `SPM_MONITOR_CLICKHOUSE_DB_HOST_PORT` is placeholder for the variable passed from `setup-spm` script.  
        * `dbDriverClass`: Fully qualified DB driver class name. Refer to 
        [Adding custom classes to the agent](#adding-custom-classes-to-the-agent) for adding driver jar to agent.
        * `dbUser`: DB username (typically passed from setup script)
        * `dbPassword`: DB password (typically passed from setup script)
        * `dbVerticalModel`: `true` if each metric is present in its own row. The default value is `false`, which should 
        be used when all metrics are present in a single row, where each column represents a metric. For more info refer to 
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
        * `async`: Set to true to fetch metrics asynchronously. The default value is `false`. Should be set to true for sources
        that can take a long time to respond, so that it does not block collection from other sources.
        * `jsonHandlerClass`: Optional JSON handler class to parse the JSON output. The class should extend `CustomJsonHandler`. 
 * `require`: This section specifies the condition that should be true to fetch the metrics defined in this YAML. 
    You can specify multiple require sections. Only when all the conditions are true the metrics specified in this YAML
    will be collected. Typically these are version checks or checks based on custom logic. You can specify the condition class and pass 
    values to the condition class from YAML using
    * `className` - Fully qualified condition class.
    * `value` - Value to be passed to the condition class. 
    For more info see [Process or Skip YAMLs based on Conditions](#process-or-skip-yamls-based-on-conditions)
* `accept`: Specifies the condition based on tag value for the observation to be collected. 
    For more info see [Process or Skip Metrics based on Tag Values](#process-or-skip-metrics-based-on-tag-values)
    * `name`: Name of the tag whose value has to be compared.
    * `value`: The value to compare. This could be the actual string to compare or values derived using `func`, `json`, `jmx` or variable placeholder (`${}`)
* `ignore`: Specifies the condition based on tag value. When the condition is met the observation will be ignored. Opposite of `accept`.
   Similar to `accept`, you can specify `name` and `value`.
* `observation`: This section contains the list of metric sources and the metrics/tags to be collected from these sources. 
    For each observation, we can specify the following parameters:
    * `name`: Name for identifying the observation. Should be unique within an integration. The name should reflect the metrics
    being queried in this observation. e.g. `cache.tomcat` - the metrics defined in this observation are related to
    tomcat cahce, `datasource.tomcat.7` - Tomcat 7 data source metrics.
    * `metricNamespace`: Namespace for all the metrics collected under this integration. This should be unique across all integrations.
    For example, if you are monitoring Jetty metrics, then the namespace will be `jetty` for the Jetty related metrics.
    * `objectName`: JMX ObjectName pattern to query. You can extract tags from the key properties of the object name. For more info,
     refer to [Extracting tags from JMX ObjectName](#extracting-tags-from-jmx-objectname). Used only with JMX data source.
    * `path`: JSONPath-like expression for the objects that should be queried in the response. You can extract tags from the path using placeholders.
     For more info refer to [Extracting tags from JSON Path](#extracting-tags-from-json-path). Used only with JSON data source.
    
    Each observation has a list of metrics and tags under `metric` and `tag` sections.
    * Each `metric` can have following fields:
        * `name`: Name of the metric. Use dot-separated hierarchical naming. Metric names will be automatically prefixed with metric namespace.
        For example, cache related metrics in Tomcat integration are named like `cache.lookups`, `cache.size`, `cache.size.max`, etc. 
        * `source`: The attribute name to query in the metric source. The metric source can also be a function. Refer to
         [Derived Metrics](#derived-metrics) 
        * `label`: Short description of the metric
        * `type`: Metric data type. Refer to [Metric Data Types](#metric-data-types) for possible values
        * `description`: Long description of the metric
        * `send`: `false` if this metric has to be sent. The default value is `true`. Set to false for metrics
         that are extracted as tags or used in the calculation of other metrics. 
        * `stateful`: `true` if the collected metric values have to be preserved across collections. The default value is `false`.
        For example, can be set to true in the case of derived metrics where we need to compare with old values.
        * `pctls`: Comma separated percentile values to be calculated from this metric. e.g. `99,95,50`. For more info
        refer to [Percentiles](#percentiles)
        * `agentAggregation`: Used to override the default aggregation function. When some of the extracted tags are 
         omitted in the definition, the agent aggregates the metric on the omitted tag using the aggregation function 
         based on `type`. By default, counters are aggregated by `SUM` and gauges by `AVG`. This could be overridden by using this field.
         Valid values are `SUM`, `AVG`, `MAX`, `MIN`, `DISCARD`.
        * `unit`: Unit of measurement for this metric e.g. `ms`, `bytes`, etc.
    * Each `tag` must have name and value fields:
        * `name`: Name of the tag. Unlike metric names, tag names won't be prefixed with metric namespace. If a tag denotes the same entity,
        tag names can be reused across YAML files in an integration. For example, `tomcat.web.app` tag is reused across Tomcat 
        integration which represents the web app name.
        * `value`: Reference to the name from where this tag has to be extracted. This could be a metric name defined 
        under the observation or the placeholder in `path` or `objectName`. For metric, use `eval` function
        to refer to metric. You can also use [built-in functions](./built-in-functions.md) to modify the values before ]
        sending to output.

## Metric Data Types

Sematext App Agent supports the following metric data types:

* `gauge`, `long_gauge`:  Gauge is a metric that represents a single numerical value that can arbitrarily go up and down.
   Agent reports gauge metrics as their current value. Examples for gauge metric are current memory usage, 
   current thread count, etc. Default aggregation is `AVG`.
* `counter`, `long_counter`: Counter is a cumulative metric that represents a single monotonically increasing counter 
   whose value can only increase or be reset to zero on restart. Agent reports counter metrics as delta between current and previous measurement.
   Examples for counter metric are number of requests served, cache hits, etc. Default aggregation is `SUM`. 
* `text`: Textual data type. Examples are database name, webapp name, etc. Typically used for metrics that have to be extracted as tags.

## Derived Metrics

Derived metrics are calculated by applying built-in or custom functions on other metrics. Derived metrics can be used to
manipulate the values of metrics before sending them. Some of the use cases are to extract a number from a string 
value or derive a metric as a function of 2 attributes. 

For derived metrics, the `source` field of the metric has to be of the format: `func:<function-name>(<params>)`. 

* `func`: Denotes that metric has to be derived by applying the function specified.
* `<function-name>`: Function to invoke. In the case of [built-in functions](./built-in-functions.md), 
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

Sematext App Agent can automatically calculate percentiles for a metric and send it. The percentiles to be
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

## Specifying Variables in YAML
The App Agent lets you to specify variables in YAML. The variable names can be referenced using `${VARIABLE_NAME}` notation.
The placeholders will be replaced by the agent with respective arguments passed from `setup-spm` script.
It is recommended to use uppercase for the variable names. For example, in the below YAML, the variables will be 
replaced accordingly when passed from `setup-spm` script. If the variable is not passed in `setup-spm` script, 
it will be replaced by an empty string.

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

## Extracting Tags from JMX ObjectName
JMX object name consists of two parts, the domain and the key properties. For example, in the case of JVM memory pool, 
each pool has its own JMX object name instance, where the key `name` refers to pool name. The instances are 
`java.lang:type=MemoryPool,name=Code Cache`, `java.lang:type=MemoryPool,name=Metaspace`, etc. The pool name can be
extracted as tag by specifying the `objectName` pattern as `java.lang:type=MemoryPool,name=${poolName}` and mapping `poolName`
placeholder to the tag in `tag` definition of observation as shown below.
```yaml
- name: jvmMemoryPool
    metricNamespace: jvm
    objectName: java.lang:type=MemoryPool,name=${poolName}
...
    tag:
      - name: jvm.memory.pool
        value: ${poolName}
```
Typically the keys for a given ObjectName pattern are static. In the case of dynamic keys add `*` at the end of pattern for matching 
all object names. For example, refer to [Tomcat Datasource YAML](https://github.com/sematext/sematext-agent-integrations/blob/master/tomcat/jmx-datasource.yml)

## Extracting Tags from JSON Path
For JSON data source, `path` specifies set of objects which should be monitored. For each placeholder (defined
with ${...}) any value will be accepted (while also storing the value of that placeholder). In the example below, if some setup has
3 indices (say 'A', 'B' and 'C'), each with 2 shards, meaning a total of 6 shards, there will be a total of 6
matching objects found by this path expression (regardless of which nodes which shards are on, since the total number of
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

## Adding Custom Classes to the Agent
The App Agent allows loading custom classes (driver libraries or classes for custom functions/conditions). The jars can 
be placed under `/opt/spm/spm-monitor/collectors/<integration>/lib` directory. Jars placed in this location will be 
loaded during agent startup.

## Process or Skip Metrics based on Tag Values
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

## Process or Skip YAMLs based on Conditions
There are use cases where you might need to skip fetching all metrics defined in a YAML file based on some conditions like 
application version, installation type, etc. This condition can be specified in the `require` section. 
You can specify the condition class and pass values to the condition class using `classname` and `value` fields. 
The condition class should extend from `com.sematext.spm.client.observation.BeanConditionCheck`. The agent provides 
a built-in `com.sematext.spm.client.observation.BaseVersionConditionCheck` base class for version check based conditions. 
You can extend from this class and write your implementation to return the actual version. 
The `value` field for version checks can take a specific version or ranges. 
Example values are `7`, `23.1.16`, `0.17-1.33.9` (match any version between specified range), 
`*-1.0` (any version till 1.0), `1.0-*` (any version greater than 1.0). In the below example, the agent fetches the old searcher 
metrics only for Solr version 1 to 6. `com.sematext.spm.client.solr.SolrVersionCheck` is custom condition class that extends
`com.sematext.spm.client.observation.BaseVersionConditionCheck` and reads the version by querying specific JMX attributes.

```yaml
type: jmx
require:
  - className: com.sematext.spm.client.solr.SolrVersionCheck
    values: 1-6

observation:
  - name: searcher_solrOld
    metricNamespace: solr
    objectName: solr/${core}:type=searcher,*

```

## How to group metrics in YAML file
It is recommended to logically group multiple metrics under a single YAML file based on what kind of metrics are collected.
For example, in Tomcat all Thread Pool related metrics are grouped under `jmx-thread-pool.yml` and in MySQL all binlog metrics
are grouped under `db--binlog-stats-status.yml`.

If a single SQL query or URL returns multiple different groups of metrics, they can still be grouped under 
different YAML files with same query or URL as source. 
The agent caches the responses internally and does not issue multiple requests to fetch data from same query/URL when
used across multiple YAML files (the agent ensures a single unique collection query is executed only once). For example,
in case of MySQL, `SHOW /*!50002 GLOBAL */ STATUS` query is used in `db-handler-stats-status.yml`, `db-command-stats-status.yml`, 
`db--binlog-stats-status.yml`, etc. The agent will execute `SHOW /*!50002 GLOBAL */ STATUS` query only once and uses the cached 
result to extract the values for metrics specified in the above YAML files. 
