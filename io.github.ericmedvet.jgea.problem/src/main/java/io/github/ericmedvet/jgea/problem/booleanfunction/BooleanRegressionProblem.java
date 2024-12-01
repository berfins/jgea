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
package io.github.ericmedvet.jgea.problem.booleanfunction;

import io.github.ericmedvet.jgea.core.fitness.ExampleBasedFitness;
import io.github.ericmedvet.jgea.core.problem.ExampleBasedProblem;
import io.github.ericmedvet.jgea.core.problem.ProblemWithExampleSolution;
import io.github.ericmedvet.jgea.core.problem.TotalOrderQualityBasedProblem;
import io.github.ericmedvet.jgea.core.util.IndexedProvider;
import java.util.Comparator;

public interface BooleanRegressionProblem extends ExampleBasedProblem<BooleanFunction, boolean[], boolean[], Integer, Double>, TotalOrderQualityBasedProblem<BooleanFunction, Double>, ProblemWithExampleSolution<BooleanFunction> {
  @Override
  BooleanRegressionFitness qualityFunction();

  @Override
  BooleanRegressionFitness validationQualityFunction();

  @Override
  default BooleanFunction example() {
    ExampleBasedFitness.Example<boolean[], boolean[]> example = qualityFunction().caseProvider().first();
    return BooleanFunction.from(
        inputs -> new boolean[example.output().length],
        example.input().length,
        example.output().length
    );
  }

  @Override
  default Comparator<Double> totalOrderComparator() {
    return Double::compareTo;
  }

  static BooleanRegressionProblem from(
      BooleanRegressionFitness qualityFunction,
      BooleanRegressionFitness validationQualityFunction
  ) {
    record HardBooleanRegressionProblem(
        BooleanRegressionFitness qualityFunction,
        BooleanRegressionFitness validationQualityFunction
    ) implements BooleanRegressionProblem {}
    return new HardBooleanRegressionProblem(qualityFunction, validationQualityFunction);
  }

  static BooleanRegressionProblem from(
      BooleanRegressionFitness.Metric metric,
      IndexedProvider<ExampleBasedFitness.Example<boolean[], boolean[]>> caseProvider,
      IndexedProvider<ExampleBasedFitness.Example<boolean[], boolean[]>> validationCaseProvider
  ) {
    return from(
        BooleanRegressionFitness.from(metric, caseProvider),
        BooleanRegressionFitness.from(metric, validationCaseProvider)
    );
  }

  static BooleanRegressionProblem from(
      BooleanRegressionFitness.Metric metric,
      BooleanFunction target,
      IndexedProvider<boolean[]> inputProvider,
      IndexedProvider<boolean[]> validationInputProvider
  ) {
    return from(
        BooleanRegressionFitness.from(
            metric,
            inputProvider.then(
                i -> new ExampleBasedFitness.Example<>(
                    i,
                    target.apply(i)
                )
            )
        ),
        BooleanRegressionFitness.from(
            metric,
            validationInputProvider.then(
                i -> new ExampleBasedFitness.Example<>(
                    i,
                    target.apply(i)
                )
            )
        )
    );
  }
}
