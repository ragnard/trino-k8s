package com.github.ragnard.trino.k8s;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.trino.spi.connector.ColumnHandle;
import io.trino.spi.type.Type;

public record KubernetesColumnHandle(
        @JsonProperty("name") String name,
        @JsonProperty("type") Type type
) implements ColumnHandle
{
}
