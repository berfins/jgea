/*-
 * ========================LICENSE_START=================================
 * jgea-experimenter
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
package io.github.ericmedvet.jgea.experimenter.builders;

import io.github.ericmedvet.jgea.core.solver.Individual;
import io.github.ericmedvet.jgea.core.solver.mapelites.MapElites;
import io.github.ericmedvet.jnb.core.Cacheable;
import io.github.ericmedvet.jnb.core.Discoverable;
import io.github.ericmedvet.jnb.core.Param;
import java.util.function.Function;

@Discoverable(prefixTemplate = "ea.solver|s.mapelites|me.descriptor|d")
public class MapElitesDescriptors {
  private MapElitesDescriptors() {
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static <G, S, Q> MapElites.Descriptor<G, S, Q> descriptor(
      @Param("f") Function<Individual<G, S, Q>, Number> f,
      @Param(value = "min", dD = 0d) double min,
      @Param(value = "max", dD = 1d) double max,
      @Param(value = "nOfBins", dI = 20) int nOfBins
  ) {
    return new MapElites.Descriptor<>(f, min, max, nOfBins);
  }
}
