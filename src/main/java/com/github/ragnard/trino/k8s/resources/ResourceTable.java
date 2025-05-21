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
package com.github.ragnard.trino.k8s.resources;

import com.github.ragnard.trino.k8s.KubernetesColumnHandle;
import com.github.ragnard.trino.k8s.KubernetesTableHandle;
import com.github.ragnard.trino.k8s.client.KubernetesResources;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.kubernetes.client.Discovery;
import io.trino.spi.connector.ColumnHandle;
import io.trino.spi.connector.ColumnMetadata;
import io.trino.spi.connector.ConnectorTableMetadata;
import io.trino.spi.connector.SchemaTableName;

import java.util.Map;
import java.util.stream.Stream;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.Objects.requireNonNull;

public record ResourceTable(
        Discovery.APIResource resource,
        SchemaTableName schemaTableName,
        ImmutableMap<String, ResourceTableColumn> columns)
{
    public static ResourceTable from(Discovery.APIResource resource)
    {
        return new ResourceTable(resource, createSchemaTableName(resource), createColumns(resource));
    }

    private static SchemaTableName createSchemaTableName(Discovery.APIResource resource)
    {
        if (isNullOrEmpty(resource.getGroup())) {
            return new SchemaTableName(KubernetesResources.RESOURCES_SCHEMA, resource.getResourcePlural());
        }
        else {
            return new SchemaTableName(KubernetesResources.RESOURCES_SCHEMA, resource.getGroup() + "." + resource.getResourcePlural());
        }
    }

    private static ImmutableMap<String, ResourceTableColumn> createColumns(Discovery.APIResource resource)
    {
        return Stream.of(ResourceTableColumns.KIND,
                        ResourceTableColumns.GROUP,
                        ResourceTableColumns.API_VERSION,
                        ResourceTableColumns.NAME,
                        ResourceTableColumns.NAMESPACE,
                        ResourceTableColumns.LABELS,
                        ResourceTableColumns.ANNOTATIONS,
                        ResourceTableColumns.CLUSTER_NAME,
                        ResourceTableColumns.CREATION_TIMESTAMP,
                        ResourceTableColumns.DELETION_GRACE_PERIOD_SECONDS,
                        ResourceTableColumns.DELETION_TIMESTAMP,
                        ResourceTableColumns.FINALIZERS,
                        ResourceTableColumns.RESOURCE_VERSION,
                        ResourceTableColumns.SELF_LINK,
                        ResourceTableColumns.UID,
                        ResourceTableColumns.METADATA,
                        ResourceTableColumns.RESOURCE)
                .collect(toImmutableMap(
                        ResourceTableColumn::name,
                        c -> c));
    }

    public KubernetesTableHandle toTableHandle()
    {
        return new ResourceTableHandle(schemaTableName());
    }

    public ConnectorTableMetadata getTableMetadata()
    {
        return new ConnectorTableMetadata(
                schemaTableName(),
                getColumnMetadata());
    }

    public ImmutableList<ResourceTableColumn> getColumns()
    {
        return columns.values().asList();
    }

    private ImmutableList<ColumnMetadata> getColumnMetadata()
    {
        return columns.values()
                .stream()
                .map(ResourceTableColumn::toColumnMetadata)
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

    public ResourceTableColumn lookupColumn(KubernetesColumnHandle columnHandle)
    {
        return this.columns.get(columnHandle.name());
    }
}
