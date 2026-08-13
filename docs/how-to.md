# How-to Guide

## How to monitor metrics for things defined in JSON responses and not known in advance?
In some cases metrics will be exposed via URLs that are not static and need to be dynamically built. For example, a runtime attribute like `jobId` or `workerName` or `bucket` may need to be specified in a URL in order to fetch metrics for a given job, worker, or bucket. The agent can build such URLs by using the derived metric functionality.

Consider the following example:

```
type: json
data:
  url: /api/v1/applications
  server: ${SPARK_API_HOST}:${SPARK_API_PORT}

observation:
  - name: apps
    metricNamespace: spark
    path: $.[?(@.id=${appId} && @.name=${appName})]

    metric:
      - name: applications.uncompleted
        source: json:/api/v1/applications/${appId} $.?(@.id=appId).attempts.[:1].completed
        type: long_gauge
        label: uncompleted apps

    tag:
      - name: spark.app
        value: ${appName}

      - name: spark.app.id
        value: ${appId}
```

In this case there is a known, static stats URL `/api/v1/applications`. The response for this URL provides general info about (Spark) applications. For each distinct entry found by the `path` expression provided here a separate (per-application) observation will be created internally by the agent.

However, to get the info for each application the agent has to call `/api/v1/applications/${appId}` for each "appId", which cannot be known in advance.  Even if they were known ahead of time, it would not make sense to create a configuration for each application. To handle this we can define a derived metric with the following definition:

`source: json:/api/v1/applications/${appId} $.?(@.id=appId).attempts.[:1].completed`

Each application observation will monitor one metric (`applications.uncompleted`) by calling the
`/api/v1/applications/${appId}` URL and applying the `$.?(@.id=appId).attempts.[:1].completed` expression on it to extract the metric value.

Note that when the number of dynamic elements (such as applications in this example) is high, a config like this would cause a high number of additional requests to be sent to the monitored service so one has to be careful when using it.

## How to find out whether a metric is defined with the wrong type (gauge vs counter)?
Mixing up `counter` and `gauge` types is easy to do when writing a new integration and hard to notice afterwards, since
the agent will happily collect and send such a metric. To help with that, the agent can watch the values it reads from
the monitored source and warn about metrics whose values don't behave like their type says they should:

* a metric defined as `gauge` whose value only ever grows is most likely a `counter`
* a metric defined as `counter` whose value regularly drops is most likely a `gauge`

The check is done on values as they were read from the source (before counter values are turned into deltas) and a
metric is reported only after at least 30 of its values were collected, so an occasional counter reset (a restart of
the monitored service, for example) is not reported. Every metric is reported at most once.

Since this check keeps some state for every collected metric, it is done only when the agent runs with `DEBUG` logging
level, which is set in the app's monitor config properties file:

```
SPM_MONITOR_LOGGING_LEVEL=DEBUG
```

The findings are written into the agent log, for example:

```
WARN [spm-monitor] com.sematext.spm.client.observation.MetricTypeMismatchDetector - Metric solr.cache:cache.lookups is defined as a gauge, but its source value only grew while 30 values were collected, which is how a counter behaves. Check the 'type' of that metric in its config.
```
