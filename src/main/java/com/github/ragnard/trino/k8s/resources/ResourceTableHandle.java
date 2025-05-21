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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.ragnard.trino.k8s.KubernetesColumnHandle;
import com.github.ragnard.trino.k8s.KubernetesTableHandle;
import io.airlift.slice.SizeOf;
import io.trino.spi.connector.ColumnHandle;
import io.trino.spi.connector.ConnectorTableHandle;
import io.trino.spi.connector.Constraint;
import io.trino.spi.connector.ConstraintApplicationResult;
import io.trino.spi.connector.SchemaTableName;
import io.trino.spi.predicate.Domain;
import io.trino.spi.predicate.TupleDomain;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import static com.github.ragnard.trino.k8s.resources.ResourceTableColumns.NAMESPACE;

public record ResourceTableHandle(
        @JsonProperty SchemaTableName schemaTableName,
        @JsonProperty TupleDomain<ColumnHandle> constraint,
        @JsonProperty OptionalInt limit)
        implements KubernetesTableHandle
{
    private static final int INSTANCE_SIZE = SizeOf.instanceSize(KubernetesTableHandle.class);

    public ResourceTableHandle(SchemaTableName schemaTableName)
    {
        this(schemaTableName, TupleDomain.all(), OptionalInt.empty());
    }

    public KubernetesTableHandle withConstraint(TupleDomain<ColumnHandle> newConstraint)
    {
        return new ResourceTableHandle(schemaTableName, newConstraint, limit);
    }

    public KubernetesTableHandle withLimit(int newLimit)
    {
        return new ResourceTableHandle(schemaTableName, constraint, OptionalInt.of(newLimit));
    }

    @Override
    public Optional<ConstraintApplicationResult<ConnectorTableHandle>> applyFilter(Constraint newConstraint)
    {
        TupleDomain<ColumnHandle> oldDomain = constraint();
        TupleDomain<ColumnHandle> newDomain = oldDomain.intersect(newConstraint.getSummary());
        TupleDomain<ColumnHandle> remainingFilter;
        if (newDomain.isNone()) {
            remainingFilter = TupleDomain.all();
        }
        else {
            Map<ColumnHandle, Domain> domains = newDomain.getDomains().orElseThrow();

            Map<ColumnHandle, Domain> supported = new HashMap<>();
            Map<ColumnHandle, Domain> unsupported = new HashMap<>();

            for (Map.Entry<ColumnHandle, Domain> entry : domains.entrySet()) {
                var columnHandle = (KubernetesColumnHandle) entry.getKey();
                var domain = entry.getValue();
                var columnType = columnHandle.type();

                if (columnHandle.name().equals(NAMESPACE.name()) && columnType.equals(NAMESPACE.type())) {
                    supported.put(columnHandle, domain);
                }
                else {
                    unsupported.put(columnHandle, domain);
                }
            }
            newDomain = TupleDomain.withColumnDomains(supported);
            remainingFilter = TupleDomain.withColumnDomains(unsupported);
        }

        if (oldDomain.equals(newDomain)) {
            return Optional.empty();
        }

        var newHandle = this.withConstraint(newDomain);

        return Optional.of(new ConstraintApplicationResult<>(newHandle, remainingFilter, newConstraint.getExpression(), false));
    }

    public long getRetainedSizeInBytes()
    {
        return (long) INSTANCE_SIZE
                + schemaTableName.getRetainedSizeInBytes()
                + constraint.getRetainedSizeInBytes(column -> ((KubernetesColumnHandle) column).getRetainedSizeInBytes());
    }
}
