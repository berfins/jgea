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
package io.github.ericmedvet.jgea.core.representation.programsynthesis;

import io.github.ericmedvet.jgea.core.representation.programsynthesis.type.Type;
import io.github.ericmedvet.jnb.datastructure.NamedFunction;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public interface Program {
  List<Type> inputTypes();

  List<Type> outputTypes();

  List<Object> run(List<Object> inputs);

  static Program from(
      Function<List<Object>, List<Object>> function,
      List<Type> inputTypes,
      List<Type> outputTypes
  ) {
    record HardProgram(
        Function<List<Object>, List<Object>> function,
        List<Type> inputTypes,
        List<Type> outputTypes
    ) implements Program {
      @Override
      public List<Object> run(List<Object> inputs) {
        return function.apply(inputs);
      }

      @Override
      public String toString() {
        return "%s(%s)->(%s)".formatted(
            NamedFunction.name(function),
            inputTypes.stream().map(Object::toString).collect(Collectors.joining(",")),
            outputTypes.stream().map(Object::toString).collect(Collectors.joining(","))
        );
      }
    }
    return new HardProgram(function, inputTypes, outputTypes);
  }
}
