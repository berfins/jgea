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

import io.github.ericmedvet.jgea.core.distance.Distance;
import io.github.ericmedvet.jgea.core.order.ParetoDominance;
import io.github.ericmedvet.jgea.core.order.PartialComparator;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public interface MultiTargetProblem<S> extends TotalOrderQualityBasedProblem<S, Double> {
  Distance<S> distance();

  Collection<S> targets();

  @Override
  default Function<S, Double> qualityFunction() {
    return s -> targets().stream()
        .mapToDouble(t -> distance().apply(s, t))
        .min()
        .orElseThrow();
  }

  @Override
  default Comparator<Double> totalOrderComparator() {
    return Double::compareTo;
  }

  default SimpleMultiHomogeneousObjectiveProblem<S, Double> toMHOProblem() {
    List<S> targets = targets().stream().toList();
    SequencedMap<String, Comparator<Double>> comparators = IntStream.range(
        0,
        targets.size()
    )
        .boxed()
        .collect(
            Collectors.toMap(
                "target%d"::formatted,
                i -> Double::compareTo,
                (c1, c2) -> c1,
                TreeMap::new
            )
        );
    Function<S, Map<String, Double>> outcomeF = s -> IntStream.range(0, targets().size())
        .boxed()
        .collect(
            Collectors.toMap(
                "target%d"::formatted,
                i -> distance().apply(s, targets.get(i)),
                (c1, c2) -> c1,
                TreeMap::new
            )
        );
    PartialComparator<MultiHomogeneousObjectiveProblem.Outcome<Map<String, Double>, Double>> partialComparator = MultiHomogeneousObjectiveProblem.Outcome
        .partialComparator(
            ParetoDominance.build(Double.class, comparators.size())
        );
    record MHOProblem<S>(
        SequencedMap<String, Comparator<Double>> comparators,
        PartialComparator<Outcome<Map<String, Double>, Double>> qualityComparator,
        Function<S, Map<String, Double>> outcomeFunction
    ) implements SimpleMultiHomogeneousObjectiveProblem<S, Double> {}
    record MHOProblemWithExample<S>(
        SequencedMap<String, Comparator<Double>> comparators,
        PartialComparator<Outcome<Map<String, Double>, Double>> qualityComparator,
        Function<S, Map<String, Double>> outcomeFunction,
        S example
    ) implements SimpleMultiHomogeneousObjectiveProblem<S, Double>, ProblemWithExampleSolution<S> {}
    if (this instanceof ProblemWithExampleSolution<?> pwes) {
      //noinspection unchecked
      return new MHOProblemWithExample<>(comparators, partialComparator, outcomeF, (S) pwes.example());
    }
    return new MHOProblem<>(comparators, partialComparator, outcomeF);
  }
}
