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
import io.github.ericmedvet.jgea.core.problem.*;
import io.github.ericmedvet.jgea.core.representation.NamedMultivariateRealFunction;
import io.github.ericmedvet.jgea.core.representation.NamedUnivariateRealFunction;
import io.github.ericmedvet.jgea.core.util.IndexedProvider;
import io.github.ericmedvet.jgea.problem.regression.univariate.UnivariateRegressionFitness;
import io.github.ericmedvet.jsdynsym.core.numerical.MultivariateRealFunction;
import io.github.ericmedvet.jsdynsym.core.numerical.UnivariateRealFunction;

import java.util.Comparator;
import java.util.Map;

public interface MultivariateRegressionProblem extends ExampleBasedProblem<NamedMultivariateRealFunction, Map<String,
    Double>, Map<String, Double>, MultivariateRegressionFitness.Outcome,
    Double>, TotalOrderQualityBasedProblem<NamedMultivariateRealFunction, Double>,
    ProblemWithExampleSolution<NamedMultivariateRealFunction> {

  @Override
  MultivariateRegressionFitness qualityFunction();

  @Override
  MultivariateRegressionFitness validationQualityFunction();

  static MultivariateRegressionProblem from(
      MultivariateRegressionFitness qualityFunction,
      MultivariateRegressionFitness validationQualityFunction
  ) {
    record HardMultivariateRegressionProblem(
        MultivariateRegressionFitness qualityFunction,
        MultivariateRegressionFitness validationQualityFunction
    ) implements MultivariateRegressionProblem {}
    return new HardMultivariateRegressionProblem(qualityFunction, validationQualityFunction);
  }

  static MultivariateRegressionProblem from(
      UnivariateRegressionFitness.Metric metric,
      IndexedProvider<ExampleBasedFitness.Example<Map<String, Double>, Map<String, Double>>> caseProvider,
      IndexedProvider<ExampleBasedFitness.Example<Map<String, Double>, Map<String, Double>>> validationCaseProvider
  ) {
    return from(
        MultivariateRegressionFitness.from(metric, caseProvider),
        MultivariateRegressionFitness.from(metric, validationCaseProvider)
    );
  }

  static MultivariateRegressionProblem from(
      UnivariateRegressionFitness.Metric metric,
      NamedMultivariateRealFunction target,
      IndexedProvider<Map<String, Double>> inputProvider,
      IndexedProvider<Map<String, Double>> validationInputProvider
  ) {
    return from(
        metric,
        inputProvider.then(i -> new ExampleBasedFitness.Example<>(
            i,
            target.compute(i)
        )),
        validationInputProvider.then(i -> new ExampleBasedFitness.Example<>(
            i,
            target.compute(i)
        ))
    );
  }

  @Override
  default NamedMultivariateRealFunction example() {
    ExampleBasedFitness.Example<Map<String, Double>, Map<String, Double>> example = qualityFunction().caseProvider()
        .first();
    return NamedMultivariateRealFunction.from(
        MultivariateRealFunction.from(
            xs -> new double[example.output().size()],
            example.input().size(),
            example.output().size()
        ),
        example.input().keySet().stream().toList(),
        example.output().keySet().stream().toList()
    );
  }

  @Override
  default Comparator<Double> totalOrderComparator() {
    return Double::compareTo;
  }

}
