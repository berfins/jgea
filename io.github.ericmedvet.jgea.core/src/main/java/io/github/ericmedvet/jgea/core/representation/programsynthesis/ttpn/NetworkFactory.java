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
  public Network build(RandomGenerator rnd) {
    try {
      Network n = new Network(
          Stream.concat(
              inputTypes.stream().map(type -> (Gate) Gate.input(type)),
              outputTypes.stream().map(Gate::output)
          ).toList(),
          Set.of()
      );
      while (n.gates().size() < maxNOfGates) {
        if (rnd.nextBoolean()) {
          n = growOnOutputs(n, rnd);
        } else {
          n = growOnInputs(n, rnd);
        }
        //n = n.wireFreeOutputEndPoints(ts -> rnd.nextInt(ts.size()));
        //n = n.wireFreeInputEndPoints(ts -> rnd.nextInt(ts.size()));
        System.out.println("=========");
        System.out.println(n);
        System.out.println("===");
        wireSubnetworks(n, rnd);
      }
      return n;
      //return NetworkUtils.grow(n, gates, rnd, maxNOfGates);
    } catch (NetworkStructureException | TypeException e) {
      throw new RuntimeException(e);
    }
  }

  private Network growOnOutputs(Network n, RandomGenerator rnd) throws NetworkStructureException, TypeException {
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
    System.out.printf("\tOUT NEW GATE: %d:%s\tNEW WIRE: %s%n", n.gates().size(), gate, wire);
    return new Network(newGates, newWires);
  }

  private Network growOnInputs(Network n, RandomGenerator rnd) throws NetworkStructureException, TypeException {
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
    System.out.printf("\tIN NEW GATE: %d:%s\tNEW WIRE: %s%n", n.gates().size(), gate, wire);
    return new Network(newGates, newWires);
  }

  private Network wireSubnetworks(Network n, RandomGenerator rnd) {
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
        .collect(Collectors.groupingBy(eep -> eep.type));
    Map<Type, List<EnrichedEndPoint>> oEeps = eeps.stream()
        .filter(eep -> eep.ioType == IOType.O)
        .collect(Collectors.groupingBy(eep -> eep.type));
    iEeps.forEach(
        (t, lEeps) -> System.out.printf(
            "input %s:%n\t%s%n",
            t,
            lEeps.stream()
                .map(EnrichedEndPoint::toString)
                .collect(
                    Collectors.joining("\n\t")
                )
        )
    );
    oEeps.forEach(
        (t, lEeps) -> System.out.printf(
            "output %s:%n\t%s%n",
            t,
            lEeps.stream()
                .map(EnrichedEndPoint::toString)
                .collect(
                    Collectors.joining("\n\t")
                )
        )
    );
    return n;
  }

}
