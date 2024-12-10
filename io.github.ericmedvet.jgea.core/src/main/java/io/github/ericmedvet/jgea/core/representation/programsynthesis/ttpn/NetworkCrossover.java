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

import io.github.ericmedvet.jgea.core.operator.Crossover;
import java.util.SequencedSet;
import java.util.random.RandomGenerator;

public class NetworkCrossover implements Crossover<Network> {
  private final SequencedSet<Gate> gates;
  private final int maxNOfGates;

  public NetworkCrossover(SequencedSet<Gate> gates, int maxNOfGates) {
    this.gates = gates;
    this.maxNOfGates = maxNOfGates;
  }

  @Override
  public Network recombine(Network n1, Network n2, RandomGenerator rnd) {
    return null;
  }
}
