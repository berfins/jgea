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
package io.github.ericmedvet.jgea.problem.classification;

import io.github.ericmedvet.jgea.core.fitness.ExampleBasedFitness;
import io.github.ericmedvet.jgea.core.util.IndexedProvider;
import io.github.ericmedvet.jgea.core.util.Misc;
import io.github.ericmedvet.jnb.datastructure.NamedFunction;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public interface ClassificationFitness<X, Y extends Enum<Y>> extends ExampleBasedFitness<Classifier<X, Y>, X, Y, ClassificationFitness.Outcome<Y>, Double> {

  enum Metric {
    ERROR_RATE, WEIGHTED_ERROR_RATE;
  }

  record Outcome<Y extends Enum<Y>>(Y actual, Y predicted) {}

  private static <Y extends Enum<Y>> double errorRate(Collection<Outcome<Y>> outcomes) {
    return outcomes.stream()
        .mapToDouble(o -> o.actual.equals(o.predicted) ? 0d : 1d)
        .average()
        .orElseThrow();
  }

  static <X, Y extends Enum<Y>> ClassificationFitness<X, Y> from(
      Metric metric,
      IndexedProvider<Example<X, Y>> caseProvider
  ) {
    Function<List<Outcome<Y>>, Double> f = switch (metric) {
      case ERROR_RATE -> ClassificationFitness::errorRate;
      case WEIGHTED_ERROR_RATE -> ClassificationFitness::weightedErrorRate;
    };
    return from(NamedFunction.from(f, Misc.enumString(metric)), caseProvider);
  }

  static <X, Y extends Enum<Y>> ClassificationFitness<X, Y> from(
      Function<List<Outcome<Y>>, Double> aggregateFunction,
      IndexedProvider<Example<X, Y>> caseProvider
  ) {
    record HardClassificationFitness<X, Y extends Enum<Y>>(
        Function<List<Outcome<Y>>, Double> aggregateFunction,
        IndexedProvider<Example<X, Y>> caseProvider
    ) implements ClassificationFitness<X, Y> {}
    return new HardClassificationFitness<>(aggregateFunction, caseProvider);
  }

  private static <Y extends Enum<Y>> double weightedErrorRate(Collection<Outcome<Y>> outcomes) {
    return outcomes.stream()
        .collect(Collectors.groupingBy(o -> o.actual))
        .values()
        .stream()
        .mapToDouble(ClassificationFitness::errorRate)
        .average()
        .orElseThrow();
  }

  @Override
  default BiFunction<Y, Y, Outcome<Y>> errorFunction() {
    return Outcome::new;
  }

  @Override
  default BiFunction<Classifier<X, Y>, X, Y> predictFunction() {
    return Classifier::classify;
  }
}
