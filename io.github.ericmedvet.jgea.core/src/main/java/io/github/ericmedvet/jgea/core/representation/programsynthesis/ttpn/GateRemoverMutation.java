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

import io.github.ericmedvet.jgea.core.operator.Mutation;
import java.util.random.RandomGenerator;

public class GateRemoverMutation implements Mutation<Network> {

  private final int maxNOfAttempts;
  private final boolean avoidDeadGates;

  public GateRemoverMutation(int maxNOfAttempts, boolean avoidDeadGates) {
    this.maxNOfAttempts = maxNOfAttempts;
    this.avoidDeadGates = avoidDeadGates;
  }

  @Override
  public Network mutate(Network network, RandomGenerator random) {
    return null;
  }
}
