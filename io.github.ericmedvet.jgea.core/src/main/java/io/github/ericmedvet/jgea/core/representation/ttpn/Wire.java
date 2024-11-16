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
package io.github.ericmedvet.jgea.core.representation.ttpn;

public record Wire(EndPoint src, EndPoint dst) {

  public record EndPoint(int gateIndex, int portIndex) {
    @Override
    public String toString() {
      return "%d.%d".formatted(gateIndex, portIndex);
    }
  }

  public static Wire of(int srcGateIndex, int srcPortIndex, int dstGateIndex, int dstPortIndex) {
    return new Wire(new EndPoint(srcGateIndex, srcPortIndex), new EndPoint(dstGateIndex, dstPortIndex));
  }

  @Override
  public String toString() {
    return "%s->%s".formatted(src, dst);
  }
}
