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
package io.github.ericmedvet.jgea.core.representation.ttpn.type;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record Sequence(Type type) implements Composed {
  @Override
  public boolean canTakeValuesOf(Type other) {
    if (other instanceof Sequence(Type otherType)) {
      return type.canTakeValuesOf(otherType);
    }
    return false;
  }

  @Override
  public Set<Generic> generics() {
    return type.generics();
  }

  @Override
  public boolean matches(Object o) {
    if (o instanceof List<?> list) {
      return list.stream().allMatch(lO -> type().matches(lO));
    }
    return false;
  }

  @Override
  public Map<Generic, Type> resolveGenerics(Type concreteType) throws TypeException {
    if (concreteType instanceof Sequence(Type otherType)) {
      return type.resolveGenerics(otherType);
    }
    throw new TypeException("Wrong concrete type %s".formatted(concreteType));
  }

  @Override
  public Type concrete(Map<Generic, Type> genericTypeMap) throws TypeException {
    if (!isGenerics()) {
      return this;
    }
    return Composed.sequence(type.concrete(genericTypeMap));
  }

  @Override
  public String toString() {
    return "[%s]".formatted(type);
  }
}
