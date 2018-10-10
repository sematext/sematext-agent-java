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

Sematext App Agent uses Influx Line Protocol to ship the metrics. So the metrics collected by the agent can be shipped to
any Influx Line Protocol complaint endpoints. You can also add support to other output formats like Elasticsearch, etc.

The agent support number of [built-in functions](/docs/built-in-functions.md) to process the collected metrics before 
sending it to output. You can also plugin-in custom functions. 

## Getting Started

To build Sematext App Agent you need: 

1) Linux based Operating System 
2) Java 1.6+
3) Maven 
4) Thirft compiler v0.9.3
5) fpm package manager

After cloning the repo, executing `build.sh` will build the packages for multiple Linux distributions. 
