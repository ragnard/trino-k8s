# trino-k8s

A connector plugin for [Trino](https://trino.io) that:

- exposes Kubernetes resources as tables
- provides a table function for querying pod logs


## Status

Almost ready for use.


## Quickstart

If you have a `~/.kube/config` locally, you can try trino-k8s quite
easily by:

- starting a Trino server container with a kubernetes catalog and your
  kubeconfig mounted
- start an interactive Trino client container connected to the server

To aid with this you can use this
[`docker-compose.yaml`](hack/docker-compose.yaml), for example:

**1. Start trino-server**

```
curl -s https://raw.githubusercontent.com/ragnard/trino-k8s/refs/heads/main/hack/docker-compose.yaml | docker compose -f - up
```

**2. Connect using trino cli**

```
docker run --network=host -it ghcr.io/ragnard/trino-k8s trino http://127.0.0.1:8080
```

**3. Run some commands**

```
trino> select count(*) from kubernetes.resources.pods;
4711
```

See [examples](#examples) for some inspiration.

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
  container            -- optional: defaults to all containers in pod
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
      selector => 'etcd-kind-control-plane'
    )
  )
order by
  timestamp desc
limit 100
```

## Examples

<details>
<summary>Show catalogs</summary>

```
trino> show catalogs;
```

```
  Catalog
------------
 kubernetes
 system
(2 rows)

Query 20250507_163100_00000_p98bd, FINISHED, 1 node
Splits: 7 total, 7 done (100.00%)
0.98 [0 rows, 0B] [0 rows/s, 0B/s]
```

</details>

<details>
<summary>Show available resource tables</summary>

```
trino> show tables in kubernetes.tables;
```

```
                             Table
----------------------------------------------------------------
 admissionregistration.k8s.io.mutatingwebhookconfigurations
 admissionregistration.k8s.io.validatingadmissionpolicies
 admissionregistration.k8s.io.validatingadmissionpolicybindings
 admissionregistration.k8s.io.validatingwebhookconfigurations
 apiextensions.k8s.io.customresourcedefinitions
 apiregistration.k8s.io.apiservices
 apps.controllerrevisions
 apps.daemonsets
 apps.deployments
 apps.replicasets
 apps.statefulsets
 authentication.k8s.io.selfsubjectreviews
 authentication.k8s.io.tokenreviews
 authorization.k8s.io.localsubjectaccessreviews
 authorization.k8s.io.selfsubjectaccessreviews
 authorization.k8s.io.selfsubjectrulesreviews
 authorization.k8s.io.subjectaccessreviews
 autoscaling.horizontalpodautoscalers
 batch.cronjobs
 batch.jobs
 bindings
 certificates.k8s.io.certificatesigningrequests
 componentstatuses
 configmaps
 coordination.k8s.io.leases
 discovery.k8s.io.endpointslices
 endpoints
 events
 events.k8s.io.events
 flowcontrol.apiserver.k8s.io.flowschemas
 flowcontrol.apiserver.k8s.io.prioritylevelconfigurations
 helm.cattle.io.helmchartconfigs
 helm.cattle.io.helmcharts
 limitranges
 namespaces
 networking.k8s.io.ingressclasses
 networking.k8s.io.ingresses
 networking.k8s.io.networkpolicies
 node.k8s.io.runtimeclasses
 nodes
 persistentvolumeclaims
 persistentvolumes
 pods
 podtemplates
 policy.poddisruptionbudgets
 rbac.authorization.k8s.io.clusterrolebindings
 rbac.authorization.k8s.io.clusterroles
 rbac.authorization.k8s.io.rolebindings
 rbac.authorization.k8s.io.roles
 replicationcontrollers
 resourcequotas
 scheduling.k8s.io.priorityclasses
 secrets
 serviceaccounts
 services
 storage.k8s.io.csidrivers
 storage.k8s.io.csinodes
 storage.k8s.io.csistoragecapacities
 storage.k8s.io.storageclasses
 storage.k8s.io.volumeattachments
(60 rows)

Query 20250507_163511_00002_p98bd, FINISHED, 1 node
Splits: 7 total, 7 done (100.00%)
0.48 [60 rows, 2.73KiB] [126 rows/s, 5.76KiB/s]
```

</details>


<details>
<summary>Query pods</summary>

```
select * from kubernetes.resources.pods where namespace = 'kube-system';
```

```
 kind | group | apiversion |                    name                    |  namespace  |                                                  labels                                               >
------+-------+------------+--------------------------------------------+-------------+------------------------------------------------------------------------------------------------------->
 Pod  |       | v1         | coredns-668d6bf9bc-452d8                   | kube-system | {pod-template-hash=668d6bf9bc, k8s-app=kube-dns}                                                      >
 Pod  |       | v1         | coredns-668d6bf9bc-ms6bx                   | kube-system | {pod-template-hash=668d6bf9bc, k8s-app=kube-dns}                                                      >
 Pod  |       | v1         | etcd-kind-control-plane                    | kube-system | {component=etcd, tier=control-plane}                                                                  >
 Pod  |       | v1         | kindnet-b5jd7                              | kube-system | {app=kindnet, controller-revision-hash=5d87d5ccb4, tier=node, pod-template-generation=1, k8s-app=kindn>
 Pod  |       | v1         | kube-apiserver-kind-control-plane          | kube-system | {component=kube-apiserver, tier=control-plane}                                                        >
 Pod  |       | v1         | kube-controller-manager-kind-control-plane | kube-system | {component=kube-controller-manager, tier=control-plane}                                               >
 Pod  |       | v1         | kube-proxy-bsbf7                           | kube-system | {controller-revision-hash=7bb84c4984, pod-template-generation=1, k8s-app=kube-proxy}                  >
 Pod  |       | v1         | kube-scheduler-kind-control-plane          | kube-system | {component=kube-scheduler, tier=control-plane}                                                        >
(8 rows)

Query 20250507_163755_00004_p98bd, FINISHED, 1 node
Splits: 1 total, 1 done (100.00%)
0.15 [8 rows, 65.5KiB] [54 rows/s, 449KiB/s]
```

</details>

<details>
<summary>Query pod logs for pod <tt>etcd-kind-control-plane</tt></summary>

```
select * from
  table(
    kubernetes.system.logs(
      namespace => 'kube-system',
      selector => 'etcd-kind-control-plane'
    )
  )
order by
  timestamp desc
limit 10
```

```
           pod           | container |             timestamp             |                                                                                                                    >
-------------------------+-----------+-----------------------------------+-------------------------------------------------------------------------------------------------------------------->
 etcd-kind-control-plane | etcd      | 2025-05-07 16:42:24.979542808 UTC | {"level":"info","ts":"2025-05-07T16:42:24.979110Z","caller":"mvcc/hash.go:151","msg":"storing new hash","hash":3427>
 etcd-kind-control-plane | etcd      | 2025-05-07 16:42:24.979399915 UTC | {"level":"info","ts":"2025-05-07T16:42:24.979008Z","caller":"mvcc/kvstore_compaction.go:72","msg":"finished schedul>
 etcd-kind-control-plane | etcd      | 2025-05-07 16:42:24.956899126 UTC | {"level":"info","ts":"2025-05-07T16:42:24.956403Z","caller":"mvcc/index.go:214","msg":"compact tree index","revisio>
 etcd-kind-control-plane | etcd      | 2025-05-07 16:37:24.936685776 UTC | {"level":"info","ts":"2025-05-07T16:37:24.936399Z","caller":"mvcc/hash.go:151","msg":"storing new hash","hash":2757>
 etcd-kind-control-plane | etcd      | 2025-05-07 16:37:24.936627070 UTC | {"level":"info","ts":"2025-05-07T16:37:24.936290Z","caller":"mvcc/kvstore_compaction.go:72","msg":"finished schedul>
 etcd-kind-control-plane | etcd      | 2025-05-07 16:37:24.910503017 UTC | {"level":"info","ts":"2025-05-07T16:37:24.909966Z","caller":"mvcc/index.go:214","msg":"compact tree index","revisio>
 etcd-kind-control-plane | etcd      | 2025-05-07 16:32:24.908258887 UTC | {"level":"info","ts":"2025-05-07T16:32:24.908085Z","caller":"mvcc/hash.go:151","msg":"storing new hash","hash":3598>
 etcd-kind-control-plane | etcd      | 2025-05-07 16:32:24.908223346 UTC | {"level":"info","ts":"2025-05-07T16:32:24.908020Z","caller":"mvcc/kvstore_compaction.go:72","msg":"finished schedul>
 etcd-kind-control-plane | etcd      | 2025-05-07 16:32:24.889477453 UTC | {"level":"info","ts":"2025-05-07T16:32:24.889238Z","caller":"mvcc/index.go:214","msg":"compact tree index","revisio>
 etcd-kind-control-plane | etcd      | 2025-05-07 16:27:24.865262172 UTC | {"level":"info","ts":"2025-05-07T16:27:24.864860Z","caller":"mvcc/hash.go:151","msg":"storing new hash","hash":1170>
(10 rows)

Query 20250507_164349_00005_p98bd, FINISHED, 1 node
Splits: 7 total, 7 done (100.00%)
0.27 [0 rows, 0B] [0 rows/s, 0B/s]
```
