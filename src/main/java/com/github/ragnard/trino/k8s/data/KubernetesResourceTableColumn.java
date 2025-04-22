package com.github.ragnard.trino.k8s.data;

import com.github.ragnard.trino.k8s.KubernetesColumnHandle;
import io.kubernetes.client.util.generic.dynamic.DynamicKubernetesObject;
import io.trino.spi.connector.ColumnHandle;
import io.trino.spi.connector.ColumnMetadata;
import io.trino.spi.type.Type;
import io.trino.spi.type.VarcharType;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

public record KubernetesResourceTableColumn(String name, Type type, ColumnValueFunction value)
{
    public interface ColumnValueFunction extends BiFunction<KubernetesResourceTable, DynamicKubernetesObject, Object> {}

    public KubernetesColumnHandle toColumnHandle()
    {
        return new KubernetesColumnHandle(name, type);
    }

    public ColumnMetadata toColumnMetadata()
    {
        return ColumnMetadata.builder()
                .setName(name)
                .setType(type).build();
    }

    public Object getValue(KubernetesResourceTable table, DynamicKubernetesObject object)
    {
        return value.apply(table, object);
    }
}
