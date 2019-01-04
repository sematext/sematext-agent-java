# How-to Guide

## How to monitor metrics on JSON URLs which are not known in advance but are instead defined by runtime data?
In some cases metrics will be exposed on URLs you can't know in advance. E.g., some runtime attribute like `jobId` or
`workerName` or `bucket` can be part of URL. Agent can monitor such URLs by using derived metric.

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

In this case there is a known stats URL `/api/v1/applications` which provides general info about applications. For each
distinct entry found by `path` expression, separate (per-app) observation will be created internally by the agent.
However, details of each application are on separate URLs `/api/v1/applications/appId` which can't be known in advance
(and even if they are known, it wouldn't make sense to create configuration for each app). However, we can define
derived metric with definition:

`source: json:/api/v1/applications/${appId} $.?(@.id=appId).attempts.[:1].completed`

Each app observation will monitor one metric (`applications.uncompleted`) which will be monitored by querying URL
`/api/v1/applications/${appId}` and running the expression `$.?(@.id=appId).attempts.[:1].completed` on it to provide metric value.

Note that in cases when there are 100s or 1000s of applications, config like this would cause a big number of additional
requests sent to monitored service so one has to be careful when using it.