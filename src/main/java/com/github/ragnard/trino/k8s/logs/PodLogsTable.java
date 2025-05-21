
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
package com.github.ragnard.trino.k8s.logs;

import com.github.ragnard.trino.k8s.KubernetesColumnHandle;
import io.airlift.slice.Slices;
import io.trino.spi.connector.ConnectorTableMetadata;
import io.trino.spi.connector.SchemaTableName;
import io.trino.spi.function.table.Descriptor;
import io.trino.spi.type.TimestampWithTimeZoneType;
import io.trino.spi.type.Type;
import io.trino.spi.type.VarcharType;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.github.ragnard.trino.k8s.client.KubernetesLogs.removeAnsiCodes;
import static com.github.ragnard.trino.k8s.KubernetesTypes.toTimestamp;

public class PodLogsTable
{
    public static final PodLogsTableColumn TIMESTAMP = new PodLogsTableColumn("timestamp", TimestampWithTimeZoneType.TIMESTAMP_TZ_NANOS, l -> toTimestamp(l.timestamp()));
    public static final PodLogsTableColumn NAMESPACE = new PodLogsTableColumn("namespace", VarcharType.VARCHAR, l -> Slices.utf8Slice(l.namespace()));
    public static final PodLogsTableColumn CONTAINER = new PodLogsTableColumn("container", VarcharType.VARCHAR, l -> Slices.utf8Slice(l.container()));
    public static final PodLogsTableColumn LOG = new PodLogsTableColumn("log", VarcharType.VARCHAR, l -> Slices.utf8Slice(removeAnsiCodes(l.log())));

    public static final List<PodLogsTableColumn> COLUMNS = List.of(TIMESTAMP, NAMESPACE, CONTAINER, LOG);

    private static final Map<String, PodLogsTableColumn> COLUMN_LOOKUP = COLUMNS.stream()
            .collect(Collectors.toMap(PodLogsTableColumn::name, c -> c));

    public static final ConnectorTableMetadata TABLE_METADATA = new ConnectorTableMetadata(
            new SchemaTableName("system", "logs"),
            COLUMNS.stream().map(PodLogsTableColumn::toColumnMetadata).toList());

    public static final List<String> COLUMN_NAMES = COLUMNS.stream().map(PodLogsTableColumn::name).toList();
    public static final List<Type> COLUMN_TYPES = COLUMNS.stream().map(PodLogsTableColumn::type).toList();

    public static final Descriptor DESCRIPTOR = Descriptor.descriptor(COLUMN_NAMES, COLUMN_TYPES);

    private PodLogsTable() {}

    public static PodLogsTableColumn lookup(KubernetesColumnHandle handle)
    {
        return COLUMN_LOOKUP.get(handle.name());
    }
}
