# Sematext Java Agent

[![Build Status](https://travis-ci.org/sematext/sematext-agent-java.svg?branch=master)](https://travis-ci.org/sematext/sematext-agent-java)

TODO

## Built-in functions
Sometimes it is not enough to just gather value of particular metric or tag. Instead, more complex calculation may be needed, based on input of 1 or N monitored attributes etc. Common case is transformation of attribute which is expressed in MB into its bytes equivalent or conversion of time attribute which is expressed in nanoseconds into ms value. For such cases Agent provides various built-in functions. They can be used out-of-the-box in yml configurations.
Functions can be applied both on metrics and on tags using ```func:``` prefix followed by function name and params. Params are either names of other metrics (when used to calculate a metric) or names of path tags (when used to calculate a tag value) or values that will be used directly inside of the function (function specification explains the meaning of each param).

Few examples of usage:

1) DoubleDivide - produces double value based on division of two other metrics. In this case, resulting metric will be named ```query.latency.primaries.avg``` and it is calculated based on values of two other metrics (they have to be part of the same observation definition) - ```query.latency.time.primaries``` and ```query.count.primaries```
```
      - name: query.latency.primaries.avg
        source: func:DoubleDivide(query.latency.time.primaries,query.count.primaries)
        type: gauge
        agentAggregation: AVG
        pctls: 99,95,50
        label: avg. query latency (primaries)
        description: avg. query latency on primary shards
        unit: ms
```

2) DirSize - calculates size of dir whose name is passed in as a value of other monitored attribute (in this case its name being ```Value```)
```
      - name: index.files.size
        source: func:DirSize(Value)
        type: gauge
        label: index size on the disk
        description: size of solr index on the disk
        unit: bytes
```


There are two groups of built-in functions:
- universal (applicable to any type of monitored service, e.g. sum or multiply)
- specific to particular service type (e.g. function that calculates Solr warmup time)

### Universal built-in functions:
- BoolToInt - converts metric of boolean/string value into int (false -> 0, true -> 1)

```
      - name: upstream.health.passed
        source: func:BoolToInt(passed)
        type: gauge
        label: upstream health last passed
        description: Value indicating if the last health check request was successful and passed tests
```

- ConcatUsingString - concats values of N path tags (or N metrics) using concat string (defined with first param) into single string value. This function is intended to be used to calculate tag values, however, it can be used to calculate textual metrics as well. It can accept any number of tags/metrics after the concat string.

```
      - name: solr.core
        value: func:ConcatUsingString(_,collection,shard,replica)
```

- CountFiles - returns count of files in specific directory. Takes one parameter - name of the metric which holds directory name. Ignores the subdirs!

```
      - name: index.files
        source: func:CountFiles(Value)
        type: gauge
        label: index num of files
        description: number of files in solr index
```

- DirSize - returns the size of specific directory. Takes one parameter - name of the metric which holds directory name. Takes into account all subdirs (meaning, result is total sum of dir and its subdirs sizes).

```
      - name: index.files.size
        source: func:DirSize(Value)
        type: gauge
        label: index size on the disk
        description: size of solr index on the disk
        unit: bytes
```

- DoubleDivide - produces double value based on division of two other metrics (result is firstParam/secondParam).

```
      - name: requests.latency.avg
        source: func:DoubleDivide(totalTime,requests)
        type: double_gauge
        stateful: true
        pctls: 99,95,50
        label: avg. request latency
        description: avg. request latency
```

- DoubleMultiply - produces double value based on multiplication of two other metrics.

```
      - name: broker.requests.time.local
        source: func:DoubleMultiply(broker.requests.time.local.count, broker.requests.mean.time.local)
        type: double_counter
        label: broker requests local time
        unit: ms
```

- DoubleMultiplyWithConstant - produces double value which is a result of multiplication of value contained in metric specified with first param and value (not metric) specified as a second param.

```
      - name: duration.seconds
        source: func:DoubleMultiplyWithConstant(duration.minutes,60)
        type: long_gauge
        label: duration
        unit: sec
```

- IfThenElse - produces a value based on value of some other metric. Can be used to calculate only metric value (not applicable on tags). Signature: IfThenElse(metric_name_to_compare, operator, value_to_compare, value_to_return_if_true, value_to_return_if_false)

```
      # When web.execution.time.min.raw value is Long.MAX_VALUE return 0.
      - name: web.execution.time.min
        source: func:IfThenElse(web.execution.time.min.raw, =, 0x7fffffffffffffffL, 0L, metric:web.execution.time.min.raw)
        type: long_gauge
        label: min servlet processing time
        description: Minimum execution time of all servlets in this context
        unit: ms
```

- IfThenElseString - similar to IfThenElse function except it produces literal value as result and can be used both on metrics and on tags.

```
      - name: tomcat.web.app
        value: func:IfThenElseString(webapp_name,, /, tag:webapp_name)
```

- LongDivide - like DoubleDivide except it produces Long value as result.
- LongMultiply - like DoubleMultiply except it produces Long value as result.
- LongMultiplyWithConstant - like DoubleMultiplyWithConstant except it produces Long value as result.

```
      - name: cache.size
        source: func:LongMultiplyWithConstant(cache.size.kb,1024)
        type: long_gauge
        label: size
        description: current cache size
        unit: bytes
```

- LongSubtract - produces result of long type calculated by subtracting second param from first param

```
      - name: batches.received.scheduling.delay
        source: func:LongSubtract(receivedStart,receivedSubmit)
        type: long_gauge
        label: last received batch scheduling time
        unit: ms
```

- SplitAndExtract - Splits the value of specified metric name (first param) using the token (second param) and returns the value at specified index in the array (index being third param). Can be used both on metrics and tags.

```
    tag:
      - name: tomcat.connector.port
        value: func:SplitAndExtract(host_port,-,1)
```

- StringEquals - Compares string values. First argument is field name. Second is string which should be used to compare. Optional third argument is ignore case. By default ignore case is false. Returns 1/0 for true/false, based on the result of equals.
```
      - name: upstream.state.up
        source: func:StringEquals(upstream.server.state,up)
        type: gauge
        label: state up
        description: Server is up
```

- TimeSince - Returns time in ms passed since event was accured. Simple calculation of now() - time_contained_in_metric_param

```
      - name: uptime.ms
        source: func:TimeSince(broker.start.time.ms)
        type: double_gauge
        unit: ms
```

- TrimUnit - Trims the specified unit string from the end of value of the metricName and returns Long. E.g. TrimUnit(Value,ms) - trims `ms` from  the end of value of metric name `Value`

```
      - name: indexing.commits.auto.time.max
        source: func:TrimUnit(autocommit maxTime,ms)
        type: gauge
        label: autocommit max time
        description: autocommit max time
        unit: ms
```

### Service type specific built-in functions:
- com.sematext.spm.client.hadoop.CalculateNumNodes - hadoop specific. The only param is metric name mapped to hadoop jmx attribute LiveNodes.

```
      - name: nn.nodes.live
        source: func:com.sematext.spm.client.hadoop.CalculateNumNodes(nodes.live)
        type: long_gauge
        label: live nodes
```

- com.sematext.spm.client.solr.CalculateCoreNumFiles - solr specific, calculates number of files for some solr core (can't use universal CountFiles function due to Solr internal specifics)

```
      - name: index.files
        source: func:com.sematext.spm.client.solr.CalculateCoreNumFiles(readerDir,true)
        type: long_gauge
        label: index num of files
        description: number of files in solr index
```

- com.sematext.spm.client.solr.CalculatePreSolr7TotalTime - used to calculate req handler total time for pre 7 versions of Solr. Takes no params, expects metrics ```requests```, ```totalTime``` and ```avgTimePerRequest``` to collected under the same observation.

```
      - name: requests.time
        source: func:com.sematext.spm.client.solr.CalculatePreSolr7TotalTime
        type: counter
        stateful: true
        label: request time
        description: request time
        unit: ms
```

- com.sematext.spm.client.solr.CalculateReplicaName - extracts the name of particular core replica. Useful only for pre 7 versions of Solr. Expects ```core``` path tag to be present in the context.

```
      - name: solr.replica
        value: func:com.sematext.spm.client.solr.CalculateReplicaName
```

- com.sematext.spm.client.solr.CalculateSegmentsCount - calculates number of segments in particular Solr core based on ```reader``` name value.

```
      - name: index.segments
        source: func:com.sematext.spm.client.solr.CalculateSegmentsCount(Value)
        type: gauge
        label: index segments
        description: index segments count
```

- com.sematext.spm.client.solr.CalculateSolrFieldCacheTotalSize - extracts solr field cache size from textual field (as exposed by Solr). Expects metric ```total_size``` to be collected by the same observation.

```
      - name: cache.size.bytes
        source: func:com.sematext.spm.client.solr.CalculateSolrFieldCacheTotalSize
        type: gauge
        label: cache memory used
        description: cache size in bytes
        unit: bytes
```

- com.sematext.spm.client.solr.CalculateWarmupTime - checks if searcher changed since last measurement, if it did, records its warmup time, otherwise it produces null (no metric)

```
      - name: warmup.time
        source: func:com.sematext.spm.client.solr.CalculateWarmupTime(warmupTime,searcherName)
        type: counter
        stateful: true
        label: warmup time
        description: warmup time
        unit: ms
```

- com.sematext.spm.client.solr.ExtractAutowarmSetting - extracts ```autowarmCount``` value from textual ```description``` field (must be specified under the same observation). Usable only for pre 7 versions of Solr.

```
      - name: cache.autowarm.count
        source: func:com.sematext.spm.client.solr.ExtractAutowarmSetting
        type: gauge
        label: autowarm count or %
        description: cache autowarm count or %
```

- com.sematext.spm.client.solr.ExtractCacheMaxSize - extracts ```maxSize``` value from textual ```description``` field (must be specified under the same observation). Usable only for pre 7 versions of Solr.

```
      - name: cache.size.max
        source: func:com.sematext.spm.client.solr.ExtractCacheMaxSize
        type: gauge
        label: cache max size
        description: cache max size
```
