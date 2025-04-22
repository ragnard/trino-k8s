package com.github.ragnard.trino.k8s.data;

import com.github.ragnard.trino.k8s.KubernetesTableHandle;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import io.kubernetes.client.Discovery;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.util.generic.dynamic.DynamicKubernetesApi;
import io.kubernetes.client.util.generic.dynamic.DynamicKubernetesObject;
import io.kubernetes.client.util.generic.options.ListOptions;
import io.trino.spi.connector.ConnectorTableHandle;
import io.trino.spi.connector.SchemaTableName;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.BinaryOperator;

public class KubernetesData
{
    private final ApiClient apiClient;

    private ImmutableMap<SchemaTableName, KubernetesResourceTable> tables;

    public final static String RESOURCES_SCHEMA = "resources";

    @Inject
    public KubernetesData(ApiClient apiClient)
    {
        this.apiClient = apiClient;
        this.tables = loadTables();
    }

    public List<SchemaTableName> listTables()
    {
        return this.tables.keySet().stream().toList();
    }

    public @Nullable ConnectorTableHandle getTableHandle(SchemaTableName tableName)
    {
        return Objects.requireNonNull(this.tables.get(tableName)).toTableHandle();
    }

    public KubernetesResourceTable lookupTable(KubernetesTableHandle tableHandle)
    {
        return this.tables.get(tableHandle.getSchemaTableName());
    }

    public ImmutableMap<SchemaTableName, KubernetesResourceTable> loadTables()

    {
        try {
            Discovery discovery = new Discovery(this.apiClient);

            var response = discovery.findAll();

            BinaryOperator<KubernetesResourceTable> merge = (KubernetesResourceTable t1, KubernetesResourceTable t2) -> {
                return t1;
            };

            return response.stream()
                    .map(KubernetesResourceTable::from)
                    .collect(ImmutableMap.toImmutableMap(KubernetesResourceTable::schemaTableName, v -> v, merge));
        }
        catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }

    public List<DynamicKubernetesObject> getObjects(KubernetesResourceTable table)
    {
        var resource = table.resource();

        var dynamicApi = new DynamicKubernetesApi(resource.getGroup(), resource.getPreferredVersion(), resource.getResourcePlural(), this.apiClient);

        var options = new ListOptions();

        var result = dynamicApi.list(options);

        return result.getObject().getItems()
                .stream()
                .toList();
    }
}
