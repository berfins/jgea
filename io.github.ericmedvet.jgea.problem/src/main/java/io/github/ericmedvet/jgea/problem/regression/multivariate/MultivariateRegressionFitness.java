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
package io.github.ericmedvet.jgea.problem.regression.multivariate;

import io.github.ericmedvet.jgea.core.fitness.ExampleBasedFitness;
import io.github.ericmedvet.jgea.core.representation.NamedMultivariateRealFunction;
import io.github.ericmedvet.jgea.core.util.IndexedProvider;
import io.github.ericmedvet.jgea.problem.regression.univariate.UnivariateRegressionFitness;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.IntStream;

public interface MultivariateRegressionFitness extends ExampleBasedFitness<NamedMultivariateRealFunction, Map<String, Double>, Map<String, Double>, MultivariateRegressionFitness.Outcome, Double> {
  record Outcome(List<UnivariateRegressionFitness.Outcome> outcomes) {}

  static MultivariateRegressionFitness from(
      Function<List<Outcome>, Double> aggregateFunction,
      IndexedProvider<Example<Map<String, Double>, Map<String, Double>>> caseProvider
  ) {
    record HardMultivariateRegressionFitness(
        Function<List<Outcome>, Double> aggregateFunction,
        IndexedProvider<Example<Map<String, Double>, Map<String, Double>>> caseProvider
    ) implements MultivariateRegressionFitness {}
    return new HardMultivariateRegressionFitness(aggregateFunction, caseProvider);
  }

  static MultivariateRegressionFitness from(
      UnivariateRegressionFitness.Metric metric,
      IndexedProvider<Example<Map<String, Double>, Map<String, Double>>> caseProvider
  ) {
    Function<List<Outcome>, Double> aggregateFunction = os -> IntStream.range(0, os.getFirst().outcomes.size())
        .mapToDouble(j -> metric.apply(os.stream().map(o -> o.outcomes.get(j)).toList()))
        .average()
        .orElseThrow();
    return from(aggregateFunction, caseProvider);
  }

  @Override
  default BiFunction<Map<String, Double>, Map<String, Double>, Outcome> errorFunction() {
    return (actualYs, predictedYs) -> new Outcome(
        actualYs.keySet()
            .stream()
            .map(k -> new UnivariateRegressionFitness.Outcome(actualYs.get(k), predictedYs.get(k)))
            .toList()
    );
  }

  @Override
  default BiFunction<NamedMultivariateRealFunction, Map<String, Double>, Map<String, Double>> predictFunction() {
    return NamedMultivariateRealFunction::compute;
  }
}
