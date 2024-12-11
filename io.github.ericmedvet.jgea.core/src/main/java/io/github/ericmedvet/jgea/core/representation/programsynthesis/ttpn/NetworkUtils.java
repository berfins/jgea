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

import io.github.ericmedvet.jgea.core.representation.programsynthesis.type.Type;
import io.github.ericmedvet.jgea.core.representation.programsynthesis.type.TypeException;

import java.util.*;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class NetworkUtils {

  private NetworkUtils() {
  }

  public static Network randomSubnetwork(
      Network n,
      RandomGenerator rnd,
      int targetNOfGates
  ) throws NetworkStructureException, TypeException {
    List<Integer> gis = new ArrayList<>();
    List<Integer> availableGis = IntStream.range(0, n.gates().size())
        .boxed()
        .filter(gi -> !(n.gates().get(gi) instanceof Gate.InputGate || n.gates().get(gi) instanceof Gate.OutputGate))
        .toList();
    while (gis.size() < targetNOfGates) {
      if (!gis.isEmpty()) {
        availableGis = gis.stream()
            .flatMap(
                gi -> Stream.concat(
                    n.wiresFrom(gi).stream().map(w -> w.dst().gateIndex()),
                    n.wiresTo(gi).stream().map(w -> w.src().gateIndex())
                )
            )
            .distinct()
            .filter(gi -> !gis.contains(gi))
            .filter(gi -> !(n.gates().get(gi) instanceof Gate.InputGate || n.gates()
                .get(gi) instanceof Gate.OutputGate))
            .toList();
      }
      if (availableGis.isEmpty()) {
        break;
      }
      gis.add(availableGis.get(rnd.nextInt(availableGis.size())));
    }
    // find and remap wires
    SequencedSet<Wire> wires = n.wires()
        .stream()
        .filter(w -> gis.contains(w.src().gateIndex()) && gis.contains(w.dst().gateIndex()))
        .map(
            w -> Wire.of(
                gis.indexOf(w.src().gateIndex()),
                w.src().portIndex(),
                gis.indexOf(w.dst().gateIndex()),
                w.dst().portIndex()
            )
        )
        .collect(Collectors.toCollection(LinkedHashSet::new));
    return new Network(
        gis.stream().map(gi -> n.gates().get(gi)).toList(),
        wires
    );
  }

  public static Network rewire(Network n, SequencedSet<Gate> gates, RandomGenerator rnd, int maxNOfGates) {
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
      List<Gate> suitableGates = suitableGates(gates, oTypes, iTypes);
      Gate gate = suitableGates.get(rnd.nextInt(suitableGates.size()));
      try {
        n = n.mergedWith(new Network(List.of(gate), Set.of()))
            .wireFreeOutputPorts(ts -> rnd.nextInt(ts.size()))
            .wireFreeInputPorts(ts -> rnd.nextInt(ts.size()));
      } catch (NetworkStructureException | TypeException e) {
        return n;
      }
      if (n.gates().size() > maxNOfGates) {
        return n;
      }
    }
    return n;
  }

  private static List<Gate> suitableGates(
      SequencedSet<Gate> gates,
      SequencedSet<Type> oTypes,
      SequencedSet<Type> iTypes
  ) {
    List<Gate> oCompatibleGates = gates.stream()
        .filter(
            g -> g.inputPorts()
                .stream()
                .anyMatch(p -> oTypes.stream().anyMatch(t -> p.type().canTakeValuesOf(t)))
        )
        .toList();
    List<Gate> iCompatibleGates = gates.stream()
        .filter(
            g -> g.outputTypes()
                .stream()
                .anyMatch(ot -> iTypes.stream().anyMatch(it -> it.canTakeValuesOf(ot)))
        )
        .toList();
    List<Gate> ioCompatibleGates = new ArrayList<>(iCompatibleGates);
    ioCompatibleGates.retainAll(oCompatibleGates);
    if (!ioCompatibleGates.isEmpty()) {
      return ioCompatibleGates;
    }
    if (!iCompatibleGates.isEmpty()) {
      return iCompatibleGates;
    }
    return oCompatibleGates;
  }
}
