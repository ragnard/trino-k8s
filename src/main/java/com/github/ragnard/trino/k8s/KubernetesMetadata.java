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

import com.github.ragnard.trino.k8s.client.KubernetesClient;
import com.github.ragnard.trino.k8s.tables.KubernetesResourceTable;
import com.google.inject.Inject;
import io.trino.spi.StandardErrorCode;
import io.trino.spi.TrinoException;
import io.trino.spi.connector.ColumnHandle;
import io.trino.spi.connector.ColumnMetadata;
import io.trino.spi.connector.ConnectorMetadata;
import io.trino.spi.connector.ConnectorSession;
import io.trino.spi.connector.ConnectorTableHandle;
import io.trino.spi.connector.ConnectorTableMetadata;
import io.trino.spi.connector.ConnectorTableVersion;
import io.trino.spi.connector.Constraint;
import io.trino.spi.connector.ConstraintApplicationResult;
import io.trino.spi.connector.LimitApplicationResult;
import io.trino.spi.connector.SchemaTableName;
import io.trino.spi.connector.TableFunctionApplicationResult;
import io.trino.spi.function.table.ConnectorTableFunctionHandle;
import io.trino.spi.predicate.Domain;
import io.trino.spi.predicate.TupleDomain;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.github.ragnard.trino.k8s.tables.KubernetesResourceTableColumns.NAMESPACE;
import static io.trino.spi.StandardErrorCode.TABLE_NOT_FOUND;

public class KubernetesMetadata
        implements ConnectorMetadata
{
    private final KubernetesClient kubernetesClient;

    @Inject
    public KubernetesMetadata(KubernetesClient kubernetesClient/*KubernetesConfig config*/)
    {
        this.kubernetesClient = kubernetesClient;
    }

    @Override
    public List<String> listSchemaNames(ConnectorSession connectorSession)
    {
        return List.of(KubernetesClient.RESOURCES_SCHEMA);
    }

    @Override
    public List<SchemaTableName> listTables(ConnectorSession session, Optional<String> schemaName)
    {
        var tables = this.kubernetesClient.listTables();

        return schemaName
                .map(s -> tables.stream()
                        .filter(n -> n.getSchemaName().equals(s))
                        .toList())
                .orElse(tables);
    }

    @Override
    public ConnectorTableHandle getTableHandle(ConnectorSession session, SchemaTableName tableName, Optional<ConnectorTableVersion> startVersion, Optional<ConnectorTableVersion> endVersion)
    {
        if (startVersion.isPresent() || endVersion.isPresent()) {
            throw new TrinoException(StandardErrorCode.NOT_SUPPORTED, "This connector does not support versioned tables");
        }
        return this.kubernetesClient.lookupTableOrThrow(tableName).toTableHandle();
    }

    @Override
    public ConnectorTableMetadata getTableMetadata(ConnectorSession session, ConnectorTableHandle tableHandle)
    {
        return this.kubernetesClient.lookupTableOrThrow((KubernetesTableHandle) tableHandle)
                .getTableMetadata();
    }

    @Override
    public Map<String, ColumnHandle> getColumnHandles(ConnectorSession session, ConnectorTableHandle tableHandle)
    {
        return this.kubernetesClient.lookupTableOrThrow((KubernetesTableHandle) tableHandle)
                .getColumnHandles();
    }

    @Override
    public ColumnMetadata getColumnMetadata(ConnectorSession session, ConnectorTableHandle tableHandle, ColumnHandle columnHandle)
    {
        return this.kubernetesClient.lookupTableOrThrow((KubernetesTableHandle) tableHandle)
                .getColumnMetadata((KubernetesColumnHandle) columnHandle);
    }

    @Override
    public Optional<TableFunctionApplicationResult<ConnectorTableHandle>> applyTableFunction(ConnectorSession session, ConnectorTableFunctionHandle handle)
    {
        return ConnectorMetadata.super.applyTableFunction(session, handle);
    }

    @Override
    public Optional<ConstraintApplicationResult<ConnectorTableHandle>> applyFilter(ConnectorSession session, ConnectorTableHandle tableHandle, Constraint constraint)
    {
        KubernetesTableHandle handle = (KubernetesTableHandle) tableHandle;

        TupleDomain<ColumnHandle> oldDomain = handle.constraint();
        TupleDomain<ColumnHandle> newDomain = oldDomain.intersect(constraint.getSummary());
        TupleDomain<ColumnHandle> remainingFilter;
        if (newDomain.isNone()) {
            remainingFilter = TupleDomain.all();
        }
        else {
            Map<ColumnHandle, Domain> domains = newDomain.getDomains().orElseThrow();

            Map<ColumnHandle, Domain> supported = new HashMap<>();
            Map<ColumnHandle, Domain> unsupported = new HashMap<>();

            for (Map.Entry<ColumnHandle, Domain> entry : domains.entrySet()) {
                var columnHandle = (KubernetesColumnHandle) entry.getKey();
                var domain = entry.getValue();
                var columnType = columnHandle.type();

                if (columnHandle.name().equals(NAMESPACE.name()) && columnType.equals(NAMESPACE.type())) {
                    supported.put(columnHandle, domain);
                }
                else {
                    unsupported.put(columnHandle, domain);
                }
            }
            newDomain = TupleDomain.withColumnDomains(supported);
            remainingFilter = TupleDomain.withColumnDomains(unsupported);
        }

        if (oldDomain.equals(newDomain)) {
            return Optional.empty();
        }

        handle = handle.withConstraint(newDomain);

        return Optional.of(new ConstraintApplicationResult<>(handle, remainingFilter, constraint.getExpression(), false));
    }

    @Override
    public Optional<LimitApplicationResult<ConnectorTableHandle>> applyLimit(ConnectorSession session, ConnectorTableHandle handle, long limit)
    {
        KubernetesTableHandle tableHandle = (KubernetesTableHandle) handle;

        if (limit > Integer.MAX_VALUE) {
            limit = Integer.MAX_VALUE;
        }

        if (tableHandle.limit().isPresent() && tableHandle.limit().getAsInt() <= limit) {
            return Optional.empty();
        }

        return Optional.of(new LimitApplicationResult<>(tableHandle.withLimit((int) limit), true, false));
    }
}
