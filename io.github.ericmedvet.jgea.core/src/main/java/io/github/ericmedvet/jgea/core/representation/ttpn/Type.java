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

import java.util.List;

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
    public boolean matches(Object o) {
      return javaClass.isInstance(o);
    }
  }

  interface Composed extends Type {
    record Pair(Type firstType, Type secondType) implements Composed {
      @Override
      public boolean matches(Object o) {
        if (o instanceof List<?> list) {
          if (list.size()==2) {
            if (firstType.matches(list.getFirst())) {
              return secondType.matches(list.getLast());
            }
          }
        }
        return false;
      }
    }
    record Sequence(Type type) implements Composed {
      @Override
      public boolean matches(Object o) {
        if (o instanceof List<?> list) {
          return list.stream().allMatch(type::matches);
        }
        return false;
      }
    }
  }

  boolean matches(Object o);

}
