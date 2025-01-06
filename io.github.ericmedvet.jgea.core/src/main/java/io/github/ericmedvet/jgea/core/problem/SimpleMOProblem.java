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

import io.github.ericmedvet.jgea.core.util.Misc;
import java.util.Comparator;
import java.util.Map;
import java.util.SequencedMap;
import java.util.function.Function;

public interface SimpleMOProblem<S, O> extends MultiObjectiveProblem<S, SequencedMap<String, O>, O> {

  SequencedMap<String, Comparator<O>> comparators();

  @Override
  default SequencedMap<String, Objective<SequencedMap<String, O>, O>> objectives() {
    return comparators().entrySet()
        .stream()
        .collect(
            Misc.toSequencedMap(
                Map.Entry::getKey,
                e -> new Objective<>(
                    map -> map.get(e.getKey()),
                    e.getValue()
                )
            )
        );
  }

  static <S, O> SimpleMOProblem<S, O> from(
      SequencedMap<String, Comparator<O>> comparators,
      Function<S, SequencedMap<String, O>> qualityFunction,
      Function<S, SequencedMap<String, O>> validationQualityFunction,
      S example
  ) {
    record HardSMOVEProblem<S, O>(
        SequencedMap<String, Comparator<O>> comparators,
        Function<S, SequencedMap<String, O>> qualityFunction,
        Function<S, SequencedMap<String, O>> validationQualityFunction,
        S example
    ) implements SimpleMOProblem<S, O>, ProblemWithValidation<S, SequencedMap<String, O>>, ProblemWithExampleSolution<S> {}
    record HardSMOVProblem<S, O>(
        SequencedMap<String, Comparator<O>> comparators,
        Function<S, SequencedMap<String, O>> qualityFunction,
        Function<S, SequencedMap<String, O>> validationQualityFunction
    ) implements SimpleMOProblem<S, O>, ProblemWithValidation<S, SequencedMap<String, O>> {}
    record HardSMOEProblem<S, O>(
        SequencedMap<String, Comparator<O>> comparators,
        Function<S, SequencedMap<String, O>> qualityFunction,
        S example
    ) implements SimpleMOProblem<S, O>, ProblemWithExampleSolution<S> {}
    record HardSMOProblem<S, O>(
        SequencedMap<String, Comparator<O>> comparators,
        Function<S, SequencedMap<String, O>> qualityFunction
    ) implements SimpleMOProblem<S, O> {}
    if (example != null && validationQualityFunction != null) {
      return new HardSMOVEProblem<>(comparators, qualityFunction, validationQualityFunction, example);
    }
    if (example != null) {
      return new HardSMOEProblem<>(comparators, qualityFunction, example);
    }
    if (validationQualityFunction != null) {
      return new HardSMOVProblem<>(comparators, qualityFunction, validationQualityFunction);
    }
    return new HardSMOProblem<>(comparators, qualityFunction);
  }
}
