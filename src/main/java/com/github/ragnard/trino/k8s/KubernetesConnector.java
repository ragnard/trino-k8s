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

import com.github.ragnard.trino.k8s.logs.PodLogsTableFunction;
import com.google.inject.Inject;
import io.trino.spi.connector.Connector;
import io.trino.spi.connector.ConnectorMetadata;
import io.trino.spi.connector.ConnectorRecordSetProvider;
import io.trino.spi.connector.ConnectorSession;
import io.trino.spi.connector.ConnectorSplitManager;
import io.trino.spi.connector.ConnectorTransactionHandle;
import io.trino.spi.function.FunctionProvider;
import io.trino.spi.function.table.ConnectorTableFunction;
import io.trino.spi.transaction.IsolationLevel;

import java.util.Optional;
import java.util.Set;

import static com.github.ragnard.trino.k8s.KubernetesTransactionHandle.INSTANCE;
import static java.util.Objects.requireNonNull;

public class KubernetesConnector
        implements Connector, FunctionProvider
{
    private final KubernetesMetadata metadata;
    private final KubernetesSplitManager splitManager;
    private final KubernetesRecordSetProvider recordSetProvider;

    @Inject
    public KubernetesConnector(
            KubernetesMetadata metadata,
            KubernetesSplitManager splitManager,
            KubernetesRecordSetProvider recordSetProvider)
    {
        this.metadata = requireNonNull(metadata, "metadata is null");
        this.splitManager = requireNonNull(splitManager, "splitManager is null");
        this.recordSetProvider = requireNonNull(recordSetProvider, "recordSetProvider is null");
    }

    @Override
    public ConnectorTransactionHandle beginTransaction(IsolationLevel isolationLevel, boolean readOnly, boolean autoCommit)
    {
        return INSTANCE;
    }

    @Override
    public ConnectorMetadata getMetadata(ConnectorSession session, ConnectorTransactionHandle transaction)
    {
        return metadata;
    }

    @Override
    public ConnectorSplitManager getSplitManager()
    {
        return splitManager;
    }

    @Override
    public ConnectorRecordSetProvider getRecordSetProvider()
    {
        return recordSetProvider;
    }

    @Override
    public Optional<FunctionProvider> getFunctionProvider()
    {
        return Optional.of(this);
    }

    @Override
    public Set<ConnectorTableFunction> getTableFunctions()
    {
        return Set.of(new PodLogsTableFunction());
    }
}
