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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedSet;
import java.util.random.RandomGenerator;

public class NetworkFactory implements IndependentFactory<Network> {
  private final List<Type> inputTypes;
  private final List<Type> outputTypes;
  private final SequencedSet<Gate> gates;
  private final int maxNOfGates;
  private final Map<Signature, Map<Integer, List<Network>>> partialNetworks;

  public NetworkFactory(List<Type> inputTypes, List<Type> outputTypes, SequencedSet<Gate> gates, int maxNOfGates) {
    this.inputTypes = inputTypes;
    this.outputTypes = outputTypes;
    this.gates = gates;
    this.maxNOfGates = maxNOfGates;
    partialNetworks = new LinkedHashMap<>();
    // iteratively populate map
  }

  private record Signature(List<Type> inputTypes, List<Type> outputTypes) {}

  @Override
  public Network build(RandomGenerator random) {
    // build input gate
    // start connecting gates
    return null;
  }

  private Network buildRandomNetwork(RandomGenerator rnd) {
    // take one gate
    // iteratively add compatible gates
    return null;
  }

}
