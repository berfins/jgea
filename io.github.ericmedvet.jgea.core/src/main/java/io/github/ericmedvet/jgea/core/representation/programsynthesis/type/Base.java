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
package io.github.ericmedvet.jgea.core.representation.programsynthesis.type;

import java.util.Map;
import java.util.Set;
import java.util.function.ToIntFunction;

public enum Base implements Type {
  BOOLEAN(Boolean.class, o -> 1), INT(Integer.class, o -> 1), REAL(Double.class, o -> 1), STRING(
      String.class,
      o -> ((String) o).length()
  );

  private final Class<?> javaClass;
  private final ToIntFunction<Object> sizer;

  Base(Class<?> javaClass, ToIntFunction<Object> sizer) {
    this.javaClass = javaClass;
    this.sizer = sizer;
  }

  @Override
  public boolean canTakeValuesOf(Type other) {
    return equals(other);
  }

  @Override
  public Type concrete(Map<Generic, Type> genericTypeMap) {
    return this;
  }

  @Override
  public Set<Generic> generics() {
    return Set.of();
  }

  @Override
  public boolean matches(Object o) {
    return javaClass.isInstance(o);
  }

  @Override
  public Map<Generic, Type> resolveGenerics(Type concreteType) {
    return Map.of();
  }

  @Override
  public int sizeOf(Object o) {
    return sizer.applyAsInt(o);
  }

  @Override
  public String toString() {
    return "%s".formatted(name().substring(0, 1));
  }

}
