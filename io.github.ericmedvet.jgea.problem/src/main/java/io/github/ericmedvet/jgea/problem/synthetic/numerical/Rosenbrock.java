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
package io.github.ericmedvet.jgea.problem.synthetic.numerical;

import java.util.stream.IntStream;

public class Rosenbrock extends AbstractNumericalProblem {
  public Rosenbrock(int p) {
    super(
        p,
        vs -> IntStream.range(0, p - 1)
            .mapToDouble(
                i -> 100 * Math.pow(vs.get(i) * vs.get(i) - vs.get(i + 1), 2) + (vs.get(i) - 1) * (vs.get(i) - 1)
            )
            .sum()
    );
  }
}
