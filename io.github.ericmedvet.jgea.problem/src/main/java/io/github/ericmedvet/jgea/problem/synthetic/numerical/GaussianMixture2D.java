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

import io.github.ericmedvet.jgea.core.problem.TotalOrderQualityBasedProblem;
import io.github.ericmedvet.jgea.core.util.VectorUtils;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class GaussianMixture2D implements TotalOrderQualityBasedProblem<List<Double>, Double> {

  private final List<Double> example;
  private final Function<List<Double>, Double> qualityFunction;

  public GaussianMixture2D(Map<List<Double>, Double> optima, double c) {
    example = List.of(0d, 0d);
    qualityFunction = x -> optima.entrySet()
        .stream()
        .mapToDouble(e -> e.getValue() * Math.exp(-VectorUtils.norm(VectorUtils.diff(x, e.getKey()), 2) * c))
        .sum();
  }

  public GaussianMixture2D(List<Double> xs, double c) {
    this(
        IntStream.range(0, xs.size())
            .boxed()
            .collect(Collectors.toMap(i -> List.of(xs.get(i), 0d), i -> (double) (i + 1))),
        c
    );
  }

  @Override
  public Optional<List<Double>> example() {
    return Optional.of(example);
  }

  @Override
  public Function<List<Double>, Double> qualityFunction() {
    return qualityFunction;
  }

  @Override
  public Comparator<Double> totalOrderComparator() {
    return ((Comparator<Double>) Double::compareTo).reversed();
  }
}
