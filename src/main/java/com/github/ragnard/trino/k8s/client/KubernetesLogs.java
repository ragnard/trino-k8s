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
package com.github.ragnard.trino.k8s.client;

import com.github.ragnard.trino.k8s.functions.PodLogsTableFunction;
import com.google.inject.Inject;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Pod;
import io.trino.spi.Page;
import io.trino.spi.PageBuilder;
import io.trino.spi.TrinoException;
import io.trino.spi.type.LongTimestampWithTimeZone;
import io.trino.spi.type.TimestampWithTimeZoneType;
import io.trino.spi.type.VarcharType;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static io.trino.spi.StandardErrorCode.GENERIC_INTERNAL_ERROR;
import static io.trino.spi.StandardErrorCode.NOT_FOUND;
import static io.trino.spi.type.TimeZoneKey.UTC_KEY;
import static io.trino.spi.type.Timestamps.PICOSECONDS_PER_NANOSECOND;
import static java.util.Objects.requireNonNull;

public class KubernetesLogs
{
    private final CoreV1Api coreApi;
    private final AppsV1Api appsApi;

    @Inject
    public KubernetesLogs(ApiClient apiClient)
    {
        this.coreApi = new CoreV1Api(apiClient);
        this.appsApi = new AppsV1Api(apiClient);
    }

    public List<V1Pod> getPods(String namespace, String selector)
    {
        try {
            var parts = selector.split("/", 2);
            if (parts.length == 1) {
                return getPodsByName(namespace, selector);
            }

            return switch (parts[0].toLowerCase()) {
                case "pod" -> getPodsByName(namespace, parts[1]);
                case "deployment" -> getPodsByDeployment(namespace, parts[1]);
                case "statefulset" -> getPodsByStatefulSet(namespace, parts[1]);
                case "replicaset" -> getPodsByReplicaSet(namespace, parts[1]);
                default -> throw new IllegalStateException("Unexpected value: " + parts[0].toLowerCase());
            };
        }
        catch (ApiException e) {
            if (e.getCode() == 404) {
                throw new TrinoException(NOT_FOUND, "Kubernetes object not found");
            }

            throw new TrinoException(GENERIC_INTERNAL_ERROR, "Kubernetes API error: %d".formatted(e.getCode()));
        }
    }

    private List<V1Pod> getPodsByDeployment(String namespace, String deploymentName)
            throws ApiException
    {
        var deployment = this.appsApi.readNamespacedDeployment(deploymentName, namespace, null);

        var replicaSets = this.appsApi.listNamespacedReplicaSet(namespace, null, null, null,
                        null, null, null, null, null, null, null)
                .getItems()
                .stream()
                .filter(hasOwner(deployment.getMetadata().getUid()));

        var replicaSetUids = replicaSets
                .map(s -> s.getMetadata().getUid())
                .collect(Collectors.toSet());

        return getPods(namespace).stream().filter(hasOwner(replicaSetUids)).toList();
    }

    private List<V1Pod> getPodsByStatefulSet(String namespace, String statefulsetName)
            throws ApiException
    {
        var statefulset = this.appsApi.readNamespacedStatefulSet(namespace, statefulsetName, null);
        var statefulsetUid = statefulset.getMetadata().getUid();

        return getPods(namespace).stream().filter(hasOwner(statefulsetUid)).toList();
    }

    private List<V1Pod> getPodsByReplicaSet(String namespace, String replicasetName)
            throws ApiException
    {
        var replicaSet = this.appsApi.readNamespacedReplicaSet(replicasetName, namespace, null);
        var replicaSetUid = replicaSet.getMetadata().getUid();

        return getPods(namespace).stream().filter(hasOwner(replicaSetUid)).toList();
    }

    private List<V1Pod> getPods(String namespace)
            throws ApiException
    {
        return this.coreApi.listNamespacedPod(namespace, null, null, null,
                null, null, null, null, null, null, null).getItems();
    }

    public List<V1Pod> getPodsByName(String namespace, String pod)
            throws ApiException
    {
        return List.of(this.coreApi.readNamespacedPod(pod, namespace, null));
    }

    private static Predicate<KubernetesObject> hasOwner(String uid)
    {
        return object -> requireNonNull(object.getMetadata().getOwnerReferences())
                .stream()
                .anyMatch(r -> uid.equals(r.getUid()));
    }

    private static Predicate<KubernetesObject> hasOwner(Set<String> uids)
    {
        return object -> requireNonNull(object.getMetadata().getOwnerReferences())
                .stream()
                .anyMatch(r -> uids.contains(r.getUid()));
    }

    public Page getLogs(String namespace, String pod, String container, OptionalInt limit)
    {
        PageBuilder builder = new PageBuilder(PodLogsTableFunction.COLUMN_TYPES);

        consumePodLog(namespace, pod, container, limit.isPresent() ? limit.getAsInt() : null, line -> {
            VarcharType.VARCHAR.writeString(builder.getBlockBuilder(0), pod);

            if (container != null) {
                VarcharType.VARCHAR.writeString(builder.getBlockBuilder(1), container);
            }
            else {
                builder.getBlockBuilder(1).appendNull();
            }

            TimestampWithTimeZoneType.TIMESTAMP_TZ_NANOS.writeObject(builder.getBlockBuilder(2), toTimestampWithZone(line.timestamp()));

            VarcharType.VARCHAR.writeString(builder.getBlockBuilder(3), removeAnsiCodes(line.log()));
            builder.declarePosition();
        });

        return builder.build();
    }

    private void consumePodLog(String namespace, String name, String container, Integer limit, Consumer<PodLogLine> consumer)
    {
        try {
            var call = this.coreApi.readNamespacedPodLogCall(
                    name,
                    namespace,
                    container,
                    false, // follow
                    false, // insecureSkipTLSVerifyBackend
                    null, // limitBytes
                    "false", // pretty
                    false, // previous
                    null, // sinceSeconds
                    limit, // tailLines
                    true, // timestamps
                    null);

            try (var response = call.execute()) {
                if (!response.isSuccessful()) {
                    throw new RuntimeException("log call failed: " + Optional.ofNullable(response.networkResponse()).map(Response::code).orElse(null));
                }
                try (var reader = new BufferedReader(new InputStreamReader(Optional.ofNullable(response.body())
                        .map(ResponseBody::byteStream)
                        .orElseThrow(() -> new TrinoException(GENERIC_INTERNAL_ERROR, "Pod log response empty body"))))) {
                    reader.lines().map(PodLogLine::parse).forEach(consumer);
                }
            }
            catch (IOException e) {
                throw new TrinoException(GENERIC_INTERNAL_ERROR, "Retrieving pod logs", e);
            }
        }
        catch (ApiException e) {
            if (e.getCode() == 404) {
                throw new TrinoException(NOT_FOUND, "Kubernetes object not found");
            }

            throw new TrinoException(GENERIC_INTERNAL_ERROR, "Kubernetes API error: %d".formatted(e.getCode()));
        }
    }

    public record PodLogLine(OffsetDateTime timestamp, String log)
    {
        public static PodLogLine parse(String line)
        {
            var parts = line.split(" ", 2);
            var timestamp = OffsetDateTime.parse(parts[0], DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            var log = parts[1];

            return new PodLogLine(timestamp, log);
        }
    }

    public static String removeAnsiCodes(String input)
    {
        return input.replaceAll("\u001B\\[[;\\d]*[ -/]*[@-~]", "");
    }

    private static LongTimestampWithTimeZone toTimestampWithZone(OffsetDateTime timestamp)
    {
        return LongTimestampWithTimeZone.fromEpochSecondsAndFraction(
                timestamp.toEpochSecond(),
                (long) timestamp.getNano() * PICOSECONDS_PER_NANOSECOND,
                UTC_KEY);
    }
}
