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
import io.github.ericmedvet.jgea.core.util.Misc;
import java.util.Map;
import java.util.SequencedMap;
import java.util.function.Function;

public interface BehaviorBasedMOProblem<S, B, O> extends MOProblem<S, BehaviorBasedProblem.Outcome<B, Map<String, O>>, O>, BehaviorBasedProblem<S, B, Map<String, O>> {

  SequencedMap<String, Objective<B, O>> behaviorObjectives();

  @Override
  default SequencedMap<String, Objective<Outcome<B, Map<String, O>>, O>> objectives() {
    return Misc.sequencedTransformValues(
        behaviorObjectives(),
        obj -> new Objective<>(o -> obj.function().apply(o.behavior()), obj.comparator())
    );
  }

  @Override
  default Function<? super B, ? extends Map<String, O>> behaviorQualityFunction() {
    return b -> Misc.sequencedTransformValues(behaviorObjectives(), obj -> obj.function().apply(b));
  }

  @Override
  default PartialComparator<Map<String, O>> behaviorQualityComparator() {
    return new ParetoDominance<>(behaviorObjectives().values().stream().map(Objective::comparator).toList())
        .on(m -> objectives().keySet().stream().map(m::get).toList());
  }

  @Override
  default PartialComparator<Outcome<B, Map<String, O>>> qualityComparator() {
    return behaviorQualityComparator().on(Outcome::behaviorQuality);
  }
}
