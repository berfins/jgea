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
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public interface NamedProgram extends Program {
  Map<String, Type> inputNamedTypes();

  Map<String, Type> outputNamedTypes();

  Map<String, Object> run(Map<String, Object> inputs) throws ProgramExecutionException;

  default List<String> inputNames() {
    return inputNamedTypes().keySet().stream().sorted().toList();
  }

  default List<String> outputNames() {
    return outputNamedTypes().keySet().stream().sorted().toList();
  }

  @Override
  default List<Type> inputTypes() {
    return inputNames().stream().map(n -> inputNamedTypes().get(n)).toList();
  }

  @Override
  default List<Type> outputTypes() {
    return outputNames().stream().map(n -> outputNamedTypes().get(n)).toList();
  }

  @Override
  default List<Object> run(List<Object> inputs) throws ProgramExecutionException {
    if (inputs.size() != inputNamedTypes().size()) {
      throw new ProgramExecutionException(
          "Wrong number of input arguments: %d expected, %d found".formatted(
              inputNamedTypes().size(),
              inputs.size()
          )
      );
    }
    List<String> iNames = inputNames();
    Map<String, Object> mInputs = IntStream.range(0, iNames.size())
        .boxed()
        .collect(
            Collectors.toMap(
                iNames::get,
                inputs::get
            )
        );
    Map<String, Object> mOutputs = run(mInputs);
    if (!mOutputs.keySet().containsAll(outputNames())) {
      throw new ProgramExecutionException(
          "Wrong output arguments: %s expected, %s found".formatted(
              outputNames(),
              mOutputs.keySet()
          )
      );
    }
    return outputNames().stream().map(mOutputs::get).toList();
  }

  static NamedProgram from(
      Function<Map<String, Object>, Map<String, Object>> function,
      Map<String, Type> inputNamedTypes,
      Map<String, Type> outputNamedTypes
  ) {
    record HardNamedProgram(
        Function<Map<String, Object>, Map<String, Object>> function,
        Map<String, Type> inputNamedTypes,
        Map<String, Type> outputNamedTypes,
        List<String> inputNames,
        List<String> outputNames
    ) implements NamedProgram {
      @Override
      public Map<String, Object> run(Map<String, Object> inputs) throws ProgramExecutionException {
        return Program.safelyRunFunction(function, inputs);
      }

      @Override
      public String toString() {
        return "%s(%s)->(%s)".formatted(
            NamedFunction.name(function),
            inputNames.stream().map(n -> "%s:%s".formatted(n, inputNamedTypes.get(n))).collect(Collectors.joining(",")),
            outputNames.stream()
                .map(n -> "%s:%s".formatted(n, outputNamedTypes.get(n)))
                .collect(Collectors.joining(","))
        );
      }
    }
    return new HardNamedProgram(
        function,
        inputNamedTypes,
        outputNamedTypes,
        inputNamedTypes.keySet().stream().sorted().toList(),
        outputNamedTypes.keySet().stream().sorted().toList()
    );
  }
}
