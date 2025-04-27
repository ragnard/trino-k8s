# trino-k8s

A connector plugin for [Trino](https://trino.io) that exposes
Kubernetes resources as tables.

## Status

Not ready for use.

## Installation

- Download plugin ZIP from [latest release](https://github.com/ragnard/trino-k8s/releases/latest)
- Follow [plugin installation instructions](https://trino.io/docs/current/installation/plugins.html#installation).

## Usage

Configure an instance of the connector by creating a `${connector-instance-name}.properties` file with:

```
connector.name=kubernetes
```

The connector provides a single schema, `resources`, which contains a
table for each kubernetes resource type (kind/apiVersion).

For example, using a file called `kubernetes.properties` with the above configuration, you can list all pods using:

```
select * from kubernetes.resources.pods
```
