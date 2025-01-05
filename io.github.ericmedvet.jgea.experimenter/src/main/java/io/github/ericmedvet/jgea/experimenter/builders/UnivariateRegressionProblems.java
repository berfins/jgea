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

import io.github.ericmedvet.jgea.core.util.IndexedProvider;
import io.github.ericmedvet.jgea.problem.regression.univariate.UnivariateRegressionFitness;
import io.github.ericmedvet.jgea.problem.regression.univariate.UnivariateRegressionProblem;
import io.github.ericmedvet.jgea.problem.regression.univariate.synthetic.*;
import io.github.ericmedvet.jnb.core.*;

import java.util.List;
import java.util.Map;

@Discoverable(prefixTemplate = "ea.problem|p.univariateRegression|ur")
public class UnivariateRegressionProblems {
  private UnivariateRegressionProblems() {
  }

  @Alias(
      name = "bundled", passThroughParams = {@PassThroughParam(name = "name", type = ParamMap.Type.STRING), @PassThroughParam(name = "xScaling", value = "none", type = ParamMap.Type.STRING), @PassThroughParam(name = "yScaling", value = "none", type = ParamMap.Type.STRING)
      }, value = // spotless:off
      """
          fromData(provider = ea.provider.num.fromBundled(name = $name; xScaling = $xScaling; yScaling = $yScaling))
          """) // spotless:on
  @Cacheable
  public static UnivariateRegressionProblem fromData(
      @Param("provider") IndexedProvider<ExampleBasedFitness.Example<Map<String, Double>, Map<String, Double>>> provider,
      @Param(value = "metric", dS = "mse") UnivariateRegressionFitness.Metric metric,
      @Param(value = "nFolds", dI = 10) int nFolds,
      @Param(value = "testFold", dI = 0) int testFold
  ) {
    return UnivariateRegressionProblem.from(
        metric,
        provider.negatedFold(testFold, nFolds),
        provider.fold(testFold, nFolds),
        provider.first()
            .output()
            .keySet()
            .stream()
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No output y var in datates"))
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
      case "keijzer6" -> new Keijzer6(List.of(metric));
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
