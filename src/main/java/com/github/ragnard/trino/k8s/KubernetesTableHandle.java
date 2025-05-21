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

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.github.ragnard.trino.k8s.logs.PodLogsTableFunctionHandle;
import com.github.ragnard.trino.k8s.resources.ResourceTableHandle;
import io.trino.spi.connector.ConnectorTableHandle;
import io.trino.spi.connector.Constraint;
import io.trino.spi.connector.ConstraintApplicationResult;

import java.util.Optional;
import java.util.OptionalInt;

@JsonSubTypes({
        @JsonSubTypes.Type(value = ResourceTableHandle.class, name = "ResourceTable"),
        @JsonSubTypes.Type(value = PodLogsTableFunctionHandle.class, name = "PodLogs"),
})
public interface KubernetesTableHandle
        extends ConnectorTableHandle
{
    OptionalInt limit();

    KubernetesTableHandle withLimit(int limit);

    default long getRetainedSizeInBytes()
    {
        return 0;
    }

    default Optional<ConstraintApplicationResult<ConnectorTableHandle>> applyFilter(Constraint constraint)
    {
        return Optional.empty();
    }

    default Optional<ResourceTableHandle> resourceTableHandle()
    {
        return switch (this) {
            case ResourceTableHandle h -> Optional.of(h);
            default -> Optional.empty();
        };
    }
}
