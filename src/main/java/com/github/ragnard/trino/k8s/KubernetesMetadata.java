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
import com.github.ragnard.trino.k8s.logs.PodLogsTable;
import com.github.ragnard.trino.k8s.logs.PodLogsTableColumn;
import com.github.ragnard.trino.k8s.logs.PodLogsTableFunctionHandle;
import com.github.ragnard.trino.k8s.logs.PodLogsTableHandle;
import com.github.ragnard.trino.k8s.resources.ResourceTableHandle;
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

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class KubernetesMetadata
        implements ConnectorMetadata
{
    private final KubernetesResources kubernetesResources;

    @Inject
    public KubernetesMetadata(KubernetesResources kubernetesResources/*KubernetesConfig config*/)
    {
        this.kubernetesResources = kubernetesResources;
    }

    @Override
    public List<String> listSchemaNames(ConnectorSession connectorSession)
    {
        return List.of(KubernetesResources.RESOURCES_SCHEMA);
    }

    @Override
    public List<SchemaTableName> listTables(ConnectorSession session, Optional<String> schemaName)
    {
        var tables = this.kubernetesResources.listTables();

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
        return this.kubernetesResources.lookupTableOrThrow(tableName).toTableHandle();
    }

    @Override
    public ConnectorTableMetadata getTableMetadata(ConnectorSession session, ConnectorTableHandle tableHandle)
    {
        return switch ((KubernetesTableHandle) tableHandle) {
            case ResourceTableHandle h -> this.kubernetesResources.lookupTableOrThrow(h).getTableMetadata();
            case PodLogsTableHandle _ -> PodLogsTable.TABLE_METADATA;
            default -> throw new IllegalStateException("Unexpected value: " + tableHandle);
        };
    }

    @Override
    public Map<String, ColumnHandle> getColumnHandles(ConnectorSession session, ConnectorTableHandle tableHandle)
    {
        return switch ((KubernetesTableHandle) tableHandle) {
            case ResourceTableHandle h -> this.kubernetesResources.lookupTableOrThrow(h).getColumnHandles();
            default -> throw new IllegalStateException("Unexpected value: " + tableHandle);
        };
    }

    @Override
    public ColumnMetadata getColumnMetadata(ConnectorSession session, ConnectorTableHandle tableHandle, ColumnHandle columnHandle)
    {
        return switch ((KubernetesTableHandle) tableHandle) {
            case ResourceTableHandle h -> this.kubernetesResources.lookupTableOrThrow(h).getColumnMetadata((KubernetesColumnHandle) columnHandle);
            case PodLogsTableHandle _ -> ((KubernetesColumnHandle) columnHandle).toColumnMetadata();
            default -> throw new IllegalStateException("Unexpected value: " + tableHandle);
        };
    }

    @Override
    public Optional<TableFunctionApplicationResult<ConnectorTableHandle>> applyTableFunction(ConnectorSession session, ConnectorTableFunctionHandle handle)
    {
        return switch (handle) {
            case PodLogsTableFunctionHandle h -> {
                var columns = PodLogsTable.COLUMNS.stream().map(PodLogsTableColumn::toColumnHandle).toList();
                yield Optional.of(new TableFunctionApplicationResult<>(new PodLogsTableHandle(h), columns));
            }
            default -> throw new IllegalStateException("Unexpected value: " + handle);
        };
    }

    @Override
    public Optional<ConstraintApplicationResult<ConnectorTableHandle>> applyFilter(ConnectorSession session, ConnectorTableHandle tableHandle, Constraint constraint)
    {
        KubernetesTableHandle handle = (KubernetesTableHandle) tableHandle;

        return handle.applyFilter(constraint);
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
