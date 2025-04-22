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
import io.trino.spi.connector.SchemaTableName;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class KubernetesMetadata
        implements ConnectorMetadata
{
    private final KubernetesData kubernetesData;

    @Inject
    public KubernetesMetadata(KubernetesData kubernetesData/*KubernetesConfig config*/)
    {
        this.kubernetesData = kubernetesData;
    }

    @Override
    public List<String> listSchemaNames(ConnectorSession connectorSession)
    {
        return List.of(KubernetesData.RESOURCES_SCHEMA);
    }

    @Override
    public List<SchemaTableName> listTables(ConnectorSession session, Optional<String> schemaName)
    {
        var tables = this.kubernetesData.listTables();

        return schemaName
                .map(s -> tables.stream()
                        .filter(n -> n.getSchemaName().equals(s))
                        .toList())
                .orElse(tables);
    }

    @Override
    public @Nullable ConnectorTableHandle getTableHandle(ConnectorSession session, SchemaTableName tableName, Optional<ConnectorTableVersion> startVersion, Optional<ConnectorTableVersion> endVersion)
    {
        if (startVersion.isPresent() || endVersion.isPresent()) {
            throw new TrinoException(StandardErrorCode.NOT_SUPPORTED, "This connector does not support versioned tables");
        }
        return this.kubernetesData.getTableHandle(tableName);
    }

    @Override
    public ConnectorTableMetadata getTableMetadata(ConnectorSession session, ConnectorTableHandle tableHandle)
    {
        return this.kubernetesData.lookupTable((KubernetesTableHandle) tableHandle).getTableMetadata();
    }

    @Override
    public Map<String, ColumnHandle> getColumnHandles(ConnectorSession session, ConnectorTableHandle tableHandle)
    {
        return this.kubernetesData.lookupTable((KubernetesTableHandle) tableHandle).getColumnHandles();
    }

    @Override
    public ColumnMetadata getColumnMetadata(ConnectorSession session, ConnectorTableHandle tableHandle, ColumnHandle columnHandle)
    {
        return this.kubernetesData.lookupTable((KubernetesTableHandle) tableHandle).getColumnMetadata((KubernetesColumnHandle) columnHandle);
    }

    @Override
    public Optional<ConstraintApplicationResult<ConnectorTableHandle>> applyFilter(ConnectorSession session, ConnectorTableHandle handle, Constraint constraint)
    {
        return ConnectorMetadata.super.applyFilter(session, handle, constraint);
    }


    /*@Override
    public Iterator<TableColumnsMetadata> streamTableColumns(ConnectorSession session, SchemaTablePrefix prefix)
    {
        return spec.getTables().entrySet().stream()
                .map(entry -> TableColumnsMetadata.forTable(
                        new SchemaTableName(prefix.getSchema().orElse(""), entry.getKey()),
                        entry.getValue().stream().map(OpenApiColumn::getMetadata).toList()))
                .iterator();
    }

    /*@Override
    public Optional<ConstraintApplicationResult<ConnectorTableHandle>> applyFilter(
            ConnectorSession session,
            ConnectorTableHandle table,
            Constraint constraint)
    {
        OpenApiTableHandle openApiTable = (OpenApiTableHandle) table;
        return openApiTable.applyFilter(constraint, getColumns(table), domainExpansionLimit);
    }*/
}
