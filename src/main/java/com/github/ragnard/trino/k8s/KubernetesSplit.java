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
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.github.ragnard.trino.k8s.logs.PodLogsTableFunctionSplit;
import com.github.ragnard.trino.k8s.resources.ResourceTableSplit;
import io.trino.spi.HostAddress;
import io.trino.spi.connector.ConnectorSplit;

import java.util.List;

@JsonSubTypes({
        @JsonSubTypes.Type(value = ResourceTableSplit.class),
        @JsonSubTypes.Type(value = PodLogsTableFunctionSplit.class),
})
public interface KubernetesSplit
        extends ConnectorSplit
{
    @Override
    @JsonProperty("addresses")
    default List<HostAddress> getAddresses()
    {
        return List.of();
    }

    @Override
    default long getRetainedSizeInBytes()
    {
        return 0;
    }
}
