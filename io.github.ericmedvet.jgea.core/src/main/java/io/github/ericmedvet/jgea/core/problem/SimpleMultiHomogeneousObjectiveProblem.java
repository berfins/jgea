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

public interface SimpleMultiHomogeneousObjectiveProblem<S, Q> extends MultiHomogeneousObjectiveProblem<S, Map<String, Q>, Q> {
  SequencedMap<String, Comparator<Q>> comparators();

  @Override
  default SequencedMap<String, Objective<Map<String, Q>, Q>> objectives() {
    return comparators().entrySet()
        .stream()
        .collect(
            Misc.toSequencedMap(
                Map.Entry::getKey,
                e -> new Objective<>(m -> m.get(e.getKey()), e.getValue())
            )
        );
  }

}
