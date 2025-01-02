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

package io.github.ericmedvet.jgea.problem.synthetic.numerical;

import io.github.ericmedvet.jgea.core.order.ParetoDominance;
import io.github.ericmedvet.jgea.core.order.PartialComparator;
import io.github.ericmedvet.jgea.core.problem.MultiHomogeneousObjectiveProblem;
import io.github.ericmedvet.jgea.core.problem.ProblemWithExampleSolution;
import io.github.ericmedvet.jgea.core.problem.SimpleMultiHomogeneousObjectiveProblem;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public record Cones(
    SequencedMap<String, Comparator<Double>> comparators,
    Function<List<Double>, Map<String, Double>> outcomeFunction,
    PartialComparator<Outcome<Map<String, Double>, Double>> qualityComparator,
    List<Double> example
) implements SimpleMultiHomogeneousObjectiveProblem<List<Double>, Double>, ProblemWithExampleSolution<List<Double>> {

  private static final SequencedMap<String, Comparator<Double>> COMPARATORS = Stream.of(
      Map.entry("lateralSurface", (Comparator<Double>) Double::compareTo),
      Map.entry("totalSurface", (Comparator<Double>) Double::compareTo),
      Map.entry("volume", ((Comparator<Double>) Double::compareTo).reversed())
  ).collect(Collectors.toMap(
      Map.Entry::getKey,
      Map.Entry::getValue,
      (c1, c2) -> c1,
      LinkedHashMap::new
  ));

  public Cones(
  ) {
    this(
        COMPARATORS,
        vs -> {
          double r = vs.get(0);
          double h = vs.get(1);
          double s = Math.sqrt(r * r + h * h);
          double lateralSurface = Math.PI * r * s;
          double totalSurface = Math.PI * r * (r + s);
          double volume = Math.PI * r * r * h / 3;
          return Map.ofEntries(
              Map.entry("lateralSurface", lateralSurface),
              Map.entry("totalSurface", totalSurface),
              Map.entry("volume", lateralSurface)
          );
        },
        Outcome.partialComparator(new ParetoDominance<>(COMPARATORS.values().stream().toList())),
        List.of(0d, 0d)
    );
  }
}
