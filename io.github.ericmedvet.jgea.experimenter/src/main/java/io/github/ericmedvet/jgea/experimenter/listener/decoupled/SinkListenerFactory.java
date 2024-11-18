/*-
 * ========================LICENSE_START=================================
 * jgea-experimenter
 * %%
 * Copyright (C) 2018 - 2024 Eric Medvet
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
package io.github.ericmedvet.jgea.experimenter.listener.decoupled;

import io.github.ericmedvet.jgea.core.listener.Listener;
import io.github.ericmedvet.jgea.core.listener.ListenerFactory;
import io.github.ericmedvet.jgea.core.solver.POCPopulationState;
import io.github.ericmedvet.jgea.core.util.Progress;
import io.github.ericmedvet.jgea.experimenter.Experiment;
import io.github.ericmedvet.jgea.experimenter.Run;
import io.github.ericmedvet.jnb.datastructure.FormattedFunction;
import io.github.ericmedvet.jnb.datastructure.NamedFunction;
import io.github.ericmedvet.jnb.datastructure.Pair;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

public class SinkListenerFactory<G, S, Q> implements ListenerFactory<POCPopulationState<?, G, S, Q, ?>, Run<?, G, S, Q>> {
  private final List<? extends Function<? super POCPopulationState<?, G, S, Q, ?>, ?>> stateFunctions;
  private final List<? extends Function<? super Run<?, G, S, Q>, ?>> runFunctions;
  private final Experiment experiment;
  private final Sink<MachineKey, MachineInfo> machineSink;
  private final Sink<ProcessKey, ProcessInfo> processSink;
  private final Sink<ExperimentKey, ExperimentInfo> experimentSink;
  private final Sink<ProcessKey, LogInfo> logSink;
  private final Sink<RunKey, RunInfo> runSink;
  private final Sink<DataItemKey, DataItemInfo> dataItemSink;

  private final AutoEmitterSink<MachineKey, MachineInfo> machineAutoEmitterSink;
  private final AutoEmitterSink<ProcessKey, ProcessInfo> processAutoEmitterSink;
  private final AutoEmitterSink<ExperimentKey, ExperimentInfo> experimentAutoEmitterSink;
  private final LogCapturer logCapturer;

  public SinkListenerFactory(
      List<? extends Function<? super POCPopulationState<?, G, S, Q, ?>, ?>> stateFunctions,
      List<? extends Function<? super Run<?, G, S, Q>, ?>> runFunctions,
      Experiment experiment,
      Sink<MachineKey, MachineInfo> machineSink,
      Sink<ProcessKey, ProcessInfo> processSink,
      Sink<ProcessKey, LogInfo> logSink,
      Sink<ExperimentKey, ExperimentInfo> experimentSink,
      Sink<RunKey, RunInfo> runSink,
      Sink<DataItemKey, DataItemInfo> dataItemSink
  ) {
    this.stateFunctions = stateFunctions;
    this.runFunctions = runFunctions;
    this.experiment = experiment;
    this.machineSink = machineSink;
    this.processSink = processSink;
    this.experimentSink = experimentSink;
    this.logSink = logSink;
    this.runSink = runSink;
    this.dataItemSink = dataItemSink;
    LocalDateTime now = LocalDateTime.now();
    machineAutoEmitterSink = new AutoEmitterSink<>(
        1000,
        () -> new Pair<>(MachineKey.local(), MachineInfo.local()),
        machineSink
    );
    processAutoEmitterSink = new AutoEmitterSink<>(
        1000,
        () -> new Pair<>(ProcessKey.local(), ProcessInfo.local()),
        processSink
    );
    experimentAutoEmitterSink = new AutoEmitterSink<>(
        5000,
        () -> new Pair<>(
            experimentKey(),
            new ExperimentInfo(
                experiment.map().toString(),
                experiment.runs().size(),
                Stream.of(runFunctions, stateFunctions)
                    .flatMap(List::stream)
                    .map(f -> new Pair<>(NamedFunction.name(f), FormattedFunction.format(f)))
                    .toList(),
                now
            )
        ),
        experimentSink
    );
    logCapturer = new LogCapturer(
        lr -> logSink.push(
            LocalDateTime.ofInstant(lr.getInstant(), ZoneId.systemDefault()),
            ProcessKey.local(),
            new LogInfo(lr.getLevel(), lr.getMessage())
        ),
        false
    );
  }

  @Override
  public Listener<POCPopulationState<?, G, S, Q, ?>> build(Run<?, G, S, Q> run) {
    RunKey runKey = runKey(run);
    LocalDateTime startDateTime = LocalDateTime.now();
    runSink.push(runKey, new RunInfo(run.index(), startDateTime, Progress.NA, false));
    runFunctions.forEach(
        f -> dataItemSink.push(new DataItemKey(runKey, NamedFunction.name(f)), new DataItemInfo(f.apply(run)))
    );
    return new Listener<>() {
      @Override
      public void listen(POCPopulationState<?, G, S, Q, ?> state) {
        synchronized (runSink) {
          LocalDateTime now = LocalDateTime.now();
          runSink.push(now, runKey, new RunInfo(run.index(), startDateTime, state.progress(), false));
          stateFunctions.forEach(
              f -> dataItemSink.push(
                  now,
                  new DataItemKey(runKey, NamedFunction.name(f)),
                  new DataItemInfo(f.apply(state))
              )
          );
        }
      }

      @Override
      public void done() {
        synchronized (runSink) {
          runSink.push(runKey, new RunInfo(run.index(), startDateTime, Progress.DONE, true));
        }
      }

      @Override
      public String toString() {
        return "sink(state:%d;run=%d)".formatted(stateFunctions.size(), runFunctions.size());
      }
    };
  }

  @Override
  public void shutdown() {
    machineAutoEmitterSink.shutdown();
    processAutoEmitterSink.shutdown();
    experimentAutoEmitterSink.shutdown();
    logCapturer.close();
    machineSink.close();
    processSink.close();
    experimentSink.close();
    runSink.close();
    logSink.close();
    dataItemSink.close();
  }

  private ExperimentKey experimentKey() {
    return new ExperimentKey(ProcessKey.local(), experiment.name());
  }

  private RunKey runKey(Run<?, ?, ?, ?> run) {
    return new RunKey(experimentKey(), "r%04d".formatted(run.index()));
  }
}
