/*-
 * ========================LICENSE_START=================================
 * jgea-problem
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
package io.github.ericmedvet.jgea.problem.booleanfunction.synthetic;

import io.github.ericmedvet.jgea.core.util.IndexedProvider;
import io.github.ericmedvet.jgea.problem.booleanfunction.BooleanFunction;
import io.github.ericmedvet.jgea.problem.booleanfunction.BooleanUtils;
import io.github.ericmedvet.jsdynsym.core.numerical.MultivariateRealFunction;
import java.util.List;
import java.util.stream.IntStream;

public class EvenParity extends PrecomputedSyntheticBRProblem {
  public EvenParity(List<Metric> metrics, int n) {
    super(
        BooleanFunction.from(
            inputs -> new boolean[]{IntStream.range(0, inputs.length).map(i -> inputs[i] ? 1 : 0).sum() % 2 == 0},
            n,
            1
        ),
        IndexedProvider.from(
            BooleanUtils.buildCompleteObservations(MultivariateRealFunction.varNames("x", n).toArray(String[]::new))
        ),
        IndexedProvider.from(
            BooleanUtils.buildCompleteObservations(MultivariateRealFunction.varNames("x", n).toArray(String[]::new))
        ),
        metrics
    );
  }
}
