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

import com.google.common.collect.ImmutableMap;
import io.airlift.log.Level;
import io.airlift.log.Logger;
import io.airlift.log.Logging;
import io.trino.Session;
import io.trino.testing.DistributedQueryRunner;

import java.util.Map;
import java.util.Optional;

import static io.trino.testing.TestingSession.testSessionBuilder;

public class QueryRunner
{
    private QueryRunner() {}

    public static io.trino.testing.QueryRunner createQueryRunner(String catalogName, Map<String, String> catalogProperties)
            throws Exception
    {
        Logging logger = Logging.initialize();
        logger.setLevel("com.github.ragnard", Level.DEBUG);
        logger.setLevel("io.trino", Level.INFO);
        logger.setLevel("io.airlift", Level.INFO);

        Session defaultSession = testSessionBuilder()
                .setCatalog(catalogName)
                .setSchema("default")
                .build();

        ImmutableMap.Builder<String, String> extraProperties = ImmutableMap.builder();
        extraProperties.put("http-server.http.port", Optional.ofNullable(System.getenv("TRINO_PORT")).orElse("8080"));

        io.trino.testing.QueryRunner queryRunner = DistributedQueryRunner.builder(defaultSession)
                .setExtraProperties(extraProperties.buildOrThrow())
                .setWorkerCount(0)
                .build();
        queryRunner.installPlugin(new KubernetesPlugin());

        queryRunner.createCatalog(catalogName, "kubernetes", catalogProperties);

        return queryRunner;
    }

    public static void main(String[] args)
            throws Exception
    {
        ImmutableMap.Builder<String, String> properties = ImmutableMap.builder();

        io.trino.testing.QueryRunner queryRunner = createQueryRunner("kubernetes", properties.buildOrThrow());

        Logger log = Logger.get(QueryRunner.class);
        log.info("======== SERVER STARTED ========");
        log.info("\n====\n%s\n====", queryRunner.getCoordinator().getBaseUrl());
    }
}
