/*-
 * ========================LICENSE_START=================================
 * jgea-core
 * %%
 * Copyright (C) 2018 - 2025 Eric Medvet
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

import java.util.Comparator;
import java.util.SequencedMap;
import java.util.function.Function;

// TODO: (b) add simple MOProblem with Q = Map<String,O>, (c) remove old
// MO problem

public interface MOProblem<S, Q, O> extends QualityBasedProblem<S, Q> {
  record Objective<Q, O>(
      Function<? super Q, ? extends O> function,
      Comparator<O> comparator
  ) {}

  SequencedMap<String, Objective<Q, O>> objectives();

  default TotalOrderQualityBasedProblem<S, Q> asTotalOrderQualityBasedProblem(String objective) {
    Function<? super Q, ? extends O> oFunction = objectives().get(objective).function();
    Comparator<O> oComparator = objectives().get(objective).comparator();
    return TotalOrderQualityBasedProblem.from(
        qualityFunction(),
        Comparator.comparing(oFunction, oComparator)
    );
  }

  default TotalOrderQualityBasedProblem<S, Q> asTotalOrderQualityBasedProblem() {
    return asTotalOrderQualityBasedProblem(objectives().firstEntry().getKey());
  }

  @Override
  default PartialComparator<Q> qualityComparator() {
    ParetoDominance<O> paretoDominance = new ParetoDominance<>(
        objectives().values()
            .stream()
            .map(Objective::comparator)
            .toList()
    );
    return paretoDominance.on(
        q -> objectives().values()
            .stream()
            .map(obj -> (O) obj.function.apply(q))
            .toList()
    );
  }
}
