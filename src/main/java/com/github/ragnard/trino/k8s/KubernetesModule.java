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

import com.github.ragnard.trino.k8s.data.KubernetesData;
import com.google.inject.Binder;
import io.airlift.configuration.AbstractConfigurationAwareModule;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.util.ClientBuilder;
import io.trino.spi.NodeManager;
import io.trino.spi.type.TypeManager;

import java.io.IOException;

import static com.google.inject.Scopes.SINGLETON;
import static java.util.Objects.requireNonNull;

public class KubernetesModule
        extends AbstractConfigurationAwareModule
{
    private final NodeManager nodeManager;
    private final TypeManager typeManager;

    public KubernetesModule(NodeManager nodeManager, TypeManager typeManager)
    {
        this.nodeManager = requireNonNull(nodeManager, "nodeManager is null");
        this.typeManager = requireNonNull(typeManager, "typeManager is null");
    }

    @Override
    protected void setup(Binder binder)
    {
        binder.bind(NodeManager.class).toInstance(nodeManager);
        binder.bind(TypeManager.class).toInstance(typeManager);

        binder.bind(KubernetesConnector.class).in(SINGLETON);
        binder.bind(KubernetesMetadata.class).in(SINGLETON);
        binder.bind(KubernetesSplitManager.class).in(SINGLETON);
        binder.bind(KubernetesRecordSetProvider.class).in(SINGLETON);
        //configBinder(binder).bindConfig(KubernetesConfig.class);

        binder.bind(ApiClient.class).toProvider(() -> {
            try {
                return ClientBuilder.standard().build();
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        binder.bind(KubernetesData.class).in(SINGLETON);
    }
}
