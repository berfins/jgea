/*-
 * ========================LICENSE_START=================================
 * jgea-problem
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
package io.github.ericmedvet.jgea.problem.synthetic;

import io.github.ericmedvet.jgea.core.problem.BehaviorBasedMOProblem;
import io.github.ericmedvet.jgea.core.problem.MOProblem;
import io.github.ericmedvet.jgea.core.problem.ProblemWithExampleSolution;
import io.github.ericmedvet.jgea.core.util.Misc;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public record LettersMax(
    Function<? super String, ? extends Map<String, Integer>> behaviorFunction,
    SequencedMap<String, MOProblem.Objective<Map<String, Integer>, Integer>> behaviorObjectives,
    String example
) implements BehaviorBasedMOProblem<String, Map<String, Integer>, Integer>, ProblemWithExampleSolution<String> {
  public LettersMax(
      SequencedSet<String> letters,
      int l
  ) {
    this(
        s -> countCharOccurrences(s, letters),
        letters.stream()
            .collect(
                Misc.toSequencedMap(
                    c -> new Objective<>(
                        occurrences -> occurrences.get(c),
                        ((Comparator<Integer>) Integer::compareTo).reversed()
                    )
                )
            ),
        String.join("", Collections.nCopies(l, "."))
    );
  }

  private static Map<String, Integer> countCharOccurrences(String s, SequencedSet<String> letters) {
    Map<String, Integer> rawCount = new HashMap<>(
        s.codePoints()
            .mapToObj(c -> String.valueOf((char) c))
            .collect(
                Collectors.groupingBy(
                    c -> c,
                    Collectors.summingInt(c -> 1)
                )
            )
    );
    letters.forEach(l -> rawCount.computeIfAbsent(l, ll -> 0));
    return rawCount;
  }

}
