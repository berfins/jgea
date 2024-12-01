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
package io.github.ericmedvet.jgea.problem.regression.univariate;

import io.github.ericmedvet.jgea.core.fitness.ExampleBasedFitness;
import io.github.ericmedvet.jgea.core.problem.ExampleBasedProblem;
import io.github.ericmedvet.jgea.core.problem.ProblemWithExampleSolution;
import io.github.ericmedvet.jgea.core.problem.TotalOrderQualityBasedProblem;
import io.github.ericmedvet.jgea.core.representation.NamedUnivariateRealFunction;
import io.github.ericmedvet.jgea.core.util.IndexedProvider;
import io.github.ericmedvet.jsdynsym.core.numerical.UnivariateRealFunction;
import java.util.Comparator;
import java.util.Map;

public interface UnivariateRegressionProblem extends ExampleBasedProblem<NamedUnivariateRealFunction, Map<String, Double>, Map<String, Double>, UnivariateRegressionFitness.Outcome, Double>, TotalOrderQualityBasedProblem<NamedUnivariateRealFunction, Double>, ProblemWithExampleSolution<NamedUnivariateRealFunction> {

  @Override
  UnivariateRegressionFitness qualityFunction();

  @Override
  UnivariateRegressionFitness validationQualityFunction();

  @Override
  default NamedUnivariateRealFunction example() {
    ExampleBasedFitness.Example<Map<String, Double>, Map<String, Double>> example = qualityFunction().caseProvider()
        .first();
    return NamedUnivariateRealFunction.from(
        UnivariateRealFunction.from(inputs -> 0d, example.input().size()),
        example.input().keySet().stream().sorted().toList(),
        qualityFunction().yVarName()
    );
  }

  @Override
  default Comparator<Double> totalOrderComparator() {
    return Double::compareTo;
  }

  static UnivariateRegressionProblem from(
      UnivariateRegressionFitness qualityFunction,
      UnivariateRegressionFitness validationQualityFunction
  ) {
    record HardUnivariateRegressionProblem(
        UnivariateRegressionFitness qualityFunction,
        UnivariateRegressionFitness validationQualityFunction
    ) implements UnivariateRegressionProblem {}
    return new HardUnivariateRegressionProblem(qualityFunction, validationQualityFunction);
  }

  static UnivariateRegressionProblem from(
      UnivariateRegressionFitness.Metric metric,
      IndexedProvider<ExampleBasedFitness.Example<Map<String, Double>, Map<String, Double>>> caseProvider,
      IndexedProvider<ExampleBasedFitness.Example<Map<String, Double>, Map<String, Double>>> validationCaseProvider,
      String yVarName
  ) {
    return from(
        UnivariateRegressionFitness.from(metric, caseProvider, yVarName),
        UnivariateRegressionFitness.from(metric, validationCaseProvider, yVarName)
    );
  }

  static UnivariateRegressionProblem from(
      UnivariateRegressionFitness.Metric metric,
      NamedUnivariateRealFunction target,
      IndexedProvider<Map<String, Double>> inputProvider,
      IndexedProvider<Map<String, Double>> validationInputProvider
  ) {
    return from(
        metric,
        inputProvider.then(
            i -> new ExampleBasedFitness.Example<>(
                i,
                target.compute(i)
            )
        ),
        validationInputProvider.then(
            i -> new ExampleBasedFitness.Example<>(
                i,
                target.compute(i)
            )
        ),
        target.yVarName()
    );
  }
}
