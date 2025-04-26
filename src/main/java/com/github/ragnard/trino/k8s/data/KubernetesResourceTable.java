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
package com.github.ragnard.trino.k8s.data;

import com.github.ragnard.trino.k8s.KubernetesColumnHandle;
import com.github.ragnard.trino.k8s.KubernetesTableHandle;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.kubernetes.client.Discovery;
import io.trino.spi.connector.ColumnHandle;
import io.trino.spi.connector.ColumnMetadata;
import io.trino.spi.connector.ConnectorTableMetadata;
import io.trino.spi.connector.SchemaTableName;

import java.util.Map;
import java.util.stream.Stream;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.Objects.requireNonNull;

public record KubernetesResourceTable(Discovery.APIResource resource, ImmutableMap<String, KubernetesResourceTableColumn> columns)
{
    public static KubernetesResourceTable from(Discovery.APIResource resource)
    {
        return new KubernetesResourceTable(resource, createColumns(resource));
    }

    private static ImmutableMap<String, KubernetesResourceTableColumn> createColumns(Discovery.APIResource resource)
    {
        return Stream.of(KubernetesResourceTableColumns.KIND,
                        KubernetesResourceTableColumns.GROUP,
                        KubernetesResourceTableColumns.API_VERSION,
                        KubernetesResourceTableColumns.NAME,
                        KubernetesResourceTableColumns.NAMESPACE,
                        KubernetesResourceTableColumns.LABELS,
                        KubernetesResourceTableColumns.ANNOTATIONS,
                        KubernetesResourceTableColumns.CLUSTER_NAME,
                        KubernetesResourceTableColumns.CREATION_TIMESTAMP,
                        KubernetesResourceTableColumns.DELETION_GRACE_PERIOD_SECONDS,
                        KubernetesResourceTableColumns.DELETION_TIMESTAMP,
                        KubernetesResourceTableColumns.FINALIZERS,
                        KubernetesResourceTableColumns.RESOURCE_VERSION,
                        KubernetesResourceTableColumns.SELF_LINK,
                        KubernetesResourceTableColumns.UID,
                        KubernetesResourceTableColumns.METADATA,
                        KubernetesResourceTableColumns.RESOURCE)
                .collect(toImmutableMap(
                        KubernetesResourceTableColumn::name,
                        c -> c));
    }

    public SchemaTableName schemaTableName()
    {
        return new SchemaTableName(KubernetesData.RESOURCES_SCHEMA, resource.getResourcePlural());
    }

    public KubernetesTableHandle toTableHandle()
    {
        return new KubernetesTableHandle(schemaTableName(), null);
    }

    public ConnectorTableMetadata getTableMetadata()
    {
        return new ConnectorTableMetadata(
                schemaTableName(),
                getColumnMetadata());
    }

    public ImmutableList<KubernetesResourceTableColumn> getColumns()
    {
        return columns.values().asList();
    }

    private ImmutableList<ColumnMetadata> getColumnMetadata()
    {
        return columns.values()
                .stream()
                .map(KubernetesResourceTableColumn::toColumnMetadata)
                .collect(toImmutableList());
    }

    public Map<String, ColumnHandle> getColumnHandles()
    {
        return columns.entrySet()
                .stream()
                .collect(toImmutableMap(
                        Map.Entry::getKey,
                        c -> c.getValue().toColumnHandle()));
    }

    public ColumnMetadata getColumnMetadata(KubernetesColumnHandle columnHandle)
    {
        return requireNonNull(columns.get(columnHandle.name())).toColumnMetadata();
    }

    public KubernetesResourceTableColumn lookupColumn(KubernetesColumnHandle columnHandle)
    {
        return this.columns.get(columnHandle.name());
    }
}
