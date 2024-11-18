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

package io.github.ericmedvet.jgea.core.distance;

import java.util.List;

public class StringSequence implements Distance<String> {

  private final Distance<List<Character>> innerDistance;

  public StringSequence(Distance<List<Character>> innerDistance) {
    this.innerDistance = innerDistance;
  }

  @Override
  public Double apply(String string1, String string2) {
    return innerDistance.apply(
        string1.chars().mapToObj(c -> (char) c).toList(),
        string2.chars().mapToObj(c -> (char) c).toList()
    );
  }
}
