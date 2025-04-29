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
package com.github.ragnard.trino.k8s.functions;

import com.github.ragnard.trino.k8s.client.KubernetesLogs;
import io.trino.spi.function.table.TableFunctionProcessorState;
import io.trino.spi.function.table.TableFunctionSplitProcessor;

import static io.trino.spi.function.table.TableFunctionProcessorState.Finished.FINISHED;

public class PodLogsTableFunctionSplitProcessor
        implements TableFunctionSplitProcessor
{
    private enum State
    {
        Start,
        Finished,
    }

    private final KubernetesLogs kubernetesLogs;
    private final PodLogsTableFunctionSplit split;
    private State state;

    public PodLogsTableFunctionSplitProcessor(KubernetesLogs kubernetesLogs, PodLogsTableFunctionSplit split)
    {
        this.kubernetesLogs = kubernetesLogs;
        this.split = split;
        this.state = State.Start;
    }

    @Override
    public TableFunctionProcessorState process()
    {
        return switch (this.state) {
            case Start -> {
                var handle = split.function();

                var page = this.kubernetesLogs.getLogs(handle.namespace(), split.pod(), split.container(), handle.limit());

                this.state = State.Finished;

                yield TableFunctionProcessorState.Processed.produced(page);
            }
            case Finished -> FINISHED;
        };
    }
}
