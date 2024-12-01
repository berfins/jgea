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

public record MultipleOutputParallelMultiplier(int n) implements BooleanFunction {
  @Override
  public int nOfInputs() {
    return 2 * n;
  }

  @Override
  public int nOfOutputs() {
    return 2 * n;
  }

  @Override
  public boolean[] apply(boolean[] inputs) {
    boolean[] a1 = new boolean[n];
    boolean[] a2 = new boolean[n];
    System.arraycopy(inputs, 0, a1, 0, n);
    System.arraycopy(inputs, n, a2, 0, n);
    int n1 = BooleanUtils.fromBinary(a1);
    int n2 = BooleanUtils.fromBinary(a2);
    return BooleanUtils.toBinary(n1 * n2, 2 * n);
  }

  @Override
  public String toString() {
    return "mopm(%d)".formatted(n);
  }
}
