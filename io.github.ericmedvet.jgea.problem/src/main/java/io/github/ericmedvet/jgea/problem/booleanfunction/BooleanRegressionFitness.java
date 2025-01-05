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

import io.github.ericmedvet.jgea.core.util.IndexedProvider;
import io.github.ericmedvet.jgea.core.util.Misc;
import io.github.ericmedvet.jnb.datastructure.NamedFunction;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.IntStream;

public interface BooleanRegressionFitness extends ExampleBasedFitness<BooleanFunction, boolean[], boolean[], Integer, Double> {
  enum Metric {
    ERROR_RATE, AVG_DISTANCE
  }

  @Override
  default BiFunction<boolean[], boolean[], Integer> errorFunction() {
    return (actual, predicted) -> IntStream.range(0, Math.min(actual.length, predicted.length))
        .map(i -> actual[i] == predicted[i] ? 0 : 1)
        .sum() + Math.abs(actual.length - predicted.length);
  }

  @Override
  default BiFunction<BooleanFunction, boolean[], boolean[]> predictFunction() {
    return Function::apply;
  }

  static BooleanRegressionFitness from(
      Function<List<Integer>, Double> aggregateFunction,
      IndexedProvider<Example<boolean[], boolean[]>> caseProvider
  ) {
    record HardBooleanRegressionFitness(
        Function<List<Integer>, Double> aggregateFunction,
        IndexedProvider<Example<boolean[], boolean[]>> caseProvider
    ) implements BooleanRegressionFitness {}
    return new HardBooleanRegressionFitness(aggregateFunction, caseProvider);
  }

  static BooleanRegressionFitness from(
      Metric metric,
      IndexedProvider<Example<boolean[], boolean[]>> caseProvider
  ) {
    Function<List<Integer>, Double> f = switch (metric) {
      case ERROR_RATE -> BooleanRegressionFitness::errorRate;
      case AVG_DISTANCE -> BooleanRegressionFitness::avgDistance;
    };
    return from(NamedFunction.from(f, Misc.enumString(metric)), caseProvider);
  }

  private static double errorRate(Collection<Integer> distances) {
    return (double) distances.stream().filter(d -> d != 0).count() / distances.size();
  }

  private static double avgDistance(Collection<Integer> distances) {
    return distances.stream().mapToDouble(Integer::doubleValue).average().orElseThrow();
  }

}
