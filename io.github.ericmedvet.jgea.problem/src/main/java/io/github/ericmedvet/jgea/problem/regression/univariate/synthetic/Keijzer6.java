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

package io.github.ericmedvet.jgea.problem.regression.univariate.synthetic;

import io.github.ericmedvet.jgea.problem.regression.MathUtils;
import java.util.List;

public class Keijzer6 extends PrecomputedSyntheticURProblem {

  public Keijzer6(List<Metric> metrics) {
    super(
        SyntheticURProblem.function(
            v -> {
              double s = 0d;
              for (double i = 1; i < v[0]; i++) {
                s = s + 1d / i;
              }
              return s;
            },
            1
        ),
        SyntheticURProblem.tupleProvider(MathUtils.pairwise(MathUtils.equispacedValues(1, 50, 1))),
        SyntheticURProblem.tupleProvider(MathUtils.pairwise(MathUtils.equispacedValues(1, 120, 1))),
        metrics
    );
  }
}
