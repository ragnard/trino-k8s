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
package com.github.ragnard.trino.k8s.tables;

import io.trino.spi.block.Block;
import io.trino.spi.block.BufferedArrayValueBuilder;
import io.trino.spi.block.BufferedMapValueBuilder;
import io.trino.spi.block.SqlMap;
import io.trino.spi.type.ArrayType;
import io.trino.spi.type.LongTimestampWithTimeZone;
import io.trino.spi.type.MapType;
import io.trino.spi.type.TypeOperators;
import io.trino.spi.type.VarcharType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static io.trino.spi.type.TimeZoneKey.UTC_KEY;
import static io.trino.spi.type.Timestamps.PICOSECONDS_PER_NANOSECOND;

public class KubernetesTypes
{
    public static final ArrayType STRING_ARRAY = new ArrayType(VarcharType.VARCHAR);
    public static final MapType STRING_MAP = new MapType(VarcharType.VARCHAR, VarcharType.VARCHAR, new TypeOperators());

    private KubernetesTypes() {}

    public static Block stringArray(List<String> l)
    {
        if (l == null) {
            return null;
        }

        return BufferedArrayValueBuilder.createBuffered(STRING_ARRAY).build(l.size(), lb -> {
            l.forEach(v -> VarcharType.VARCHAR.writeString(lb, v));
        });
    }

    public static SqlMap stringMap(Map<String, String> m)
    {
        if (m == null) {
            return null;
        }

        return BufferedMapValueBuilder.createBuffered(STRING_MAP).build(m.size(), (kb, vb) -> {
            m.forEach((k, v) -> {
                VarcharType.VARCHAR.writeString(kb, k);
                VarcharType.VARCHAR.writeString(vb, v);
            });
        });
    }

    public static LongTimestampWithTimeZone toTimestamp(OffsetDateTime dateTime)
    {
        if (dateTime == null) {
            return null;
        }

        return LongTimestampWithTimeZone.fromEpochSecondsAndFraction(
                dateTime.toEpochSecond(),
                (long) dateTime.getNano() * PICOSECONDS_PER_NANOSECOND,
                UTC_KEY);
    }
}
