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
