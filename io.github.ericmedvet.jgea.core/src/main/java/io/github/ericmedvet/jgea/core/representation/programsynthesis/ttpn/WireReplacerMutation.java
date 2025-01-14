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

import io.github.ericmedvet.jgea.core.operator.Mutation;
import io.github.ericmedvet.jgea.core.representation.programsynthesis.type.TypeException;
import io.github.ericmedvet.jgea.core.util.Misc;
import java.util.HashSet;
import java.util.SequencedSet;
import java.util.Set;
import java.util.random.RandomGenerator;

// TODO add a wire changer mutation and a gate remover mutation; fix crossover
public class WireReplacerMutation implements Mutation<Network> {
  private final SequencedSet<Gate> gates;
  private final int maxNOfGates;
  private final int maxNOfAttempts;
  private final boolean avoidDeadGates;

  public WireReplacerMutation(
      SequencedSet<Gate> gates,
      int maxNOfGates,
      int maxNOfAttempts,
      boolean avoidDeadGates
  ) {
    this.gates = gates;
    this.maxNOfGates = maxNOfGates;
    this.maxNOfAttempts = maxNOfAttempts;
    this.avoidDeadGates = avoidDeadGates;
  }

  @Override
  public Network mutate(Network n, RandomGenerator rnd) {
    if (n.gates().size() >= maxNOfGates || n.wires().isEmpty()) {
      return n;
    }
    int nOfAttempts = 0;
    while (nOfAttempts < maxNOfAttempts) {
      // cut one wire
      Wire toRemoveWire = Misc.pickRandomly(n.wires(), rnd);
      Set<Wire> newWires = new HashSet<>(n.wires());
      newWires.remove(toRemoveWire);
      try {
        Network newN = new Network(n.gates(), newWires);
        newN = NetworkUtils.growOnEndPoints(newN, toRemoveWire.src(), toRemoveWire.dst(), gates, rnd)
            .applyTo(newN);
        while (true) {
          Network.Addition addition = NetworkUtils.wire(newN, false, rnd);
          if (addition.isEmpty()) {
            break;
          }
          newN = addition.applyTo(newN);
        }
        if (!avoidDeadGates || NetworkUtils.deadComparator().compare(newN, n) <= 0) {
          return newN;
        }
        nOfAttempts = nOfAttempts + 1;
      } catch (NetworkStructureException | TypeException e) {
        nOfAttempts = nOfAttempts + 1;
      }
    }
    return n;
  }
}
