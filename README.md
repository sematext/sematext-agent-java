# Sematext App Agent

[![Build Status](https://travis-ci.org/sematext/sematext-agent-java.svg?branch=master)](https://travis-ci.org/sematext/sematext-agent-java)

This repository contains the source code for Sematext App Agent. Sematext App Agent can be used to collect application 
metrics from multiple data sources. The data sources and the metrics to be collected can be defined in 
[Metrics Configuration YAML](/docs/metrics-yaml-format.md) files.
There are number of built-in integrations available for various applications in 
[sematext-agent-integrations](https://github.com/sematext/sematext-agent-integrations) repo.

## Data Sources
The supported data sources are:
* JMX
* HTTP REST APIs
* SQL

Sematext App Agent uses Influx Line Protocol to ship the metrics. The metrics collected by the agent can be shipped to
any Influx Line Protocol compatible endpoints like InfluxDB. In the future, we will add support for other output formats
like HTTP, Graphite, etc.

## Configuration
The Agent supports a number of [built-in functions](/docs/built-in-functions.md) to process the collected metrics before 
sending them to output. You can also plug-in custom functions.

The [How-to Guide](/docs/how-to.md) describes how to configure the App Agent in some specific cases. 

## Getting Started

### Build
To build Sematext App Agent you need: 

1) Linux based Operating System 
2) Java 1.6+
3) Maven 
4) Thrift compiler v0.9.3
    * Steps to install Thrift in Debian based systems
        ```bash
          sudo apt-get install automake bison flex g++ git libboost-all-dev libevent-dev libssl-dev libtool make pkg-config
          wget http://www.us.apache.org/dist/thrift/0.9.3/thrift-0.9.3.tar.gz
          tar xfz thrift-0.9.3.tar.gz
          cd thrift-0.9.3 && ./configure --enable-libs=no  && sudo make install
        ```
5) fpm package manager 
    * For steps to install fpm refer [https://fpm.readthedocs.io/en/latest/installing.html](https://fpm.readthedocs.io/en/latest/installing.html)

After cloning the repo, executing `build.sh` will build the packages for multiple Linux distributions.

### Docker

Docker image building for Sematext App Agent is triggered by Maven target:

```bash
$ sudo mvn clean install dockerfile:build
```

If Docker daemon is listening on TCP socket, you can set `DOCKER_HOST` environment variable and start
the build with regular user:

```bash
DOCKER_HOST=tcp://0.0.0.0:2375 mvn clean install dockerfile:build
```

Once the image is built, launching a new container with Sematext App Agent can be achieved with the following command:

```bash
sudo docker run -i -t --name solr-app-agent -e MONITORING_TOKEN=<monitoring-token> -e AGENT_TYPE=standalone -e APP_TYPE=solr -e JMX_PARAMS=-Dspm.remote.jmx.url=172.17.0.4:3000 spm-client:version
```


### Set up
The packages can be installed using OS specific package manager like dpkg, yum, etc. Once installed a new App can be 
set up by running `setup-spm` command. For example, to set up monitoring for a JVM application in standalone mode, add

```bash
-Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=3000 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false
```
to startup arguments of Java process you wish to monitor. Then you can set up new App by running the following command:

```bash
sudo bash /opt/spm/bin/setup-spm  \
    --monitoring-token <monitoring-token>   \
    --app-type jvm  \
    --agent-type standalone \
    --jmx-params '-Dspm.remote.jmx.url=localhost:3000'
```

`<monitoring-token>` - Monitoring Token should point to Sematext App Token if you are sending metrics to Sematext. App is an entity to 
group similar/related metrics. e.g. All Elasticsearch metrics can be grouped under Elasticsearch App. Each App has a unique token.
For other Influx endpoints you can specify a hexadecimal value with format `xxxxxxxx--xxxx-xxxx-xxxx-xxxxxxxxxxxx` 
e.g. `d0add28a-0a0f-46b2-9e1e-4928db5200e7`.

Visit [Sematext Documentation](https://sematext.com/docs/monitoring/spm-client/) for more info on how to set up and 
configure the agent to ship metrics.

By default, the agent sends the collected metrics to Sematext. You can configure a different Influx compatible destination
by changing the following properties in `/opt/spm/properties/agent.properties` file:

* `server_base_url` - Base URL of the destination server. e.g. `http://192.168.0.4:8086`
* `metrics_endpoint` - Path to send the metrics. This will be appended with `server_base_url` to form the complete URL.
    Default value is `/write?db=metrics`. You can update this property to send metrics to different endpoint 
    or to specify username/password for InfluxDB. e.g. `/write?db=mydb&u=user&p=pass`
    
## Contributing
We welcome bug fixes or feature enhancements to Sematext App Agent. When done working on and testing,
just submit a pull request to have Sematext review and merge your changes. 

To add support to new integration refer to [Adding a New Agent Integration](/docs/add-new-integration.md).

To modify built-in integrations refer to [Modifying built-in integrations](/docs/modify-integration.md) 





