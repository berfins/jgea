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
import io.github.ericmedvet.jsdynsym.core.numerical.UnivariateRealFunction;

import java.util.List;
import java.util.Random;

public class Vladislavleva4 extends PrecomputedSyntheticURProblem {

  // aka: UBall5D,
  // https://www.researchgate.net/profile/Ekaterina_Katya_Vladislavleva/publication/224330345_Order_of_Nonlinearity_as_a_Complexity_Measure_for_Models_Generated_by_Symbolic_Regression_via_Pareto_Genetic_Programming/links/00b7d5306967756b1d000000.pdf
  public Vladislavleva4(List<Metric> metrics, long seed) {
    super(
        SyntheticURProblem.function(
            v -> {
              double s = 0;
              for (int i = 0; i < 5; i++) {
                s = s + (v[i] - 3d) * (v[i] - 3d);
              }
              return 10d / (5d + s);
            },
            5
        ),
        SyntheticURProblem.tupleProvider(MathUtils.pairwise(
            MathUtils.uniformSample(0.05, 6.05, 1024, new Random(seed)),
            MathUtils.uniformSample(0.05, 6.05, 1024, new Random(seed + 1)),
            MathUtils.uniformSample(0.05, 6.05, 1024, new Random(seed + 2)),
            MathUtils.uniformSample(0.05, 6.05, 1024, new Random(seed + 3)),
            MathUtils.uniformSample(0.05, 6.05, 1024, new Random(seed + 4))
        )),
        SyntheticURProblem.tupleProvider(MathUtils.pairwise(
            MathUtils.uniformSample(-0.25, 6.35, 5000, new Random(seed)),
            MathUtils.uniformSample(-0.25, 6.35, 5000, new Random(seed + 1)),
            MathUtils.uniformSample(-0.25, 6.35, 5000, new Random(seed + 2)),
            MathUtils.uniformSample(-0.25, 6.35, 5000, new Random(seed + 3)),
            MathUtils.uniformSample(-0.25, 6.35, 5000, new Random(seed + 4))
        )),
        metrics
    );
  }
}
