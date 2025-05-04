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

import com.github.ragnard.trino.k8s.client.KubernetesClient;
import com.github.ragnard.trino.k8s.client.KubernetesLogs;
import com.google.inject.Binder;
import com.google.inject.Provider;
import io.airlift.configuration.AbstractConfigurationAwareModule;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.util.ClientBuilder;

import java.io.IOException;

import static com.google.inject.Scopes.SINGLETON;
import static io.airlift.configuration.ConfigBinder.configBinder;

public class KubernetesModule
        extends AbstractConfigurationAwareModule
{
    @Override
    protected void setup(Binder binder)
    {
        // configBinder(binder).bindConfig(KubernetesConfig.class);

        binder.bind(KubernetesConnector.class).in(SINGLETON);
        binder.bind(KubernetesMetadata.class).in(SINGLETON);
        binder.bind(KubernetesRecordSetProvider.class).in(SINGLETON);
        binder.bind(KubernetesSplitManager.class).in(SINGLETON);
        binder.bind(KubernetesTableFunctionProcessorProvider.class).in(SINGLETON);

        binder.bind(KubernetesClient.class).in(SINGLETON);
        binder.bind(KubernetesLogs.class).in(SINGLETON);

        binder.bind(ApiClient.class).toProvider(ApiClientProvider.class);
    }

    public static class ApiClientProvider
            implements Provider<ApiClient>
    {
        @Override
        public ApiClient get()
        {
            try {
                return ClientBuilder.standard(false).build();
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
