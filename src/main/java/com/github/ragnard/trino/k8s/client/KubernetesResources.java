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
package com.github.ragnard.trino.k8s.client;

import com.github.ragnard.trino.k8s.KubernetesColumnHandle;
import com.github.ragnard.trino.k8s.resources.ResourceTableHandle;
import com.github.ragnard.trino.k8s.KubernetesTableHandle;
import com.github.ragnard.trino.k8s.resources.ResourceTable;
import com.github.ragnard.trino.k8s.resources.ResourceTableColumn;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import io.airlift.slice.Slice;
import io.kubernetes.client.Discovery;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.util.generic.dynamic.DynamicKubernetesApi;
import io.kubernetes.client.util.generic.dynamic.DynamicKubernetesObject;
import io.kubernetes.client.util.generic.options.ListOptions;
import io.trino.spi.TrinoException;
import io.trino.spi.connector.ColumnHandle;
import io.trino.spi.connector.ColumnMetadata;
import io.trino.spi.connector.InMemoryRecordSet;
import io.trino.spi.connector.RecordSet;
import io.trino.spi.connector.SchemaTableName;
import io.trino.spi.predicate.TupleDomain;

import java.util.List;
import java.util.Optional;

import static com.github.ragnard.trino.k8s.resources.ResourceTableColumns.NAMESPACE;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static io.trino.spi.StandardErrorCode.TABLE_NOT_FOUND;

public class KubernetesResources
{
    private final ApiClient apiClient;

    private final ImmutableMap<SchemaTableName, ResourceTable> tables;

    public static final String RESOURCES_SCHEMA = "resources";

    @Inject
    public KubernetesResources(ApiClient apiClient)
    {
        this.apiClient = apiClient;
        this.tables = loadTables();
    }

    public List<SchemaTableName> listTables()
    {
        return this.tables.keySet().stream().toList();
    }

    public Optional<ResourceTable> lookupTable(KubernetesTableHandle tableHandle)
    {
        return tableHandle.resourceTableHandle().map(h -> this.tables.get(h.schemaTableName()));
    }

    public Optional<ResourceTable> lookupTable(SchemaTableName schemaTableName)
    {
        return Optional.ofNullable(this.tables.get(schemaTableName));
    }

    public ResourceTable lookupTableOrThrow(ResourceTableHandle tableHandle)
    {
        return lookupTableOrThrow(tableHandle.schemaTableName());
    }

    public ResourceTable lookupTableOrThrow(SchemaTableName schemaTableName)
    {
        return this.lookupTable(schemaTableName)
                .orElseThrow(() -> new TrinoException(TABLE_NOT_FOUND, "Table not found: %s".formatted(schemaTableName)));
    }

    public ImmutableMap<SchemaTableName, ResourceTable> loadTables()

    {
        try {
            Discovery discovery = new Discovery(this.apiClient);

            var response = discovery.findAll();

            return response.stream()
                    .map(ResourceTable::from)
                    .collect(toImmutableMap(ResourceTable::schemaTableName, v -> v));
        }
        catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }

    public RecordSet execute(ResourceTableHandle handle, List<KubernetesColumnHandle> columnHandles)
    {
        var table = this.lookupTableOrThrow(handle);

        var columns = columnHandles.stream()
                .map(table::lookupColumn)
                .collect(toImmutableList());

        var columnMetadata = columns.stream()
                .map(ResourceTableColumn::toColumnMetadata)
                .toList();

        var resource = table.resource();

        var dynamicApi = new DynamicKubernetesApi(resource.getGroup(), resource.getPreferredVersion(), resource.getResourcePlural(), this.apiClient);

        var options = new ListOptions();
        handle.limit().ifPresent(options::setLimit);

        var namespace = getNamespace(handle.constraint());

        var result = namespace
                .map(ns -> dynamicApi.list(ns, options))
                .orElseGet(() -> dynamicApi.list(options));

        var records = ImmutableList.<List<?>>builder();
        //var builder = InMemoryRecordSet.builder(columnMetadata);

        for (DynamicKubernetesObject object : result.getObject().getItems()) {
            var values = columns
                    .stream()
                    .map(c -> c.getValue(table, object))
                    .toList();

            //builder.addRow(values);
            records.add(values);
        }

        var types = columnMetadata.stream().map(ColumnMetadata::getType).toList();
        return new InMemoryRecordSet(types, records.build());
    }

    private Optional<String> getNamespace(TupleDomain<ColumnHandle> constraint)
    {
        var namespaceDomain = constraint.getDomain(NAMESPACE.toColumnHandle(), NAMESPACE.type());
        if (namespaceDomain == null) {
            return Optional.empty();
        }

        if (namespaceDomain.isSingleValue()) {
            var value = namespaceDomain.getSingleValue();
            return switch (value) {
                case Slice slice -> Optional.of(slice.toStringUtf8());
                default -> throw new IllegalStateException("Unexpected type for namespace filter: " + value.getClass());
            };
        }

        return Optional.empty();
    }
}
