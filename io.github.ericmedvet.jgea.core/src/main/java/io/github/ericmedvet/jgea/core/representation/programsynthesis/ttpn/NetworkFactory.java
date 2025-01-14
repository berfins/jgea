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
              Network newN = switch (gateAddition) {
                case GROW_IN -> NetworkUtils.growOnInputs(n, gates, rnd).applyTo(n);
                case GROW_OUT -> NetworkUtils.growOnOutputs(n, gates, rnd).applyTo(n);
                case LINK_IO -> NetworkUtils.growOnBoth(n, gates, rnd).applyTo(n);
              };
              if (!avoidDeadGates || NetworkUtils.deadComparator().compare(newN, n) <= 0) {
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
            Network newN = switch (wireAddition) {
              case INTER_SUBNETS -> NetworkUtils.wire(n, true, rnd).applyTo(n);
              case INTRA_SUBNETS -> NetworkUtils.wire(n, false, rnd).applyTo(n);
            };
            if (!avoidDeadGates || NetworkUtils.deadComparator().compare(newN, n) <= 0) {
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

}
