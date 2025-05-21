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
import com.github.ragnard.trino.k8s.KubernetesSplit;
import io.airlift.slice.SizeOf;

import java.util.OptionalInt;

public record PodLogsTableFunctionSplit(
        @JsonProperty String namespace,
        @JsonProperty String pod,
        @JsonProperty String container,
        @JsonProperty OptionalInt limit)
        implements KubernetesSplit
{
    public static final long INSTANCE_SIZE = SizeOf.instanceSize(PodLogsTableFunctionSplit.class);

    @Override
    public long getRetainedSizeInBytes()
    {
        return INSTANCE_SIZE;
    }

    public Integer limitOrNull()
    {
        return limit.isPresent() ? limit.getAsInt() : null;
    }
}
