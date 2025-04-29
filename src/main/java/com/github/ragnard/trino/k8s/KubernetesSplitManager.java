/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.ragnard.trino.k8s;

import com.github.ragnard.trino.k8s.client.KubernetesLogs;
import com.github.ragnard.trino.k8s.functions.PodLogsTableFunctionHandle;
import com.github.ragnard.trino.k8s.functions.PodLogsTableFunctionSplit;
import com.google.inject.Inject;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodSpec;
import io.trino.spi.connector.ConnectorSession;
import io.trino.spi.connector.ConnectorSplitManager;
import io.trino.spi.connector.ConnectorSplitSource;
import io.trino.spi.connector.ConnectorTableHandle;
import io.trino.spi.connector.ConnectorTransactionHandle;
import io.trino.spi.connector.Constraint;
import io.trino.spi.connector.DynamicFilter;
import io.trino.spi.connector.FixedSplitSource;
import io.trino.spi.function.table.ConnectorTableFunctionHandle;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class KubernetesSplitManager
        implements ConnectorSplitManager
{
    private final KubernetesLogs kubernetesLogs;

    @Inject
    public KubernetesSplitManager(KubernetesLogs kubernetesLogs)
    {
        this.kubernetesLogs = kubernetesLogs;
    }

    @Override
    public ConnectorSplitSource getSplits(ConnectorTransactionHandle transaction, ConnectorSession session, ConnectorTableHandle table, DynamicFilter dynamicFilter, Constraint constraint)
    {
        return new FixedSplitSource(new KubernetesSplit((KubernetesTableHandle) table));
    }

    @Override
    public ConnectorSplitSource getSplits(ConnectorTransactionHandle transaction, ConnectorSession session, ConnectorTableFunctionHandle functionHandle)
    {
        var handle = (PodLogsTableFunctionHandle) functionHandle;

        var pods = this.kubernetesLogs.getPods(handle.namespace(), handle.selector());

        var splits = pods.stream()
                .flatMap(pod -> Optional.ofNullable(pod).map(V1Pod::getSpec).map(V1PodSpec::getContainers).orElse(List.of())
                        .stream()
                        .map(c -> new PodLogsTableFunctionSplit(handle, Objects.requireNonNull(pod.getMetadata()).getName(), c.getName())))
                .filter(split -> handle.container().map(c -> c.equals(split.container())).orElse(true))
                .toList();

        return new FixedSplitSource(splits);
    }
}
