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

import io.github.ericmedvet.jgea.problem.regression.univariate.UnivariateRegressionProblemOLD;
import io.github.ericmedvet.jsdynsym.core.numerical.UnivariateRealFunction;
import java.util.List;

public class SyntheticUnivariateRegressionProblem extends UnivariateRegressionProblemOLD<SyntheticUnivariateRegressionFitness> {

  private final UnivariateRealFunction targetFunction;

  public SyntheticUnivariateRegressionProblem(
      UnivariateRealFunction targetFunction,
      List<double[]> trainingPoints,
      List<double[]> validationPoints,
      UnivariateRegressionFitnessOLD.Metric metric
  ) {
    super(
        new SyntheticUnivariateRegressionFitness(targetFunction, trainingPoints, metric),
        new SyntheticUnivariateRegressionFitness(targetFunction, validationPoints, metric)
    );
    this.targetFunction = targetFunction;
  }

  public UnivariateRealFunction getTargetFunction() {
    return targetFunction;
  }
}
