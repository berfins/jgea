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

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public interface MultiHomogeneousObjectiveProblem<S, O, Q> extends QualityBasedProblem<S,
    MultiHomogeneousObjectiveProblem.Outcome<O, Q>> {

  record Objective<O, Q>(
      Function<? super O, ? extends Q> function,
      Comparator<Q> comparator
  ) {
  }

  interface Outcome<O, Q> {
    O outcome();

    Map<String, Q> objectives();

    static <O, Q> MultiHomogeneousObjectiveProblem.Outcome<O, Q> from(
        O o,
        SequencedMap<String, Objective<O, Q>> objectives
    ) {
      record HardOutcome<O, Q>(
          O outcome,
          Map<String, Q> objectives
      ) implements Outcome<O, Q> {}
      return new HardOutcome<>(
          o,
          objectives.entrySet().stream().collect(Collectors.toMap(
              Map.Entry::getKey,
              e -> e.getValue().function().apply(o),
              (q1, q2) -> q1,
              LinkedHashMap::new
          ))
      );
    }

    static <O, Q> PartialComparator<Outcome<O, Q>> partialComparator(
        PartialComparator<List<Q>>
            partialComparator
    ) {
      return (o1, o2) -> partialComparator.compare(
          o1.objectives().keySet().stream().map(k -> o1.objectives().get(k)).toList(),
          o1.objectives().keySet().stream().map(k -> o2.objectives().get(k)).toList()
      );
    }
  }

  SequencedMap<String, Objective<O, Q>> objectives();

  Function<S, O> outcomeFunction();

  @Override
  default Function<S, Outcome<O, Q>> qualityFunction() {
    return s -> Outcome.from(outcomeFunction().apply(s), objectives());
  }

}
