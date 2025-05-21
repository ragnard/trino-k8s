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

import io.airlift.slice.Slice;
import io.trino.spi.connector.ConnectorAccessControl;
import io.trino.spi.connector.ConnectorSession;
import io.trino.spi.connector.ConnectorTransactionHandle;
import io.trino.spi.function.table.AbstractConnectorTableFunction;
import io.trino.spi.function.table.Argument;
import io.trino.spi.function.table.ReturnTypeSpecification;
import io.trino.spi.function.table.ScalarArgument;
import io.trino.spi.function.table.ScalarArgumentSpecification;
import io.trino.spi.function.table.TableFunctionAnalysis;
import io.trino.spi.type.VarcharType;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.github.ragnard.trino.k8s.logs.PodLogsTable.DESCRIPTOR;

public class PodLogsTableFunction
        extends AbstractConnectorTableFunction
{
    public static final ScalarArgumentSpecification NAMESPACE = ScalarArgumentSpecification.builder()
            .name("NAMESPACE")
            .type(VarcharType.VARCHAR)
            .build();

    public static final ScalarArgumentSpecification SELECTOR = ScalarArgumentSpecification.builder()
            .name("SELECTOR")
            .type(VarcharType.VARCHAR)
            .build();

    public static final ScalarArgumentSpecification CONTAINER = ScalarArgumentSpecification.builder()
            .name("CONTAINER")
            .type(VarcharType.VARCHAR)
            .defaultValue(null)
            .build();

    public PodLogsTableFunction()
    {
        super(
                "system",
                "logs",
                List.of(NAMESPACE, SELECTOR, CONTAINER),
                new ReturnTypeSpecification.DescribedTable(DESCRIPTOR));
    }

    @Override
    public TableFunctionAnalysis analyze(ConnectorSession session, ConnectorTransactionHandle transaction, Map<String, Argument> arguments, ConnectorAccessControl accessControl)
    {
        var namespace = getRequiredString(arguments.get(NAMESPACE.getName()));
        var selector = getRequiredString(arguments.get(SELECTOR.getName()));
        var container = getOptionalString(arguments.get(CONTAINER.getName()));

        return TableFunctionAnalysis.builder()
                .handle(new PodLogsTableFunctionHandle(namespace, selector, Optional.ofNullable(container)))
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
