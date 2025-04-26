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

import java.util.List;
import java.util.function.BinaryOperator;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.Objects.requireNonNull;

public class KubernetesData
{
    private final ApiClient apiClient;

    private final ImmutableMap<SchemaTableName, KubernetesResourceTable> tables;

    public static final String RESOURCES_SCHEMA = "resources";

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

    public ConnectorTableHandle getTableHandle(SchemaTableName tableName)
    {
        return requireNonNull(this.tables.get(tableName)).toTableHandle();
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
                    .collect(toImmutableMap(KubernetesResourceTable::schemaTableName, v -> v, merge));
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
