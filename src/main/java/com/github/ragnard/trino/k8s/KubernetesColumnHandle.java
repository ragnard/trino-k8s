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

import com.fasterxml.jackson.annotation.JsonProperty;
import io.airlift.slice.SizeOf;
import io.trino.spi.connector.ColumnHandle;
import io.trino.spi.connector.ColumnMetadata;
import io.trino.spi.type.Type;

public record KubernetesColumnHandle(
        @JsonProperty("name") String name,
        @JsonProperty("type") Type type
)
        implements ColumnHandle
{
    private static final int INSTANCE_SIZE = SizeOf.instanceSize(KubernetesTableHandle.class);

    public long getRetainedSizeInBytes()
    {
        return INSTANCE_SIZE;
    }

    public ColumnMetadata toColumnMetadata()
    {
        return new ColumnMetadata(name, type);
    }
}
