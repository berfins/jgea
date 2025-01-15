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
import io.github.ericmedvet.jgea.core.representation.programsynthesis.type.TypeException;

import java.util.random.RandomGenerator;

public class NetworkCrossover implements Crossover<Network> {
  private final int maxNOfGates;
  private final double subnetSizeRate;

  public NetworkCrossover(int maxNOfGates, double subnetSizeRate) {
    this.maxNOfGates = maxNOfGates;
    this.subnetSizeRate = subnetSizeRate;
  }

  @Override
  public Network recombine(Network n1, Network n2, RandomGenerator rnd) {
    try {
      int innerSize1 = (int) n1.gates()
          .values()
          .stream()
          .filter(
              g -> Gate.InputGate.class.isAssignableFrom(g.getClass()) || Gate.OutputGate.class.isAssignableFrom(
                  g.getClass()
              )
          )
          .count();
      int innerSize2 = (int) n2.gates()
          .values()
          .stream()
          .filter(
              g -> Gate.InputGate.class.isAssignableFrom(g.getClass()) || Gate.OutputGate.class.isAssignableFrom(
                  g.getClass()
              )
          )
          .count();
      Network hn1 = NetworkUtils.randomHoledNetwork(n1, rnd, (int) Math.max(1, innerSize1 * subnetSizeRate));
      Network sn2 = NetworkUtils.randomSubnetwork(
          n2,
          rnd,
          Math.min((int) Math.max(1, innerSize2 * subnetSizeRate), maxNOfGates - hn1.gates().size())
      );
      return hn1.mergedWith(sn2);
    } catch (NetworkStructureException | TypeException e) {
      return n1;
    }
  }
}
