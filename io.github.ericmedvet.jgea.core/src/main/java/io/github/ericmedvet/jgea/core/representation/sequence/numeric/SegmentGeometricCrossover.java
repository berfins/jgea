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
package io.github.ericmedvet.jgea.core.representation.sequence.numeric;

import io.github.ericmedvet.jgea.core.operator.Crossover;
import io.github.ericmedvet.jnb.datastructure.DoubleRange;
import java.util.List;
import java.util.random.RandomGenerator;
import java.util.stream.IntStream;

public class SegmentGeometricCrossover implements Crossover<List<Double>> {
  private final DoubleRange range;

  public SegmentGeometricCrossover(DoubleRange range) {
    this.range = range;
  }

  public SegmentGeometricCrossover() {
    this(DoubleRange.UNIT);
  }

  @Override
  public List<Double> recombine(List<Double> g1, List<Double> g2, RandomGenerator random) {
    if (g1.size() != g2.size()) {
      throw new IllegalArgumentException(
          "Parent genotype sizes are different: %d vs. %d".formatted(g1.size(), g2.size())
      );
    }
    double alpha = range.denormalize(random.nextDouble());
    return IntStream.range(0, g1.size())
        .mapToObj(i -> g1.get(i) + (g2.get(i) - g1.get(i)) * alpha)
        .toList();
  }
}
