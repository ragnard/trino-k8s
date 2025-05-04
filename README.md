# trino-k8s

A connector plugin for [Trino](https://trino.io) that exposes
Kubernetes resources as tables.

## Status

Not ready for use.

## Installation

- Download plugin ZIP from [latest release](https://github.com/ragnard/trino-k8s/releases/latest)
- Follow [plugin installation instructions](https://trino.io/docs/current/installation/plugins.html#installation).

## Usage

Configure a catalog that uses the connector by creating a
`${catalog-name}.properties` file with:

```
connector.name=kubernetes
```

### Client config

The kubernetes client is configured using the standard approach:

- if `KUBECONFIG` env variable is set, use the file pointed to
- if a `.kube/config` file exists, use that
- if a `/var/run/secrets/kubernetes.io/serviceaccount/ca.crt` file
  exists, assume we running in a cluster and use injected
  serviceaccount

So, when running in a cluster you can either:

- supply a config map with a valid kubeconfig file, and set KUBECONFIG env variable to point to that
- use a serviceaccount


### Resources as tables

The connector provides a `resources`, which contains a table for each
kubernetes resource type (kind/apiVersion).

For example, if you have configured a `kubernetes` catalog called
`mycluster` you can list all pods using:

```
select
  *
from
  mycluster.resources.pods
```

Namespace filtering and limits are pushed down to the the Kubernetes API.


### Pog logs as a table function

The connector provides a table function `system.logs` that can be used
to retrieve pod logs.

Function signature:

```sql
system.logs(
  namespace,           -- required
  selector,            -- required: NAME or TYPE/NAME like deployment/nginx
  container,           -- optional: defaults to all containers in pod
  limit                -- optional: number of log lines to retrieve, defaults to all
)
```

For example:

```sql
select
  *
from
  table(
    mycluster.system.logs(
      namespace => 'kube-system',
      selector => 'etcd-kind-control-plane',
      limit => 10
    )
  )
order by
  timestamp desc
```

```
           pod           | container |             timestamp             |                                                                                                                                                                   log
-------------------------+-----------+-----------------------------------+------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
 etcd-kind-control-plane | etcd      | 2025-05-02 18:16:50.971692535 UTC | {"level":"info","ts":"2025-05-02T18:16:50.971536Z","caller":"mvcc/hash.go:151","msg":"storing new hash","hash":2764698308,"revision":3946441,"compact-revision":3945720}
 etcd-kind-control-plane | etcd      | 2025-05-02 18:16:50.971678050 UTC | {"level":"info","ts":"2025-05-02T18:16:50.971502Z","caller":"mvcc/kvstore_compaction.go:72","msg":"finished scheduled compaction","compact-revision":3946441,"took":"16.033513ms","hash":2764698308,"current-db-size-bytes":7766016,"current-db-size":"7.8 MB","current-db-size-in-use-bytes":2457600,"current-db-size-in-use":"2.5 MB"}
 etcd-kind-control-plane | etcd      | 2025-05-02 18:16:50.955430716 UTC | {"level":"info","ts":"2025-05-02T18:16:50.955282Z","caller":"mvcc/index.go:214","msg":"compact tree index","revision":3946441}
 etcd-kind-control-plane | etcd      | 2025-05-02 18:11:50.938719909 UTC | {"level":"info","ts":"2025-05-02T18:11:50.938437Z","caller":"mvcc/hash.go:151","msg":"storing new hash","hash":1341677485,"revision":3945720,"compact-revision":3945001}
 etcd-kind-control-plane | etcd      | 2025-05-02 18:11:50.938624033 UTC | {"level":"info","ts":"2025-05-02T18:11:50.938330Z","caller":"mvcc/kvstore_compaction.go:72","msg":"finished scheduled compaction","compact-revision":3945720,"took":"22.420579ms","hash":1341677485,"current-db-size-bytes":7766016,"current-db-size":"7.8 MB","current-db-size-in-use-bytes":2465792,"current-db-size-in-use":"2.5 MB"}
 etcd-kind-control-plane | etcd      | 2025-05-02 18:11:50.915702058 UTC | {"level":"info","ts":"2025-05-02T18:11:50.915318Z","caller":"mvcc/index.go:214","msg":"compact tree index","revision":3945720}
 etcd-kind-control-plane | etcd      | 2025-05-02 18:06:50.913550096 UTC | {"level":"info","ts":"2025-05-02T18:06:50.913138Z","caller":"mvcc/hash.go:151","msg":"storing new hash","hash":579205076,"revision":3945001,"compact-revision":3944282}
 etcd-kind-control-plane | etcd      | 2025-05-02 18:06:50.913488865 UTC | {"level":"info","ts":"2025-05-02T18:06:50.913017Z","caller":"mvcc/kvstore_compaction.go:72","msg":"finished scheduled compaction","compact-revision":3945001,"took":"41.223804ms","hash":579205076,"current-db-size-bytes":7766016,"current-db-size":"7.8 MB","current-db-size-in-use-bytes":2482176,"current-db-size-in-use":"2.5 MB"}
 etcd-kind-control-plane | etcd      | 2025-05-02 18:06:50.871530963 UTC | {"level":"info","ts":"2025-05-02T18:06:50.871118Z","caller":"mvcc/index.go:214","msg":"compact tree index","revision":3945001}
 etcd-kind-control-plane | etcd      | 2025-05-02 18:05:22.551798533 UTC | {"level":"info","ts":"2025-05-02T18:05:22.551445Z","caller":"fileutil/purge.go:96","msg":"purged","path":"/var/lib/etcd/member/snap/000000000000000c-0000000000408d1b.snap"}
(10 rows)

Query 20250502_182111_00019_zs354, FINISHED, 1 node
Splits: 7 total, 7 done (100.00%)
0.09 [0 rows, 0B] [0 rows/s, 0B/s]
```
