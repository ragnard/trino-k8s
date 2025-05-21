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
package com.github.ragnard.trino.k8s.resources;

import com.github.ragnard.trino.k8s.KubernetesTypes;
import com.google.gson.JsonElement;
import io.kubernetes.client.Discovery;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.trino.spi.type.BigintType;
import io.trino.spi.type.TimestampWithTimeZoneType;
import io.trino.spi.type.VarcharType;

import java.util.Optional;
import java.util.function.Function;

import static com.github.ragnard.trino.k8s.KubernetesTypes.STRING_ARRAY;
import static com.github.ragnard.trino.k8s.KubernetesTypes.STRING_MAP;

public class ResourceTableColumns
{
    public static final ResourceTableColumn KIND = new ResourceTableColumn("kind", VarcharType.VARCHAR, resourceMethod(Discovery.APIResource::getKind));
    public static final ResourceTableColumn GROUP = new ResourceTableColumn("group", VarcharType.VARCHAR, resourceMethod(Discovery.APIResource::getGroup));
    public static final ResourceTableColumn API_VERSION = new ResourceTableColumn("apiVersion", VarcharType.VARCHAR, resourceMethod(Discovery.APIResource::getPreferredVersion));

    // V1ObjectMeta
    public static final ResourceTableColumn NAME = new ResourceTableColumn("name", VarcharType.VARCHAR, metadataMethod(V1ObjectMeta::getName));
    public static final ResourceTableColumn NAMESPACE = new ResourceTableColumn("namespace", VarcharType.VARCHAR, metadataMethod(V1ObjectMeta::getNamespace));
    public static final ResourceTableColumn LABELS = new ResourceTableColumn("labels", STRING_MAP, metadataMethod(V1ObjectMeta::getLabels, KubernetesTypes::stringMap));
    public static final ResourceTableColumn ANNOTATIONS = new ResourceTableColumn("annotations", STRING_MAP, metadataMethod(V1ObjectMeta::getAnnotations, KubernetesTypes::stringMap));
    public static final ResourceTableColumn CLUSTER_NAME = new ResourceTableColumn("clusterName", VarcharType.VARCHAR, metadataMethod(V1ObjectMeta::getClusterName));
    public static final ResourceTableColumn CREATION_TIMESTAMP = new ResourceTableColumn("creationTimestamp", TimestampWithTimeZoneType.TIMESTAMP_TZ_MICROS, metadataMethod(V1ObjectMeta::getCreationTimestamp, KubernetesTypes::toTimestamp));
    public static final ResourceTableColumn DELETION_GRACE_PERIOD_SECONDS = new ResourceTableColumn("deletionGracePeriodSeconds", BigintType.BIGINT, metadataMethod(V1ObjectMeta::getDeletionGracePeriodSeconds));
    public static final ResourceTableColumn DELETION_TIMESTAMP = new ResourceTableColumn("deletionTimestamp", TimestampWithTimeZoneType.TIMESTAMP_TZ_MICROS, metadataMethod(V1ObjectMeta::getDeletionTimestamp, KubernetesTypes::toTimestamp));
    public static final ResourceTableColumn FINALIZERS = new ResourceTableColumn("finalizers", STRING_ARRAY, metadataMethod(V1ObjectMeta::getFinalizers, KubernetesTypes::stringArray));
    public static final ResourceTableColumn RESOURCE_VERSION = new ResourceTableColumn("resourceVersion", VarcharType.VARCHAR, metadataMethod(V1ObjectMeta::getResourceVersion));
    public static final ResourceTableColumn SELF_LINK = new ResourceTableColumn("selfLink", VarcharType.VARCHAR, metadataMethod(V1ObjectMeta::getSelfLink));
    public static final ResourceTableColumn UID = new ResourceTableColumn("uid", VarcharType.VARCHAR, metadataMethod(V1ObjectMeta::getUid));

    //
    public static final ResourceTableColumn METADATA = new ResourceTableColumn("metadata", VarcharType.VARCHAR, metadata());
    public static final ResourceTableColumn RESOURCE = new ResourceTableColumn("resource", VarcharType.VARCHAR, resource());

    private ResourceTableColumns() {}

    public static ResourceTableColumn.ColumnValueFunction resourceMethod(Function<Discovery.APIResource, Object> fn)
    {
        return (table, _) -> fn.apply(table.resource());
    }

    public static ResourceTableColumn.ColumnValueFunction metadataMethod(Function<V1ObjectMeta, Object> fn)
    {
        return (_, object) -> fn.apply(object.getMetadata());
    }

    public static <T, U> ResourceTableColumn.ColumnValueFunction metadataMethod(Function<V1ObjectMeta, T> fn, Function<T, U> convert)
    {
        return (_, object) -> Optional.ofNullable(fn.apply(object.getMetadata())).map(convert::apply).orElse(null);
    }

    public static ResourceTableColumn.ColumnValueFunction metadata()
    {
        return (_, object) -> Optional.ofNullable(object.getRaw().get("metadata")).map(JsonElement::toString).orElse(null);
    }

    public static ResourceTableColumn.ColumnValueFunction resource()
    {
        return (_, object) -> {
            var copy = object.getRaw().deepCopy();
            copy.remove("metadata");
            return copy.toString();
        };
    }
}
