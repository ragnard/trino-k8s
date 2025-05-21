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
import com.github.ragnard.trino.k8s.client.KubernetesLogs;
import io.trino.spi.connector.ColumnHandle;
import io.trino.spi.connector.ColumnMetadata;
import io.trino.spi.type.Type;

import java.util.function.Function;

public record PodLogsTableColumn(String name, Type type, ColumnValueFunction value)
{
    public interface ColumnValueFunction
            extends Function<KubernetesLogs.PodLogLine, Object> {}

    public ColumnHandle toColumnHandle()
    {
        return new KubernetesColumnHandle(name, type);
    }

    public ColumnMetadata toColumnMetadata()
    {
        return ColumnMetadata.builder()
                .setName(name)
                .setType(type).build();
    }

    public Object getValue(KubernetesLogs.PodLogLine line)
    {
        return value.apply(line);
    }
}
