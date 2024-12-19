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

public class NetworkMutation implements Mutation<Network> {
  private final SequencedSet<Gate> gates;
  private final int maxNOfGates;

  public NetworkMutation(SequencedSet<Gate> gates, int maxNOfGates) {
    this.gates = gates;
    this.maxNOfGates = maxNOfGates;
  }

  @Override
  public Network mutate(Network n, RandomGenerator rnd) {
    if (n.gates().size() >= maxNOfGates) {
      return n;
    }
    // cut one wire
    Set<Wire> newWires = new HashSet<>(n.wires());
    if (!newWires.isEmpty()) {
      newWires.remove(Misc.pickRandomly(newWires, rnd));
    }
    try {
      n = new Network(n.gates(), newWires);
    } catch (NetworkStructureException | TypeException e) {
      return n;
    }
    // attempt adding gates
    return NetworkUtils.grow(n, gates, rnd, maxNOfGates);
  }
}
