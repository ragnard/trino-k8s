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

import com.github.ragnard.trino.k8s.data.KubernetesData;
import com.github.ragnard.trino.k8s.data.KubernetesResourceTableColumn;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import io.kubernetes.client.util.generic.dynamic.DynamicKubernetesObject;
import io.trino.spi.connector.ColumnHandle;
import io.trino.spi.connector.ColumnMetadata;
import io.trino.spi.connector.ConnectorRecordSetProvider;
import io.trino.spi.connector.ConnectorSession;
import io.trino.spi.connector.ConnectorSplit;
import io.trino.spi.connector.ConnectorTableHandle;
import io.trino.spi.connector.ConnectorTransactionHandle;
import io.trino.spi.connector.InMemoryRecordSet;
import io.trino.spi.connector.RecordSet;

import java.util.List;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.Objects.requireNonNull;

public class KubernetesRecordSetProvider
        implements ConnectorRecordSetProvider
{
    private final KubernetesData kubernetesData;

    @Inject
    public KubernetesRecordSetProvider(KubernetesData kubernetesData)
    {
        this.kubernetesData = kubernetesData;
    }

    @Override
    public RecordSet getRecordSet(
            ConnectorTransactionHandle connectorTransactionHandle,
            ConnectorSession connectorSession,
            ConnectorSplit connectorSplit,
            ConnectorTableHandle tableHandle,
            List<? extends ColumnHandle> columnHandles)
    {
        var table = requireNonNull(this.kubernetesData.lookupTable((KubernetesTableHandle) tableHandle));

        var columns = columnHandles.stream()
                .map(c -> ((KubernetesColumnHandle) c))
                .map(table::lookupColumn)
                .collect(toImmutableList());

        var columnMetadata = columns.stream()
                .map(KubernetesResourceTableColumn::toColumnMetadata)
                .toList();

        var objects = this.kubernetesData.getObjects(table);

        var records = ImmutableList.<List<?>>builder();
        //var builder = InMemoryRecordSet.builder(columnMetadata);

        for (DynamicKubernetesObject object : objects) {
            var values = columns
                    .stream()
                    .map(c -> c.getValue(table, object))
                    .toList();

            //builder.addRow(values);
            records.add(values);
        }

        var types = columnMetadata.stream().map(ColumnMetadata::getType).toList();
        return new InMemoryRecordSet(types, records.build());

        //return builder.build();
    }
}
