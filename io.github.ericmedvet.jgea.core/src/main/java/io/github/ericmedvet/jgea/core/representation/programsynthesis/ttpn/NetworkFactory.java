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

import io.github.ericmedvet.jgea.core.IndependentFactory;
import io.github.ericmedvet.jgea.core.representation.programsynthesis.type.Type;
import io.github.ericmedvet.jgea.core.representation.programsynthesis.type.TypeException;
import java.util.*;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NetworkFactory implements IndependentFactory<Network> {
  private final List<Type> inputTypes;
  private final List<Type> outputTypes;
  private final SequencedSet<Gate> gates;
  private final int maxNOfGates;
  private final Map<Signature, Map<Integer, List<Network>>> partialNetworks;

  public NetworkFactory(List<Type> inputTypes, List<Type> outputTypes, SequencedSet<Gate> gates, int maxNOfGates) {
    this.inputTypes = inputTypes;
    this.outputTypes = outputTypes;
    this.gates = gates;
    this.maxNOfGates = maxNOfGates;
    partialNetworks = new LinkedHashMap<>();
    // iteratively populate map
    long seed = 1;
    RandomGenerator rnd = new Random(seed);
    List<Gate> lGates = gates.stream().toList();
    int partialNetworkMaxSize = 5;
    int minNOfPartialNetworks = 5;

  }

  private record Signature(List<Type> inputTypes, List<Type> outputTypes) {}

  @Override
  public Network build(RandomGenerator random) {
    try {
      Network n = new Network(
          Stream.concat(
              inputTypes.stream().map(type -> (Gate) Gate.input(type)),
              outputTypes.stream().map(Gate::output)
          ).toList(),
          Set.of()
      );
      while (!n.freeInputPorts().isEmpty()) {
        LinkedHashSet<Type> oTypes = n.freeOutputPorts()
            .stream()
            .map(n::concreteOutputType)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        List<Gate> suitableGates = gates.stream()
            .filter(
                g -> g.inputPorts()
                    .stream()
                    .anyMatch(p -> oTypes.stream().anyMatch(t -> p.type().canTakeValuesOf(t)))
            )
            .toList();

        System.out.println("oTypes: " + oTypes);
        System.out.println("suitableGates: " + suitableGates);

        Gate gate = suitableGates.get(random.nextInt(suitableGates.size()));
        n = n.mergedWith(new Network(List.of(gate), Set.of()));
        int inputT = n.gates().size() >= maxNOfGates ? 0 : 1;
        int outputT = n.gates().size() >= maxNOfGates ? 0 : Integer.MAX_VALUE;
        while (n.freeInputPorts().size() > inputT || n.freeOutputPorts().size() > outputT) {
          Network wiredN = n.wireFreeInputPorts((t, ts) -> random.nextInt(ts.size()))
              .wireFreeOutputPorts((t, ts) -> random.nextInt(ts.size()));
          if (wiredN.equals(n)) {
            break;
          }
          n = wiredN;
        }
        System.out.println("========>");
        System.out.println(n);
      }
      return n;
    } catch (NetworkStructureException | TypeException e) {
      throw new RuntimeException(e);
    }
  }

}
