package com.github.ragnard.trino.k8s.data;

import com.google.gson.JsonElement;
import io.kubernetes.client.Discovery;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.trino.spi.type.BigintType;
import io.trino.spi.type.TimestampWithTimeZoneType;
import io.trino.spi.type.VarcharType;

import java.util.Optional;
import java.util.function.Function;

import static com.github.ragnard.trino.k8s.data.KubernetesTypes.STRING_ARRAY;
import static com.github.ragnard.trino.k8s.data.KubernetesTypes.STRING_MAP;

public class KubernetesResourceTableColumns
{
    public static final KubernetesResourceTableColumn KIND = new KubernetesResourceTableColumn("kind", VarcharType.VARCHAR, resourceMethod(Discovery.APIResource::getKind));
    public static final KubernetesResourceTableColumn GROUP = new KubernetesResourceTableColumn("group", VarcharType.VARCHAR, resourceMethod(Discovery.APIResource::getGroup));
    public static final KubernetesResourceTableColumn API_VERSION = new KubernetesResourceTableColumn("apiVersion", VarcharType.VARCHAR, resourceMethod(Discovery.APIResource::getPreferredVersion));

    // V1ObjectMeta
    public static final KubernetesResourceTableColumn NAME = new KubernetesResourceTableColumn("name", VarcharType.VARCHAR, metadataMethod(V1ObjectMeta::getName));
    public static final KubernetesResourceTableColumn NAMESPACE = new KubernetesResourceTableColumn("namespace", VarcharType.VARCHAR, metadataMethod(V1ObjectMeta::getNamespace));
    public static final KubernetesResourceTableColumn LABELS = new KubernetesResourceTableColumn("labels", STRING_MAP, metadataMethod(V1ObjectMeta::getLabels, KubernetesTypes::stringMap));
    public static final KubernetesResourceTableColumn ANNOTATIONS = new KubernetesResourceTableColumn("annotations", STRING_MAP, metadataMethod(V1ObjectMeta::getAnnotations, KubernetesTypes::stringMap));
    public static final KubernetesResourceTableColumn CLUSTER_NAME = new KubernetesResourceTableColumn("clusterName", VarcharType.VARCHAR, metadataMethod(V1ObjectMeta::getClusterName));
    public static final KubernetesResourceTableColumn CREATION_TIMESTAMP = new KubernetesResourceTableColumn("creationTimestamp", TimestampWithTimeZoneType.TIMESTAMP_TZ_MICROS, metadataMethod(V1ObjectMeta::getCreationTimestamp, KubernetesTypes::toTimestamp));
    public static final KubernetesResourceTableColumn DELETION_GRACE_PERIOD_SECONDS = new KubernetesResourceTableColumn("deletionGracePeriodSeconds", BigintType.BIGINT, metadataMethod(V1ObjectMeta::getDeletionGracePeriodSeconds));
    public static final KubernetesResourceTableColumn DELETION_TIMESTAMP = new KubernetesResourceTableColumn("deletionTimestamp", TimestampWithTimeZoneType.TIMESTAMP_TZ_MICROS, metadataMethod(V1ObjectMeta::getDeletionTimestamp, KubernetesTypes::toTimestamp));
    public static final KubernetesResourceTableColumn FINALIZERS = new KubernetesResourceTableColumn("finalizers", STRING_ARRAY, metadataMethod(V1ObjectMeta::getFinalizers, KubernetesTypes::stringArray));
    public static final KubernetesResourceTableColumn RESOURCE_VERSION = new KubernetesResourceTableColumn("resourceVersion", VarcharType.VARCHAR, metadataMethod(V1ObjectMeta::getResourceVersion));
    public static final KubernetesResourceTableColumn SELF_LINK = new KubernetesResourceTableColumn("selfLink", VarcharType.VARCHAR, metadataMethod(V1ObjectMeta::getSelfLink));
    public static final KubernetesResourceTableColumn UID = new KubernetesResourceTableColumn("uid", VarcharType.VARCHAR, metadataMethod(V1ObjectMeta::getUid));


    //
    public static final KubernetesResourceTableColumn METADATA = new KubernetesResourceTableColumn("metadata", VarcharType.VARCHAR, metadata());
    public static final KubernetesResourceTableColumn RESOURCE = new KubernetesResourceTableColumn("resource", VarcharType.VARCHAR, resource());



    public static KubernetesResourceTableColumn.ColumnValueFunction resourceMethod(Function<Discovery.APIResource, Object> fn)
    {
        return (table, _) -> fn.apply(table.resource());
    }

    public static KubernetesResourceTableColumn.ColumnValueFunction metadataMethod(Function<V1ObjectMeta, Object> fn)
    {
        return (_, object) -> fn.apply(object.getMetadata());
    }

    public static <T, U> KubernetesResourceTableColumn.ColumnValueFunction metadataMethod(Function<V1ObjectMeta, T> fn, Function<T, U> convert)
    {
        return (_, object) -> Optional.ofNullable(fn.apply(object.getMetadata())).map(convert::apply).orElse(null);
    }

    public static KubernetesResourceTableColumn.ColumnValueFunction metadata() {
        return (_, object) -> Optional.ofNullable(object.getRaw().get("metadata")).map(JsonElement::toString).orElse(null);
    }

    public static KubernetesResourceTableColumn.ColumnValueFunction resource() {
        return (_, object) -> {
            var copy = object.getRaw().deepCopy();
            copy.remove("metadata");
            return copy.toString();
        };
    }
}
