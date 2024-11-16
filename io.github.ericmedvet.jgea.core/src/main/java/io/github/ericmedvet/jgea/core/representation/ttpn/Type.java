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
/*
 * Copyright 2024 eric
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.ericmedvet.jgea.core.representation.ttpn;

import io.github.ericmedvet.jgea.core.util.Misc;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface Type {

  enum Base implements Type {
    BOOLEAN(Boolean.class),
    INT(Integer.class),
    REAL(Double.class),
    STRING(String.class);

    private final Class<?> javaClass;

    Base(Class<?> javaClass) {
      this.javaClass = javaClass;
    }

    @Override
    public boolean canTakeValuesOf(Type other) {
      return equals(other);
    }

    @Override
    public boolean matches(Object o) {
      return javaClass.isInstance(o);
    }

    @Override
    public Set<Generic> generics() {
      return Set.of();
    }

  }

  interface Composed extends Type {
    record Pair(Type firstType, Type secondType) implements Composed {
      @Override
      public boolean canTakeValuesOf(Type other) {
        if (other instanceof Pair(Type otherFirstType, Type otherSecondType)) {
          return firstType.canTakeValuesOf(otherFirstType) && secondType.canTakeValuesOf(otherSecondType);
        }
        return false;
      }

      @Override
      public boolean matches(Object o) {
        if (o instanceof List<?> list) {
          if (list.size() == 2) {
            if (firstType.matches(list.getFirst())) {
              return secondType.matches(list.getLast());
            }
          }
        }
        return false;
      }

      @Override
      public Set<Generic> generics() {
        return Misc.union(firstType.generics(), secondType.generics());
      }

      @Override
      public String toString() {
        return "<%s,%s>".formatted(firstType, secondType);
      }
    }

    record Sequence(Type type) implements Composed {
      @Override
      public boolean canTakeValuesOf(Type other) {
        if (other instanceof Sequence(Type otherType)) {
          return type.canTakeValuesOf(otherType);
        }
        return false;
      }

      @Override
      public boolean matches(Object o) {
        if (o instanceof List<?> list) {
          return list.stream().allMatch(lO -> type().matches(lO));
        }
        return false;
      }

      @Override
      public Set<Generic> generics() {
        return type.generics();
      }

      @Override
      public String toString() {
        return "[%s]".formatted(type);
      }
    }

    static Pair pair(Type firstType, Type secondType) {
      return new Pair(firstType, secondType);
    }

    static Sequence sequence(Type type) {
      return new Sequence(type);
    }
  }

  record Generic(String name) implements Type {
    public static Generic of(String name) {
      return new Generic(name);
    }

    @Override
    public boolean canTakeValuesOf(Type other) {
      return true;
    }

    @Override
    public boolean matches(Object o) {
      return true;
    }

    @Override
    public Set<Generic> generics() {
      return Set.of(this);
    }

    @Override
    public String toString() {
      return name;
    }
  }

  boolean canTakeValuesOf(Type other);

  Set<Type.Generic> generics();

  boolean matches(Object o);

  Map<Type.Generic, Type> resolveGenerics(Type concreteType);

  default boolean isGenerics() {
    return !generics().isEmpty();
  }
}
