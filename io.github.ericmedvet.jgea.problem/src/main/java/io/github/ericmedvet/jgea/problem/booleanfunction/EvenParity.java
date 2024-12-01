/*-
 * ========================LICENSE_START=================================
 * jgea-problem
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
package io.github.ericmedvet.jgea.problem.booleanfunction;

import java.util.stream.IntStream;

public record EvenParity(int n) implements BooleanFunction {
  @Override
  public int nOfInputs() {
    return n;
  }

  @Override
  public int nOfOutputs() {
    return 1;
  }

  @Override
  public boolean[] apply(boolean[] inputs) {
    return new boolean[]{IntStream.range(0, inputs.length).map(i -> inputs[i] ? 1 : 0).sum() % 2 == 0};
  }

  @Override
  public String toString() {
    return "evenParity(%d)".formatted(n);
  }
}
