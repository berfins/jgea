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

import io.github.ericmedvet.jgea.core.order.ParetoDominance;
import io.github.ericmedvet.jgea.core.order.PartialComparator;
import io.github.ericmedvet.jgea.core.util.Misc;
import java.util.*;
import java.util.function.Function;

public interface MultiHomogeneousObjectiveProblem<S, O, Q> extends QualityBasedProblem<S, MultiHomogeneousObjectiveProblem.Outcome<O, Q>> {

  record Objective<O, Q>(
      Function<? super O, ? extends Q> function,
      Comparator<Q> comparator
  ) {
  }

  record Outcome<O, Q>(O outcome, Map<String, Q> objectives) {}

  SequencedMap<String, Objective<O, Q>> objectives();

  Function<S, O> outcomeFunction();

  @Override
  default Function<S, Outcome<O, Q>> qualityFunction() {
    return s -> {
      O o = outcomeFunction().apply(s);
      return new Outcome<>(
          o,
          objectives().entrySet()
              .stream()
              .collect(
                  Misc.toSequencedMap(
                      Map.Entry::getKey,
                      e -> e.getValue().function.apply(o)
                  )
              )
      );
    };
  }

  @Override
  default PartialComparator<Outcome<O, Q>> qualityComparator() {
    ParetoDominance<Q> objectivesComparator = new ParetoDominance<>(
        objectives().values()
            .stream()
            .map(Objective::comparator)
            .toList()
    );
    return objectivesComparator.on(
        (Outcome<O, Q> o) -> objectives().keySet()
            .stream()
            .map(n -> o.objectives().get(n))
            .toList()
    );
  }
}
