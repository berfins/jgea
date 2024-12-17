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
package io.github.ericmedvet.jgea.problem.programsynthesis;

import io.github.ericmedvet.jgea.core.representation.programsynthesis.type.Typed;
import java.util.List;
import java.util.stream.IntStream;

public class Problems {
  private Problems() {
  }

  @Typed("R")
  public static Double vProduct(@Typed("[R]") List<Double> v1, @Typed("[R]") List<Double> v2) {
    if (v1.size() != v2.size()) {
      throw new IllegalArgumentException("Input sizes are different: %d and %d".formatted(v1.size(), v2.size()));
    }
    return IntStream.range(0, v1.size()).mapToDouble(i -> v1.get(i) * v2.get(i)).sum();
  }
}
