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

  public NetworkFactory(List<Type> inputTypes, List<Type> outputTypes, SequencedSet<Gate> gates, int maxNOfGates) {
    this.inputTypes = inputTypes;
    this.outputTypes = outputTypes;
    this.gates = gates;
    this.maxNOfGates = maxNOfGates;
  }

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
      while (!n.freeInputPorts().isEmpty() || !n.freeOutputPorts().isEmpty()) {
        SequencedSet<Type> oTypes = n.freeOutputPorts()
            .stream()
            .map(n::concreteOutputType)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        SequencedSet<Type> iTypes = n.freeInputPorts()
            .stream()
            .map(n::inputType)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        List<Gate> suitableGates = suitableGates(oTypes, iTypes);
        if (suitableGates.isEmpty()) {
          suitableGates = suitableGates(oTypes, null);
        }
        if (suitableGates.isEmpty()) {
          suitableGates = suitableGates(null, iTypes);
        }
        if (suitableGates.isEmpty()) {
          suitableGates = suitableGates(null, null);
        }
        Gate gate = suitableGates.get(random.nextInt(suitableGates.size()));
        n = n.mergedWith(new Network(List.of(gate), Set.of()))
            .wireFreeOutputPorts(ts -> random.nextInt(ts.size()))
            .wireFreeInputPorts(ts -> random.nextInt(ts.size()));
        if (n.gates().size() > maxNOfGates) {
          return n;
        }
      }
      return n;
    } catch (NetworkStructureException | TypeException e) {
      throw new RuntimeException(e);
    }
  }

  private List<Gate> suitableGates(SequencedSet<Type> oTypes, SequencedSet<Type> iTypes) {
    return gates.stream()
        .filter(
            g -> oTypes == null || g.inputPorts()
                .stream()
                .anyMatch(p -> oTypes.stream().anyMatch(t -> p.type().canTakeValuesOf(t)))
        )
        .filter(
            g -> iTypes == null || g.outputTypes()
                .stream()
                .anyMatch(ot -> iTypes.stream().anyMatch(it -> it.canTakeValuesOf(ot)))
        )
        .toList();
  }

}
