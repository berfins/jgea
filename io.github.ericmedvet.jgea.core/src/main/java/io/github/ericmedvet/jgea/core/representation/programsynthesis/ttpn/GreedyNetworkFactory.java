/*-
 * ========================LICENSE_START=================================
 * jgea-core
 * %%
 * Copyright (C) 2018 - 2025 Eric Medvet
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
import java.util.function.Function;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class GreedyNetworkFactory implements IndependentFactory<Network> {
  private final List<Type> inputTypes;
  private final List<Type> outputTypes;
  private final SequencedSet<Gate> gates;
  private final int maxNOfGates;
  private final int maxNOfAttempts;

  public GreedyNetworkFactory(
      List<Type> inputTypes,
      List<Type> outputTypes,
      SequencedSet<Gate> gates,
      int maxNOfGates,
      int maxNOfAttempts
  ) {
    this.inputTypes = inputTypes;
    this.outputTypes = outputTypes;
    this.gates = gates;
    this.maxNOfGates = maxNOfGates;
    this.maxNOfAttempts = maxNOfAttempts;
  }

  private record Step(Network network, Queue<Network.Addition> additions) {}

  private record WeightedAdditionProvider(Function<Network, List<Network.Addition>> provider, double weight) {}

  @Override
  public Network build(RandomGenerator random) {
    // build initial empty network
    Network network;
    try {
      network = new Network(
          Stream.concat(
              inputTypes.stream().map(type -> (Gate) Gate.input(type)),
              outputTypes.stream().map(Gate::output)
          ).toList(),
          Set.of()
      );
    } catch (NetworkStructureException | TypeException e) {
      throw new IllegalArgumentException("Cannot init network", e);
    }
    // do greedy building
    List<WeightedAdditionProvider> additionProviders = List.of(
        new WeightedAdditionProvider(GreedyNetworkFactory::wireAdditions, 10d),
        new WeightedAdditionProvider(n -> onInputGateAdditions(n, gates), 1d),
        new WeightedAdditionProvider(n -> onOutputGateAdditions(n, gates), 1d),
        new WeightedAdditionProvider(n -> inTheMiddleGateAdditions(n, gates), 5d)
    );
    List<Step> steps = new LinkedList<>();
    steps.addLast(computeStep(network, additionProviders, random));
    int nOfAttempts = 0;
    while (nOfAttempts < maxNOfAttempts) {
      Step step = steps.getLast();
      if (step.additions.isEmpty()) {
        steps.removeLast();
        if (steps.isEmpty()) {
          break;
        }
      } else {
        Network.Addition addition = step.additions.remove();
        try {
          Network newNetwork = addition.applyTo(step.network);
          if (newNetwork.deadOrIUnwiredOutputGates().isEmpty()) {
            return newNetwork;
          }
          if (newNetwork.gates().size() <= maxNOfGates) {
            steps.addLast(computeStep(newNetwork, additionProviders, random));
          }
        } catch (NetworkStructureException | TypeException e) {
          // ignore (should happen rarely...)
        }
        nOfAttempts = nOfAttempts + 1;
      }
    }
    return steps.isEmpty() ? network : steps.getLast().network();
  }

  private static Step computeStep(
      Network network,
      List<WeightedAdditionProvider> providers,
      RandomGenerator random
  ) {
    record WeightedAddition(Network.Addition addition, double weight) {}
    List<WeightedAddition> weightedAdditions = providers.stream()
        .flatMap(
            wProvider -> wProvider.provider.apply(network)
                .stream()
                .map(addition -> new WeightedAddition(addition, wProvider.weight))
        )
        .toList();
    int[] indexes = IntStream.range(0, weightedAdditions.size()).toArray();
    // random shuffle
    IntStream.range(0, indexes.length)
        .forEach(
            i -> swap(
                indexes,
                random.nextInt(indexes.length),
                random.nextInt(indexes.length)
            )
        );
    // weighted shuffle
    IntStream.range(0, indexes.length).forEach(i -> {
      int i1 = random.nextInt(indexes.length - 1);
      int i2 = random.nextInt(i1, indexes.length - 1);
      double w1 = weightedAdditions.get(i1).weight;
      double w2 = weightedAdditions.get(i2).weight;
      double x = random.nextDouble(w1 + w2);
      if (x > w1) {
        swap(indexes, i1, i2);
      }
    });
    return new Step(
        network,
        Arrays.stream(indexes)
            .mapToObj(i -> weightedAdditions.get(i).addition)
            .collect(Collectors.toCollection(LinkedList::new))
    );
  }

  private static void swap(int[] array, int i, int j) {
    int v = array[i];
    array[i] = array[j];
    array[j] = v;
  }

  private static List<Network.Addition> wireAdditions(Network network) {
    List<Network.Addition> additions = new ArrayList<>();
    for (Wire.EndPoint iEp : network.freeInputEndPoints()) {
      Type iType = network.concreteInputType(iEp);
      for (Map.Entry<Integer, Gate> gateEntry : network.gates().entrySet()) {
        for (int pi = 0; pi < gateEntry.getValue().outputTypes().size(); pi = pi + 1) {
          if (iType.canTakeValuesOf(network.concreteOutputType(new Wire.EndPoint(gateEntry.getKey(), pi)))) {
            additions.add(
                new Network.Addition(
                    Map.of(),
                    Set.of(new Wire(new Wire.EndPoint(gateEntry.getKey(), pi), iEp))
                )
            );
          }
        }
      }
    }
    return additions;
  }

  private static List<Network.Addition> inTheMiddleGateAdditions(Network network, SequencedSet<Gate> gates) {
    int newGateIndex = NetworkUtils.freeGateIndex(network);
    List<Network.Addition> additions = new ArrayList<>();
    for (Wire.EndPoint dstEp : network.freeInputEndPoints()) {
      Type dstType = network.concreteInputType(dstEp);
      for (Map.Entry<Integer, Gate> srcGateEntry : network.gates().entrySet()) {
        for (int srcOpi = 0; srcOpi < srcGateEntry.getValue().outputTypes().size(); srcOpi = srcOpi + 1) {
          Type srcType = network.concreteOutputType(new Wire.EndPoint(srcGateEntry.getKey(), srcOpi));
          for (Gate newGate : gates) {
            for (int ipi = 0; ipi < newGate.inputPorts().size(); ipi = ipi + 1) {
              for (int opi = 0; opi < newGate.outputTypes().size(); opi = opi + 1) {
                if (newGate.inputPorts().get(ipi).type().canTakeValuesOf(srcType) && dstType.canTakeValuesOf(
                    newGate.outputTypes().get(opi)
                )) {
                  additions.add(
                      new Network.Addition(
                          Map.of(newGateIndex, newGate),
                          Set.of(
                              Wire.of(srcGateEntry.getKey(), srcOpi, newGateIndex, ipi),
                              Wire.of(newGateIndex, opi, dstEp.gateIndex(), dstEp.portIndex())
                          )
                      )
                  );
                }
              }
            }
          }
        }
      }
    }
    return additions;
  }

  private static List<Network.Addition> onOutputGateAdditions(Network network, SequencedSet<Gate> gates) {
    int newGateIndex = NetworkUtils.freeGateIndex(network);
    List<Network.Addition> additions = new ArrayList<>();
    for (Map.Entry<Integer, Gate> gateEntry : network.gates().entrySet()) {
      for (int pi = 0; pi < gateEntry.getValue().outputTypes().size(); pi = pi + 1) {
        Type oType = network.concreteOutputType(new Wire.EndPoint(gateEntry.getKey(), pi));
        for (Gate newGate : gates) {
          for (int gpi = 0; gpi < newGate.inputPorts().size(); gpi = gpi + 1) {
            if (newGate.inputPorts().get(gpi).type().canTakeValuesOf(oType)) {
              additions.add(
                  new Network.Addition(
                      Map.of(newGateIndex, newGate),
                      Set.of(Wire.of(gateEntry.getKey(), pi, newGateIndex, gpi))
                  )
              );
            }
          }
        }
      }
    }
    return additions;
  }

  private static List<Network.Addition> onInputGateAdditions(Network network, SequencedSet<Gate> gates) {
    int newGateIndex = NetworkUtils.freeGateIndex(network);
    List<Network.Addition> additions = new ArrayList<>();
    for (Wire.EndPoint iEp : network.freeInputEndPoints()) {
      Type iType = network.concreteInputType(iEp);
      for (Gate gate : gates) {
        for (int gpi = 0; gpi < gate.outputTypes().size(); gpi = gpi + 1) {
          if (iType.canTakeValuesOf(gate.outputTypes().get(gpi))) {
            additions.add(
                new Network.Addition(
                    Map.of(newGateIndex, gate),
                    Set.of(Wire.of(newGateIndex, gpi, iEp.gateIndex(), iEp.portIndex()))
                )
            );
          }
        }
      }
    }
    return additions;
  }

}
