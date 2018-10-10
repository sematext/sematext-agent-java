# Sematext App Agent

[![Build Status](https://travis-ci.org/sematext/sematext-agent-java.svg?branch=master)](https://travis-ci.org/sematext/sematext-agent-java)

This repository contains the source code for Sematext App Agent. Sematext App Agent can be used to collect application 
metrics from multiple data sources. The data sources and the metrics to be collected can be defined in an YAML file.
There are number of built-in integrations available for various applications in 
[sematext-agent-integration](https://github.com/sematext/sematext-agent-integrations) repo.

Currently supported data sources are:
* JMX
* HTTP REST APIs
* SQL

Sematext App Agent uses Influx Line Protocol to ship the metrics. The metrics collected by the agent can be shipped to
any Influx Line Protocol complaint endpoints like InfluxDB. You can also add support to other output formats like Elasticsearch, etc.

The agent supports number of [built-in functions](/docs/built-in-functions.md) to process the collected metrics before 
sending it to output. You can also plugin-in custom functions. 

## Getting Started

### Build
To build Sematext App Agent you need: 

1) Linux based Operating System 
2) Java 1.6+
3) Maven 
4) Thrift compiler v0.9.3
5) fpm package manager

After cloning the repo, executing `build.sh` will build the packages for multiple Linux distributions.

### Setup
The packages can be installed using OS specific package manager like dpkg, yum, etc. Once installed new App can be 
setup by running `setup-spm` command. e.g. To setup monitoring for JVM application in standalone mode, add 
`-Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=3000 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false`
command line arguments to the Java startup process. And you can setup new app by running following command:

```bash
sudo bash /opt/spm/bin/setup-spm  \
    --app-token <app-token>   \
    --app-type jvm  \
    --agent-type standalone \
    --jmx-params '-Dspm.remote.jmx.url=localhost:3000'
``` 

`<app-token>` - Sematext App Token. App Token is required if you are sending metrics to Sematext. For other Influx endpoints
you can specify a dummy value like `xxxx-xxxx-xxxx-xxxxxx-xxx`. Make sure the token value is unique for each application 
that needs to be monitored in a host. 

Visit [Sematext Documentation](https://sematext.com/docs/monitoring/spm-client/) for more info on how to setup and 
configure agent to ship metrics.

By default the agent sends the collected metrics to Sematext. You can configure different Influx compatible destination
by changing the following properties in `/opt/spm/properties/agent.properties` file:

* `server_base_url` - Base URL of the destination server. e.g. `http://192.168.0.4:8086`
* `metrics_endpoint` - Path to send the metrics. This will be appended with `server_base_url` to form the complete URL.
    Default value is `/write?db=metrics`. You can update this property to send metrics to different endpoint 
    or to specify username/password for InfluxDB. e.g. `/write?db=mydb&u=user&p=pass`
    
## Contributing
We welcome bug fixes or feature enhancements to Sematext App Agent.



