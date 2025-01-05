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
import io.github.ericmedvet.jgea.core.util.Misc;

import java.util.*;
import java.util.function.Function;
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

  default SimpleMOProblem<S, Double> toMHOProblem() {
    List<S> targets = targets().stream().toList();
    SequencedMap<String, Comparator<Double>> comparators = IntStream.range(
            0,
            targets.size()
        )
        .boxed()
        .collect(
            Misc.toSequencedMap(
                "target%d"::formatted,
                i -> Double::compareTo
            )
        );
    Function<S, SequencedMap<String, Double>> outcomeF = s -> IntStream.range(0, targets().size())
        .boxed()
        .collect(
            Misc.toSequencedMap(
                "target%d"::formatted,
                i -> distance().apply(s, targets.get(i))
            )
        );
    record HardMOProblem<S>(
        SequencedMap<String, Comparator<Double>> comparators,
        Function<S, SequencedMap<String, Double>> qualityFunction
    ) implements SimpleMOProblem<S, Double> {}
    record HardMOProblemWithExample<S>(
        SequencedMap<String, Comparator<Double>> comparators,
        Function<S, SequencedMap<String, Double>> qualityFunction,
        S example
    ) implements SimpleMOProblem<S, Double>, ProblemWithExampleSolution<S> {}
    if (this instanceof ProblemWithExampleSolution<?> pwes) {
      //noinspection unchecked
      return new HardMOProblemWithExample<>(comparators, outcomeF, (S) pwes.example());
    }
    return new HardMOProblem<>(comparators, outcomeF);
  }
}
