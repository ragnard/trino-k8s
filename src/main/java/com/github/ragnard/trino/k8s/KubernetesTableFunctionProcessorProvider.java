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
import com.github.ragnard.trino.k8s.functions.PodLogsTableFunctionSplit;
import com.github.ragnard.trino.k8s.functions.PodLogsTableFunctionSplitProcessor;
import com.google.inject.Inject;
import io.trino.spi.connector.ConnectorSession;
import io.trino.spi.connector.ConnectorSplit;
import io.trino.spi.function.table.ConnectorTableFunctionHandle;
import io.trino.spi.function.table.TableFunctionProcessorProvider;
import io.trino.spi.function.table.TableFunctionSplitProcessor;

public class KubernetesTableFunctionProcessorProvider
        implements TableFunctionProcessorProvider
{
    private final KubernetesLogs kubernetesLogs;

    @Inject
    public KubernetesTableFunctionProcessorProvider(KubernetesLogs kubernetesLogs)
    {
        this.kubernetesLogs = kubernetesLogs;
    }

    @Override
    public TableFunctionSplitProcessor getSplitProcessor(ConnectorSession session, ConnectorTableFunctionHandle handle, ConnectorSplit split)
    {
        return new PodLogsTableFunctionSplitProcessor(this.kubernetesLogs, (PodLogsTableFunctionSplit) split);
    }
}
