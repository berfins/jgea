/*-
 * ========================LICENSE_START=================================
 * jgea-core
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
package io.github.ericmedvet.jgea.core.problem;

import io.github.ericmedvet.jgea.core.order.PartialComparator;

import java.util.Comparator;
import java.util.function.Function;

public interface TotalOrderQualityBasedProblem<S, Q> extends QualityBasedProblem<S, Q> {
  Comparator<Q> totalOrderComparator();

  @Override
  default PartialComparator<Q> qualityComparator() {
    return (q1, q2) -> {
      int outcome = totalOrderComparator().compare(q1, q2);
      if (outcome == 0) {
        return PartialComparator.PartialComparatorOutcome.SAME;
      }
      if (outcome < 0) {
        return PartialComparator.PartialComparatorOutcome.BEFORE;
      }
      return PartialComparator.PartialComparatorOutcome.AFTER;
    };
  }

  static <S, Q> TotalOrderQualityBasedProblem<S, Q> from(
      Function<S, Q> qualityFunction,
      Comparator<Q> totalOrderComparator
  ) {
    return from(qualityFunction, null, totalOrderComparator, null);
  }

  static <S, Q> TotalOrderQualityBasedProblem<S, Q> from(
      Function<S, Q> qualityFunction,
      Comparator<Q> totalOrderComparator,
      S example
  ) {
    return from(qualityFunction, null, totalOrderComparator, example);
  }

  static <S, Q> TotalOrderQualityBasedProblem<S, Q> from(
      Function<S, Q> qualityFunction,
      Function<S, Q> validationqualityFunction,
      Comparator<Q> totalOrderComparator
  ) {
    return from(qualityFunction, validationqualityFunction, totalOrderComparator, null);
  }

  static <S, Q> TotalOrderQualityBasedProblem<S, Q> from(
      QualityBasedProblem<S,Q> qbProblem,
      Comparator<Q> comparator
  ) {
    if (qbProblem instanceof ProblemWithValidation<S,Q> pwv) {
      if (qbProblem instanceof ProblemWithExampleSolution<?> pse) {
        //noinspection unchecked
        return from(qbProblem.qualityFunction(), pwv.validationQualityFunction(), comparator, (S)pse.example());
      }
      return from(qbProblem.qualityFunction(), pwv.validationQualityFunction(), comparator);
    }
    if (qbProblem instanceof ProblemWithExampleSolution<?> pse) {
      //noinspection unchecked
      return from(qbProblem.qualityFunction(), comparator, (S)pse.example());
    }
    return from(qbProblem.qualityFunction(), comparator);
  }

  static <S, Q> TotalOrderQualityBasedProblem<S, Q> from(
      Function<S, Q> qualityFunction,
      Function<S, Q> validationQualityFunction,
      Comparator<Q> totalOrderComparator,
      S example
  ) {
    record HardTOQVEProblem<S, Q>(
        Function<S, Q> qualityFunction,
        Function<S, Q> validationQualityFunction,
        Comparator<Q> totalOrderComparator,
        S example
    ) implements TotalOrderQualityBasedProblem<S, Q>, ProblemWithExampleSolution<S>, ProblemWithValidation<S, Q> {}
    record HardTOQEProblem<S, Q>(
        Function<S, Q> qualityFunction,
        Comparator<Q> totalOrderComparator,
        S example
    ) implements TotalOrderQualityBasedProblem<S, Q>, ProblemWithExampleSolution<S> {}
    record HardTOQVProblem<S, Q>(
        Function<S, Q> qualityFunction,
        Function<S, Q> validationQualityFunction,
        Comparator<Q> totalOrderComparator
    ) implements TotalOrderQualityBasedProblem<S, Q>, ProblemWithValidation<S, Q> {}
    record HardTOQProblem<S, Q>(
        Function<S, Q> qualityFunction,
        Comparator<Q> totalOrderComparator
    ) implements TotalOrderQualityBasedProblem<S, Q> {}
    if (example != null && validationQualityFunction != null) {
      return new HardTOQVEProblem<>(qualityFunction, validationQualityFunction, totalOrderComparator, example);
    }
    if (example != null) {
      return new HardTOQEProblem<>(qualityFunction, totalOrderComparator, example);
    }
    if (validationQualityFunction != null) {
      return new HardTOQVProblem<>(qualityFunction, validationQualityFunction, totalOrderComparator);
    }
    return new HardTOQProblem<>(qualityFunction, totalOrderComparator);
  }
}
