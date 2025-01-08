/*-
 * ========================LICENSE_START=================================
 * jgea-core
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
package io.github.ericmedvet.jgea.core.representation.programsynthesis.ttpn;

import io.github.ericmedvet.jgea.core.representation.programsynthesis.InstrumentedProgram;
import io.github.ericmedvet.jgea.core.representation.programsynthesis.ProgramExecutionException;
import io.github.ericmedvet.jgea.core.representation.programsynthesis.RunProfile;
import io.github.ericmedvet.jgea.core.representation.programsynthesis.type.Type;
import io.github.ericmedvet.jnb.datastructure.NamedFunction;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Runner {

  private final int maxNOfSteps;
  private final int maxNOfTokens;

  public Runner(int maxNOfSteps, int maxNOfTokens) {
    this.maxNOfSteps = maxNOfSteps;
    this.maxNOfTokens = maxNOfTokens;
  }

  private static <T> Queue<T> emptyQueue() {
    return new ArrayDeque<>();
  }

  private static <T> List<T> takeAll(Queue<T> queue) {
    List<T> list = new ArrayList<>(queue);
    queue.clear();
    return list;
  }

  private static <T> List<T> takeExactly(Queue<T> queue, int n) {
    return IntStream.range(0, n).mapToObj(i -> queue.remove()).toList();
  }

  private static <T> Optional<T> takeOne(Queue<T> queue) {
    if (queue.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(queue.remove());
  }

  public InstrumentedProgram asInstrumentedProgram(Network network) {
    return InstrumentedProgram.from(
        NamedFunction.from(
            inputs -> {
              try {
                return run(network, inputs);
              } catch (ProgramExecutionException e) {
                throw new RuntimeException(e);
              }
            },
            "ttpn[g=%d,w=%d]".formatted(network.gates().size(), network.wires().size())
        ),
        network.inputGates().values().stream().toList(),
        network.outputGates().values().stream().toList()
    );
  }

  public InstrumentedProgram.Outcome run(Network network, List<Object> inputs) throws ProgramExecutionException {
    // check validity
    List<Type> inputTypes = network.inputTypes();
    SortedMap<Integer, Type> outputTypes = network.outputGates();
    if (inputs.size() != inputTypes.size()) {
      throw new ProgramExecutionException(
          "Wrong number of inputs: %d expected, %d found".formatted(
              inputTypes.size(),
              inputs.size()
          )
      );
    }
    for (int i = 0; i < inputTypes.size(); i++) {
      if (!inputTypes.get(i).matches(inputs.get(i))) {
        throw new ProgramExecutionException(
            "Invalid input type for input %d of type %s: %s".formatted(
                i,
                inputTypes.get(i),
                inputs.get(i).getClass()
            )
        );
      }
    }
    // prepare memory
    Map<Wire, Queue<Object>> current = new HashMap<>();
    network.wires().forEach(w -> current.put(w, new ArrayDeque<>()));
    Map<Wire, List<Object>> next = new HashMap<>();
    Map<Wire, Type> actualTypes = new HashMap<>();
    for (Wire w : network.wires()) {
      Type type = network.concreteOutputType(w.src());
      if (type == null) {
        throw new ProgramExecutionException("No concrete type at output port %s".formatted(w.src()));
      }
      actualTypes.put(w, type);
    }
    // prepare state, counter, and output map
    int k = 0;
    List<RunProfile.State> states = new ArrayList<>();
    Queue<Object> networkInputsQueue = new ArrayDeque<>(inputs);
    SortedMap<Integer, Object> outputs = new TreeMap<>();
    // iterate
    while (k < maxNOfSteps) {
      for (int i = 0; i < network.gates().size(); i++) {
        int gi = i;
        Gate g = network.gates().get(gi);
        if (g instanceof Gate.InputGate) {
          if (!networkInputsQueue.isEmpty()) {
            network.wiresFrom(gi, 0).forEach(w -> next.put(w, List.of(networkInputsQueue.remove())));
          }
        } else if (g instanceof Gate.OutputGate) {
          network.wireTo(gi, 0).flatMap(w -> takeOne(current.get(w))).ifPresent(token -> outputs.put(gi, token));
        } else {
          List<Queue<Object>> inputQueues = IntStream.range(0, g.inputPorts().size())
              .mapToObj(
                  pi -> network.wireTo(
                      gi,
                      pi
                  ).map(current::get).orElse(emptyQueue())
              )
              .toList();
          // check conditions and possibly apply function
          if (IntStream.range(0, g.inputPorts().size())
              .allMatch(
                  pi -> inputQueues.get(pi).size() >= g.inputPorts()
                      .get(
                          pi
                      )
                      .n()
              )) {
            Gate.Data localIn = Gate.Data.of(
                IntStream.range(0, g.inputPorts().size())
                    .mapToObj(pi -> switch (g.inputPorts().get(pi).condition()) {
                      case EXACTLY -> takeExactly(inputQueues.get(pi), g.inputPorts().get(pi).n());
                      case AT_LEAST -> takeAll(inputQueues.get(pi));
                    })
                    .toList()
            );
            try {
              Gate.Data localOut = g.operator().apply(localIn);              // check number of outputs
              if (localOut.lines().size() != g.outputTypes().size()) {
                throw new ProgramExecutionException(
                    "Unexpected wrong number of outputs: %d expected, %d found".formatted(
                        g.outputTypes()
                            .size(),
                        localOut.lines().size()
                    )
                );
              }
              // put outputs
              IntStream.range(0, localOut.lines().size())
                  .forEach(
                      pi -> network.wiresFrom(gi, pi)
                          .forEach(w -> next.put(w, localOut.lines().get(pi)))
                  );
            } catch (RuntimeException e) {
              throw new ProgramExecutionException("Cannot run %s on %s".formatted(g, localIn), e);
            }
          }
        }
      }
      // check no new tokens
      if (next.values().stream().mapToInt(List::size).sum() == 0) {
        break;
      }
      // add tokens to current
      next.forEach((w, ts) -> current.get(w).addAll(ts));
      next.clear();
      // build state
      RunProfile.State state = RunProfile.State.from(
          current.entrySet()
              .stream()
              .collect(
                  Collectors.toMap(
                      e -> actualTypes.get(e.getKey()),
                      Map.Entry::getValue,
                      (c1, c2) -> Stream.concat(c1.stream(), c2.stream()).toList()
                  )
              )
      );
      if (state.count() > maxNOfTokens) {
        throw new ProgramExecutionException(
            "Exceeded number of tokens: %d > %d".formatted(state.count(), maxNOfTokens)
        );
      }
      states.add(state);
      // increment k
      k = k + 1;
    }
    // check output
    if (!outputTypes.keySet().equals(outputs.keySet())) {
      throw new ProgramExecutionException(
          "Missing outputs on gates: %s".formatted(
              outputTypes.keySet()
                  .stream()
                  .filter(gi -> !outputs.containsKey(gi))
                  .map(gi -> Integer.toString(gi))
                  .collect(
                      Collectors.joining(",")
                  )
          )
      );
    }
    for (int gi : outputTypes.keySet()) {
      if (!outputTypes.get(gi).matches(outputs.get(gi))) {
        throw new ProgramExecutionException(
            "Invalid output type for input %d of type %s: %s".formatted(
                gi,
                outputTypes.get(gi),
                outputs.get(gi).getClass()
            )
        );
      }
    }
    return new InstrumentedProgram.Outcome(outputs.values().stream().toList(), new RunProfile(states));
  }
}
