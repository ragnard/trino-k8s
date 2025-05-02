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
package com.github.ragnard.trino.k8s.functions;

import io.airlift.slice.Slice;
import io.trino.spi.connector.ColumnMetadata;
import io.trino.spi.connector.ConnectorAccessControl;
import io.trino.spi.connector.ConnectorSession;
import io.trino.spi.connector.ConnectorTransactionHandle;
import io.trino.spi.function.table.AbstractConnectorTableFunction;
import io.trino.spi.function.table.Argument;
import io.trino.spi.function.table.Descriptor;
import io.trino.spi.function.table.ReturnTypeSpecification;
import io.trino.spi.function.table.ScalarArgument;
import io.trino.spi.function.table.ScalarArgumentSpecification;
import io.trino.spi.function.table.TableFunctionAnalysis;
import io.trino.spi.type.IntegerType;
import io.trino.spi.type.TimestampWithTimeZoneType;
import io.trino.spi.type.Type;
import io.trino.spi.type.VarcharType;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

public class PodLogsTableFunction
        extends AbstractConnectorTableFunction
{
    private static final ColumnMetadata POD = ColumnMetadata.builder()
            .setName("pod")
            .setType(VarcharType.VARCHAR)
            .build();

    private static final ColumnMetadata CONTAINER = ColumnMetadata.builder()
            .setName("container")
            .setType(VarcharType.VARCHAR)
            .build();

    private static final ColumnMetadata TIMESTAMP = ColumnMetadata.builder()
            .setName("timestamp")
            .setType(TimestampWithTimeZoneType.TIMESTAMP_TZ_NANOS)
            .build();

    private static final ColumnMetadata LOG = ColumnMetadata.builder()
            .setName("log")
            .setType(VarcharType.VARCHAR)
            .build();

    public static final List<ColumnMetadata> COLUMNS = List.of(
            POD,
            CONTAINER,
            TIMESTAMP,
            LOG);

    public static final List<String> COLUMN_NAMES = COLUMNS.stream().map(ColumnMetadata::getName).toList();
    public static final List<Type> COLUMN_TYPES = COLUMNS.stream().map(ColumnMetadata::getType).toList();

    private static final Descriptor DESCRIPTOR = Descriptor.descriptor(COLUMN_NAMES, COLUMN_TYPES);

    public PodLogsTableFunction()
    {
        super(
                "system",
                "logs",
                List.of(
                        ScalarArgumentSpecification.builder()
                                .name("NAMESPACE")
                                .type(VarcharType.VARCHAR)
                                .build(),
                        ScalarArgumentSpecification.builder()
                                .name("SELECTOR")
                                .type(VarcharType.VARCHAR)
                                .build(),
                        ScalarArgumentSpecification.builder()
                                .name("CONTAINER")
                                .type(VarcharType.VARCHAR)
                                .defaultValue(null)
                                .build(),
                        ScalarArgumentSpecification.builder()
                                .name("LIMIT")
                                .type(IntegerType.INTEGER)
                                .defaultValue(null)
                                .build()),
                new ReturnTypeSpecification.DescribedTable(DESCRIPTOR));
    }

    @Override
    public TableFunctionAnalysis analyze(ConnectorSession session, ConnectorTransactionHandle transaction, Map<String, Argument> arguments, ConnectorAccessControl accessControl)
    {
        var namespace = getRequiredString(arguments.get("NAMESPACE"));
        var selector = getRequiredString(arguments.get("SELECTOR"));
        var container = getOptionalString(arguments.get("CONTAINER"));

        var limitValue = ((ScalarArgument) arguments.get("LIMIT")).getValue();
        var limit = limitValue != null ? OptionalInt.of(((Long) limitValue).intValue()) : OptionalInt.empty();

        return TableFunctionAnalysis.builder()
                .handle(new PodLogsTableFunctionHandle(namespace, selector, Optional.ofNullable(container), limit))
                .build();
    }

    private static String getRequiredString(Argument argument)
    {
        var value = ((ScalarArgument) argument).getValue();
        if (!(value instanceof Slice)) {
            throw new RuntimeException("argument value not a slice");
        }
        return ((Slice) value).toStringUtf8();
    }

    private static String getOptionalString(Argument argument)
    {
        var value = ((ScalarArgument) argument).getValue();
        if (value == null) {
            return null;
        }
        if (!(value instanceof Slice)) {
            throw new RuntimeException("argument value not a slice");
        }
        return ((Slice) value).toStringUtf8();
    }
}
