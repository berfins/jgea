/*-
 * ========================LICENSE_START=================================
 * jgea-experimenter
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

package io.github.ericmedvet.jgea.experimenter.builders;

import io.github.ericmedvet.jgea.core.fitness.ExampleBasedFitness;
import io.github.ericmedvet.jgea.core.util.IndexedProvider;
import io.github.ericmedvet.jgea.problem.regression.multivariate.MultivariateRegressionProblem;
import io.github.ericmedvet.jgea.problem.regression.univariate.UnivariateRegressionFitness;
import io.github.ericmedvet.jgea.problem.regression.univariate.UnivariateRegressionProblem;
import io.github.ericmedvet.jgea.problem.regression.univariate.synthetic.*;
import io.github.ericmedvet.jnb.core.Cacheable;
import io.github.ericmedvet.jnb.core.Discoverable;
import io.github.ericmedvet.jnb.core.Param;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Discoverable(prefixTemplate = "ea.problem|p.univariateRegression|ur")
public class UnivariateRegressionProblems {
  private UnivariateRegressionProblems() {
  }

  @Cacheable
  public static UnivariateRegressionProblem fromData(
      @Param("provider") IndexedProvider<ExampleBasedFitness.Example<Map<String, Double>, Map<String, Double>>> provider,
      @Param("string") String yVarName,
      @Param(value = "metric", dS = "mse") UnivariateRegressionFitness.Metric metric,
      @Param(value = "nFolds", dI = 10) int nFolds,
      @Param(value = "testFold", dI = 0) int testFold
  ) {
    return UnivariateRegressionProblem.from(
        metric,
        provider.negatedFold(testFold, nFolds),
        provider.fold(testFold, nFolds),
        yVarName
    );
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static SyntheticURP synthetic(
      @Param("name") String name,
      @Param(value = "metric", dS = "mse") UnivariateRegressionFitness.Metric metric,
      @Param(value = "seed", dI = 1) int seed
  ) {
    return switch (name) {
      case "keijzer6" -> new Keijzer6(metric);
      case "nguyen7" -> new Nguyen7(metric, seed);
      case "pagie1" -> new Pagie1(metric);
      case "polynomial4" -> new Polynomial4(metric);
      case "vladislavleva4" -> new Vladislavleva4(metric, seed);
      case "korns12" -> new Korns12(metric, seed);
      case "xor" -> new Xor(metric);
      default -> throw new IllegalArgumentException("Unknown synthetic function: %s".formatted(name));
    };
  }
}
