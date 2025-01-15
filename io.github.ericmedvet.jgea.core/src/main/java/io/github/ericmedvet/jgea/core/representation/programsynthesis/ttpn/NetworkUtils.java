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
import io.github.ericmedvet.jgea.core.util.Misc;
import java.util.*;
import java.util.function.Function;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class NetworkUtils {

  private NetworkUtils() {
  }

  public static List<Integer> compatibleInputPorts(Gate gate, Type type) {
    return IntStream.range(0, gate.inputPorts().size())
        .filter(pi -> gate.inputPorts().get(pi).type().canTakeValuesOf(type))
        .boxed()
        .toList();
  }

  public static List<Integer> compatibleOutputPorts(Gate gate, Type type) {
    return IntStream.range(0, gate.outputTypes().size())
        .filter(pi -> gate.outputTypes().get(pi).canTakeValuesOf(type))
        .boxed()
        .toList();
  }

  public static Comparator<Network> deadComparator() {
    return Comparator.comparingInt(network -> network.deadGates().size());
  }

  public static Network grow(Network n, SequencedSet<Gate> gates, RandomGenerator rnd, int maxNOfGates) {
    while (!n.freeInputEndPoints().isEmpty() || !n.freeOutputEndPoints().isEmpty()) {
      SequencedSet<Type> oTypes = n.freeOutputEndPoints()
          .stream()
          .map(n::concreteOutputType)
          .filter(Objects::nonNull)
          .collect(Collectors.toCollection(LinkedHashSet::new));
      SequencedSet<Type> iTypes = n.freeInputEndPoints()
          .stream()
          .map(n::inputType)
          .filter(Objects::nonNull)
          .collect(Collectors.toCollection(LinkedHashSet::new));
      List<Gate> suitableGates = suitableGates(gates, oTypes, iTypes);
      Gate gate = suitableGates.get(rnd.nextInt(suitableGates.size()));
      try {
        n = n.mergedWith(new Network(List.of(gate), Set.of()))
            .wireFreeOutputEndPoints(ts -> rnd.nextInt(ts.size()))
            .wireFreeInputEndPoints(ts -> rnd.nextInt(ts.size()));
      } catch (NetworkStructureException | TypeException e) {
        return n;
      }
      if (n.gates().size() > maxNOfGates) {
        return n;
      }
    }
    return n;
  }

  public static Network.Addition growOnBoth(
      Network n,
      SequencedSet<Gate> gates,
      RandomGenerator rnd
  ) throws NetworkStructureException, TypeException {
    List<Wire.EndPoint> iEps = new ArrayList<>(n.freeInputEndPoints());
    List<Wire.EndPoint> oEps = new ArrayList<>(n.freeOutputEndPoints());
    Collections.shuffle(iEps, rnd);
    Collections.shuffle(oEps, rnd);
    for (Wire.EndPoint iEp : iEps) {
      for (Wire.EndPoint oEp : oEps) {
        Network.Addition addition = growOnEndPoints(n, oEp, iEp, gates, rnd);
        if (!addition.isEmpty())
          return addition;
      }
    }
    return Network.Addition.empty();
  }

  // TODO modify to add more than one gates at once
  public static Network.Addition growOnEndPoints(
      Network network,
      Wire.EndPoint srcEndPoint,
      Wire.EndPoint dstEndPoint,
      SequencedSet<Gate> gates,
      RandomGenerator rnd
  ) {
    List<Gate> shuffledGates = new ArrayList<>(gates);
    Collections.shuffle(shuffledGates, rnd);
    Type dstType = network.concreteInputType(dstEndPoint);
    Type srcType = network.concreteOutputType(srcEndPoint);
    for (Gate gate : shuffledGates) {
      List<Integer> suitableIps = IntStream.range(0, gate.inputPorts().size())
          .filter(
              pi -> gate.inputPorts()
                  .get(pi)
                  .type()
                  .canTakeValuesOf(srcType)
          )
          .boxed()
          .toList();
      List<Integer> suitableOps = IntStream.range(0, gate.outputTypes().size())
          .filter(pi -> dstType.canTakeValuesOf(gate.outputTypes().get(pi)))
          .boxed()
          .toList();
      if (!suitableIps.isEmpty() && !suitableOps.isEmpty()) {
        int newGateIndex = freeGateIndex(network);
        Wire inWire = new Wire(srcEndPoint, new Wire.EndPoint(newGateIndex, Misc.pickRandomly(suitableIps, rnd)));
        Wire outWire = new Wire(new Wire.EndPoint(newGateIndex, Misc.pickRandomly(suitableOps, rnd)), dstEndPoint);
        return new Network.Addition(Map.of(newGateIndex, gate), Set.of(inWire, outWire));
      }
    }
    return Network.Addition.empty();
  }

  public static int freeGateIndex(Network network) {
    return IntStream.range(0, network.gates().size() + 1)
        .filter(gi -> !network.gates().containsKey(gi))
        .min()
        .orElseThrow();
  }

  public static Network.Addition growOnInputs(
      Network network,
      SequencedSet<Gate> gates,
      RandomGenerator rnd
  ) throws NetworkStructureException, TypeException {
    record WeightedEndPoint(Wire.EndPoint endPoint, int distance) {}
    Optional<Wire.EndPoint> oFreeEndPoint = network.freeInputEndPoints()
        .stream()
        .map(ep -> new WeightedEndPoint(ep, network.outputDistanceFrom(Gate.OutputGate.class, ep.gateIndex())))
        .min(Comparator.comparingInt(WeightedEndPoint::distance))
        .map(WeightedEndPoint::endPoint);
    if (oFreeEndPoint.isEmpty()) {
      return Network.Addition.empty();
    }
    Wire.EndPoint freeEndPoint = oFreeEndPoint.get();
    Type type = network.concreteInputType(freeEndPoint);
    List<Gate> suitableGates = gates.stream()
        .filter(g -> !NetworkUtils.compatibleOutputPorts(g, type).isEmpty())
        .toList();
    if (suitableGates.isEmpty()) {
      return Network.Addition.empty();
    }
    Gate newGate = suitableGates.get(rnd.nextInt(suitableGates.size()));
    int newGateIndex = freeGateIndex(network);
    List<Integer> pis = NetworkUtils.compatibleOutputPorts(newGate, type);
    Wire wire = new Wire(new Wire.EndPoint(newGateIndex, pis.get(rnd.nextInt(pis.size()))), freeEndPoint);
    return new Network.Addition(Map.of(newGateIndex, newGate), Set.of(wire));
  }

  protected static Network.Addition growOnOutputs(
      Network network,
      SequencedSet<Gate> gates,
      RandomGenerator rnd
  ) throws NetworkStructureException, TypeException {
    record WeightedEndPoint(Wire.EndPoint endPoint, int distance) {}
    Optional<Wire.EndPoint> oFreeEndPoint = network.freeOutputEndPoints()
        .stream()
        .map(ep -> new WeightedEndPoint(ep, network.inputDistanceFrom(Gate.InputGate.class, ep.gateIndex())))
        .min(Comparator.comparingInt(WeightedEndPoint::distance))
        .map(WeightedEndPoint::endPoint);
    if (oFreeEndPoint.isEmpty()) {
      return Network.Addition.empty();
    }
    Wire.EndPoint freeEndPoint = oFreeEndPoint.get();
    Type type = network.concreteOutputType(freeEndPoint);
    List<Gate> suitableGates = gates.stream()
        .filter(g -> !NetworkUtils.compatibleInputPorts(g, type).isEmpty())
        .toList();
    if (suitableGates.isEmpty()) {
      return Network.Addition.empty();
    }
    Gate newGate = suitableGates.get(rnd.nextInt(suitableGates.size()));
    int newGateIndex = network.gates().size();
    List<Integer> pis = NetworkUtils.compatibleInputPorts(newGate, type);
    Wire wire = new Wire(freeEndPoint, new Wire.EndPoint(network.gates().size(), pis.get(rnd.nextInt(pis.size()))));
    return new Network.Addition(Map.of(newGateIndex, newGate), Set.of(wire));
  }

  public static Network randomHoledNetwork(
      Network n,
      RandomGenerator rnd,
      int targetNOfGates
  ) throws NetworkStructureException, TypeException {
    List<Integer> toRemoveGis = new ArrayList<>();
    List<Integer> availableGis = n.gates()
        .entrySet()
        .stream()
        .filter(e -> !(e.getValue() instanceof Gate.InputGate || e.getValue() instanceof Gate.OutputGate))
        .map(Map.Entry::getKey)
        .toList();
    while (toRemoveGis.size() < targetNOfGates) {
      if (!toRemoveGis.isEmpty()) {
        availableGis = toRemoveGis.stream()
            .flatMap(
                gi -> Stream.concat(
                    n.wiresFrom(gi).stream().map(w -> w.dst().gateIndex()),
                    n.wiresTo(gi).stream().map(w -> w.src().gateIndex())
                )
            )
            .distinct()
            .filter(gi -> !toRemoveGis.contains(gi))
            .filter(
                gi -> !(n.gates().get(gi) instanceof Gate.InputGate) && !(n.gates()
                    .get(gi) instanceof Gate.OutputGate)
            )
            .toList();
      }
      if (availableGis.isEmpty()) {
        break;
      }
      toRemoveGis.add(availableGis.get(rnd.nextInt(availableGis.size())));
    }
    return new Network(
        n.gates()
            .entrySet()
            .stream()
            .filter(e -> !toRemoveGis.contains(e.getKey()))
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue
                )
            ),
        n.wires()
            .stream()
            .filter(w -> !toRemoveGis.contains(w.src().gateIndex()) && !toRemoveGis.contains(w.dst().gateIndex()))
            .collect(Collectors.toSet())
    );
  }

  public static Network randomSubnetwork(
      Network n,
      RandomGenerator rnd,
      int targetNOfGates
  ) throws NetworkStructureException, TypeException {
    List<Integer> toKeepGis = new ArrayList<>();
    List<Integer> availableGis = IntStream.range(0, n.gates().size())
        .boxed()
        .filter(gi -> !(n.gates().get(gi) instanceof Gate.InputGate) && !(n.gates().get(gi) instanceof Gate.OutputGate))
        .toList();
    while (toKeepGis.size() < targetNOfGates) {
      if (!toKeepGis.isEmpty()) {
        availableGis = toKeepGis.stream()
            .flatMap(
                gi -> Stream.concat(
                    n.wiresFrom(gi).stream().map(w -> w.dst().gateIndex()),
                    n.wiresTo(gi).stream().map(w -> w.src().gateIndex())
                )
            )
            .distinct()
            .filter(gi -> !toKeepGis.contains(gi))
            .filter(
                gi -> !(n.gates().get(gi) instanceof Gate.InputGate || n.gates()
                    .get(gi) instanceof Gate.OutputGate)
            )
            .toList();
      }
      if (availableGis.isEmpty()) {
        break;
      }
      toKeepGis.add(availableGis.get(rnd.nextInt(availableGis.size())));
    }
    return new Network(
        toKeepGis.stream().collect(Collectors.toMap(gi -> gi, gi -> n.gates().get(gi))),
        n.wires()
            .stream()
            .filter(w -> toKeepGis.contains(w.src().gateIndex()) && toKeepGis.contains(w.dst().gateIndex()))
            .collect(Collectors.toSet())
    );
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
                .anyMatch(ot -> iTypes.stream().anyMatch(ot::canTakeValuesOf))
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
    if (!oCompatibleGates.isEmpty()) {
      return oCompatibleGates;
    }
    return gates.stream().toList();
  }

  public static Network.Addition wire(
      Network n,
      boolean forceDifferentSubnets,
      RandomGenerator rnd
  ) throws NetworkStructureException, TypeException {
    enum IOType { I, O }
    record EnrichedEndPoint(int subnetIndex, int nOfWires, Type type, IOType ioType, Wire.EndPoint endPoint) {}
    List<EnrichedEndPoint> eeps = new ArrayList<>();
    List<List<Integer>> subnetsGis = n.disjointSubnetworksGateIndexes();
    for (int si = 0; si < subnetsGis.size(); si = si + 1) {
      for (int gi : subnetsGis.get(si)) {
        for (int pi = 0; pi < n.gates().get(gi).outputTypes().size(); pi = pi + 1) {
          Wire.EndPoint ep = new Wire.EndPoint(gi, pi);
          eeps.add(
              new EnrichedEndPoint(
                  si,
                  n.wiresFrom(gi, pi).size(),
                  n.concreteOutputType(ep),
                  IOType.O,
                  ep
              )
          );
        }
        for (int pi = 0; pi < n.gates().get(gi).inputPorts().size(); pi = pi + 1) {
          Wire.EndPoint ep = new Wire.EndPoint(gi, pi);
          eeps.add(
              new EnrichedEndPoint(
                  si,
                  n.wireTo(gi, pi).isEmpty() ? 0 : 1,
                  n.concreteInputType(ep),
                  IOType.I,
                  ep
              )
          );
        }
      }
    }
    Map<Type, List<EnrichedEndPoint>> iEeps = eeps.stream()
        .filter(eep -> eep.ioType == IOType.I && eep.nOfWires == 0)
        .collect(
            Collectors.groupingBy(
                eep -> eep.type,
                LinkedHashMap::new,
                Collectors.mapping(Function.identity(), Collectors.toList())
            )
        );
    Map<Type, List<EnrichedEndPoint>> oEeps = eeps.stream()
        .filter(eep -> eep.ioType == IOType.O)
        .collect(
            Collectors.groupingBy(
                eep -> eep.type,
                LinkedHashMap::new,
                Collectors.mapping(Function.identity(), Collectors.toList())
            )
        );
    // get common types
    Set<Type> types = Misc.intersection(iEeps.keySet(), oEeps.keySet());
    if (types.isEmpty()) {
      return Network.Addition.empty();
    }
    // wire one input
    for (Type type : types) {
      for (EnrichedEndPoint iEep : iEeps.get(type)) {
        List<EnrichedEndPoint> suitableOEeps = oEeps.get(type)
            .stream()
            .filter(oEep -> !forceDifferentSubnets || oEep.subnetIndex != iEep.subnetIndex)
            .toList();
        if (!suitableOEeps.isEmpty()) {
          EnrichedEndPoint oEep = Misc.pickRandomly(suitableOEeps, rnd);
          return new Network.Addition(Map.of(), Set.of(new Wire(oEep.endPoint, iEep.endPoint)));
        }
      }
    }
    return Network.Addition.empty();
  }

}
