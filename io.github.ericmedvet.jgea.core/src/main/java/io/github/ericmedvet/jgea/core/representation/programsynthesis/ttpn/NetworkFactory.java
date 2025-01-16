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

import java.util.List;
import java.util.SequencedSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.random.RandomGenerator;
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
    int targetNOfGates = rnd.nextInt(network.gates().size() + 1, maxNOfGates);
    while (network.gates().size() < targetNOfGates) {

    }
    while (nOfAttempts < maxNOfAttempts && network.gates().size() < maxNOfGates) {
      // add gate
      Network.Addition addition;
      if (network.gates().size() < targetNOfGates) {
        addition = rnd.nextBoolean() ? NetworkUtils.growOnInputs(network, gates, rnd) : NetworkUtils.growOnOutputs(
            network,
            gates,
            rnd
        );
      } else {
        List<Network.Addition> additions = NetworkUtils.growBothAdditions(network, gates);
        for (Network.Addition bAddition : additions) {
          if (addition.isEmpty()) {
            addition = rnd.nextBoolean() ? NetworkUtils.growOnInputs(network, gates, rnd) : NetworkUtils.growOnOutputs(
                network,
                gates,
                rnd
            );
          }
        }
      }
      if (!addition.isEmpty()) {
        try {
          network = addition.applyTo(network);
          consumer.accept(network);
          System.out.println(addition);
        } catch (NetworkStructureException | TypeException e) {
          nOfAttempts = nOfAttempts + 1;
        }
      }
    }
    // final wiring
    return NetworkUtils.wireAll(network, avoidDeadGates, rnd);
  }

}
