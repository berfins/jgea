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
import io.github.ericmedvet.jgea.core.util.Misc;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class NetworkFactory implements IndependentFactory<Network> {
  private final List<Type> inputTypes;
  private final List<Type> outputTypes;
  private final SequencedSet<Gate> gates;
  private final int maxNOfGates;
  private final int maxNOfAttempts;
  private final boolean avoidDeadGates;

  public NetworkFactory(
      List<Type> inputTypes,
      List<Type> outputTypes,
      SequencedSet<Gate> gates,
      int maxNOfGates,
      int maxNOfAttempts,
      boolean avoidDeadGates
  ) {
    this.inputTypes = inputTypes;
    this.outputTypes = outputTypes;
    this.gates = gates;
    this.maxNOfGates = maxNOfGates;
    this.maxNOfAttempts = maxNOfAttempts;
    this.avoidDeadGates = avoidDeadGates;
  }

  @Override
  public Network build(RandomGenerator rnd) {
    return build(rnd, n -> {});
  }

  public Network build(RandomGenerator rnd, Consumer<Network> consumer) {
    enum GateAddition { GROW_IN, GROW_OUT, LINK_IO }
    enum WireAddition { INTRA_SUBNETS, INTER_SUBNETS }
    List<GateAddition> gateAdditions = new ArrayList<>(Arrays.stream(GateAddition.values()).toList());
    List<WireAddition> wireAdditions = new ArrayList<>(Arrays.stream(WireAddition.values()).toList());
    int nOfAttempts = 0;
    try {
      Network n = new Network(
          Stream.concat(
              inputTypes.stream().map(type -> (Gate) Gate.input(type)),
              outputTypes.stream().map(Gate::output)
          ).toList(),
          Set.of()
      );
      int targetNOfGates = rnd.nextInt(n.gates().size(), maxNOfGates);
      while (true) {
        try {
          consumer.accept(n);
          int nOfGates = n.gates().size();
          if (n.gates().size() < targetNOfGates) {
            Collections.shuffle(gateAdditions, rnd);
            for (GateAddition gateAddition : gateAdditions) {
              int nOfDeadsBefore = n.deadGates().size();
              Network newN = switch (gateAddition) {
                case GROW_IN -> growOnInputs(n, gates, rnd);
                case GROW_OUT -> growOnOutputs(n, gates, rnd);
                case LINK_IO -> growOnBoth(n, gates, rnd);
              };
              int nOfDeadsAfter = newN.deadGates().size();
              if (!avoidDeadGates || nOfDeadsAfter <= nOfDeadsBefore) {
                n = newN;
              }
              if (n.gates().size() != nOfGates) {
                break;
              }
            }
          }
          int nOfWires = n.wires().size();
          Collections.shuffle(wireAdditions, rnd);
          for (WireAddition wireAddition : wireAdditions) {
            int nOfDeadsBefore = n.deadGates().size();
            Network newN = switch (wireAddition) {
              case INTER_SUBNETS -> wire(n, true, rnd);
              case INTRA_SUBNETS -> wire(n, false, rnd);
            };
            int nOfDeadsAfter = newN.deadGates().size();
            if (!avoidDeadGates || nOfDeadsAfter <= nOfDeadsBefore) {
              n = newN;
            }
            if (n.wires().size() != nOfWires) {
              break;
            }
          }
          if (nOfGates == n.gates().size() && nOfWires == n.wires().size()) {
            nOfAttempts = nOfAttempts + 1;
            if (nOfAttempts > maxNOfAttempts) {
              break;
            }
          }
        } catch (NetworkStructureException | TypeException e) {
          nOfAttempts = nOfAttempts + 1;
          if (nOfAttempts > maxNOfAttempts) {
            return n;
          }
        }
      }
      return n;
    } catch (NetworkStructureException | TypeException e) {
      throw new RuntimeException(e);
    }
  }

  protected static Network growOnBoth(
      Network n,
      SequencedSet<Gate> gates,
      RandomGenerator rnd
  ) throws NetworkStructureException, TypeException {
    List<Wire.EndPoint> iEps = new ArrayList<>(n.freeInputEndPoints());
    List<Wire.EndPoint> oEps = new ArrayList<>(n.freeOutputEndPoints());
    List<Gate> shuffledGates = new ArrayList<>(gates);
    Collections.shuffle(iEps, rnd);
    Collections.shuffle(oEps, rnd);
    Collections.shuffle(shuffledGates, rnd);
    for (Wire.EndPoint iEp : iEps) {
      Type iType = n.concreteInputType(iEp);
      for (Wire.EndPoint oEp : oEps) {
        Type oType = n.concreteOutputType(oEp);
        for (Gate gate : shuffledGates) {
          List<Integer> suitableIps = IntStream.range(0, gate.inputPorts().size())
              .filter(
                  pi -> gate.inputPorts()
                      .get(pi)
                      .type()
                      .canTakeValuesOf(oType)
              )
              .boxed()
              .toList();
          List<Integer> suitableOps = IntStream.range(0, gate.outputTypes().size())
              .filter(pi -> iType.canTakeValuesOf(gate.outputTypes().get(pi)))
              .boxed()
              .toList();
          if (!suitableIps.isEmpty() && !suitableOps.isEmpty()) {
            int newGateIndex = n.gates().size();
            Wire inWire = new Wire(oEp, new Wire.EndPoint(newGateIndex, Misc.pickRandomly(suitableIps, rnd)));
            Wire outWire = new Wire(new Wire.EndPoint(newGateIndex, Misc.pickRandomly(suitableOps, rnd)), iEp);
            List<Gate> newGates = new ArrayList<>(n.gates());
            Set<Wire> newWires = new LinkedHashSet<>(n.wires());
            newGates.add(gate);
            newWires.add(inWire);
            newWires.add(outWire);
            return new Network(newGates, newWires);
          }
        }
      }
    }
    return n;
  }

  protected static Network growOnInputs(
      Network n,
      SequencedSet<Gate> gates,
      RandomGenerator rnd
  ) throws NetworkStructureException, TypeException {
    record WeightedEndPoint(Wire.EndPoint endPoint, int distance) {}
    Optional<Wire.EndPoint> oFreeEndPoint = n.freeInputEndPoints()
        .stream()
        .map(ep -> new WeightedEndPoint(ep, n.outputDistanceFrom(Gate.OutputGate.class, ep.gateIndex())))
        .min(Comparator.comparingInt(WeightedEndPoint::distance))
        .map(WeightedEndPoint::endPoint);
    if (oFreeEndPoint.isEmpty()) {
      return n;
    }
    Wire.EndPoint freeEndPoint = oFreeEndPoint.get();
    Type type = n.concreteInputType(freeEndPoint);
    List<Gate> suitableGates = gates.stream()
        .filter(g -> !NetworkUtils.compatibleOutputPorts(g, type).isEmpty())
        .toList();
    if (suitableGates.isEmpty()) {
      return n;
    }
    Gate gate = suitableGates.get(rnd.nextInt(suitableGates.size()));
    List<Integer> pis = NetworkUtils.compatibleOutputPorts(gate, type);
    Wire wire = new Wire(new Wire.EndPoint(n.gates().size(), pis.get(rnd.nextInt(pis.size()))), freeEndPoint);
    List<Gate> newGates = new ArrayList<>(n.gates());
    Set<Wire> newWires = new LinkedHashSet<>(n.wires());
    newGates.add(gate);
    newWires.add(wire);
    return new Network(newGates, newWires);
  }

  protected static Network growOnOutputs(
      Network n,
      SequencedSet<Gate> gates,
      RandomGenerator rnd
  ) throws NetworkStructureException, TypeException {
    record WeightedEndPoint(Wire.EndPoint endPoint, int distance) {}
    Optional<Wire.EndPoint> oFreeEndPoint = n.freeOutputEndPoints()
        .stream()
        .map(ep -> new WeightedEndPoint(ep, n.inputDistanceFrom(Gate.InputGate.class, ep.gateIndex())))
        .min(Comparator.comparingInt(WeightedEndPoint::distance))
        .map(WeightedEndPoint::endPoint);
    if (oFreeEndPoint.isEmpty()) {
      return n;
    }
    Wire.EndPoint freeEndPoint = oFreeEndPoint.get();
    Type type = n.concreteOutputType(freeEndPoint);
    List<Gate> suitableGates = gates.stream()
        .filter(g -> !NetworkUtils.compatibleInputPorts(g, type).isEmpty())
        .toList();
    if (suitableGates.isEmpty()) {
      return n;
    }
    Gate gate = suitableGates.get(rnd.nextInt(suitableGates.size()));
    List<Integer> pis = NetworkUtils.compatibleInputPorts(gate, type);
    Wire wire = new Wire(freeEndPoint, new Wire.EndPoint(n.gates().size(), pis.get(rnd.nextInt(pis.size()))));
    List<Gate> newGates = new ArrayList<>(n.gates());
    Set<Wire> newWires = new LinkedHashSet<>(n.wires());
    newGates.add(gate);
    newWires.add(wire);
    return new Network(newGates, newWires);
  }

  private Network wire(
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
      return n;
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
          Set<Wire> newWires = new LinkedHashSet<>(n.wires());
          newWires.add(new Wire(oEep.endPoint, iEep.endPoint));
          return new Network(
              n.gates(),
              newWires
          );
        }
      }
    }
    return n;
  }

}
