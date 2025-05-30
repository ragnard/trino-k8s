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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.ragnard.trino.k8s.KubernetesTableHandle;

import java.util.OptionalInt;

public record PodLogsTableHandle(
        @JsonProperty("functionHandle") PodLogsTableFunctionHandle functionHandle,
        @JsonProperty("limit") OptionalInt limit)
        implements KubernetesTableHandle
{
    public PodLogsTableHandle(PodLogsTableFunctionHandle functionHandle)
    {
        this(functionHandle, OptionalInt.empty());
    }

    @Override
    public KubernetesTableHandle withLimit(int limit)
    {
        return new PodLogsTableHandle(functionHandle, OptionalInt.of(limit));
    }
}
