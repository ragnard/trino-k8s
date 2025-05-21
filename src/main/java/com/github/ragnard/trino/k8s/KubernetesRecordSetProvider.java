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

import com.github.ragnard.trino.k8s.client.KubernetesResources;
import com.github.ragnard.trino.k8s.client.KubernetesLogs;
import com.github.ragnard.trino.k8s.logs.PodLogsTableFunctionSplit;
import com.github.ragnard.trino.k8s.resources.ResourceTableSplit;
import com.google.inject.Inject;
import io.trino.spi.connector.ColumnHandle;
import io.trino.spi.connector.ConnectorRecordSetProvider;
import io.trino.spi.connector.ConnectorSession;
import io.trino.spi.connector.ConnectorSplit;
import io.trino.spi.connector.ConnectorTableHandle;
import io.trino.spi.connector.ConnectorTransactionHandle;
import io.trino.spi.connector.RecordSet;

import java.util.List;

public class KubernetesRecordSetProvider
        implements ConnectorRecordSetProvider
{
    private final KubernetesResources kubernetesResources;
    private final KubernetesLogs kubernetesLogs;

    @Inject
    public KubernetesRecordSetProvider(
            KubernetesResources kubernetesResources,
            KubernetesLogs kubernetesLogs)
    {
        this.kubernetesResources = kubernetesResources;
        this.kubernetesLogs = kubernetesLogs;
    }

    @Override
    public RecordSet getRecordSet(
            ConnectorTransactionHandle connectorTransactionHandle,
            ConnectorSession connectorSession,
            ConnectorSplit connectorSplit,
            ConnectorTableHandle tableHandle,
            List<? extends ColumnHandle> columnHandles)
    {
        @SuppressWarnings("unchecked")
        var columns = (List<KubernetesColumnHandle>) columnHandles;

        return switch (connectorSplit) {
            case ResourceTableSplit s -> getRecordSet(s, columns);
            case PodLogsTableFunctionSplit s -> getRecordSet(s, columns);
            default -> throw new IllegalStateException("Unexpected value: " + connectorSplit);
        };
    }

    private RecordSet getRecordSet(ResourceTableSplit split, List<KubernetesColumnHandle> columnHandles)
    {
        return kubernetesResources.execute(split.tableHandle(), columnHandles);
    }

    private RecordSet getRecordSet(PodLogsTableFunctionSplit split, List<KubernetesColumnHandle> columnHandles)
    {
        return kubernetesLogs.getLogs(split, columnHandles);
    }
}
