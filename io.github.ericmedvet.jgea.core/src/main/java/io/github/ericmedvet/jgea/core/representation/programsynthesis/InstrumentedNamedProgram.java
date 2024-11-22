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

public interface InstrumentedNamedProgram extends NamedProgram {
  record Outcome(Map<String, Object> outputs, RunProfile profile) {}

  Outcome runInstrumented(Map<String, Object> inputs) throws ProgramExecutionException;

  @Override
  default Map<String, Object> run(Map<String, Object> inputs) throws ProgramExecutionException {
    return runInstrumented(inputs).outputs;
  }

  static InstrumentedNamedProgram from(
      Function<Map<String, Object>, Outcome> function,
      Map<String, Type> inputNamedTypes,
      Map<String, Type> outputNamedTypes
  ) {
    record HardInstrumentedNamedProgram(
        Function<Map<String, Object>, Outcome> function,
        Map<String, Type> inputNamedTypes,
        Map<String, Type> outputNamedTypes,
        List<String> inputNames,
        List<String> outputNames
    ) implements InstrumentedNamedProgram {
      @Override
      public Outcome runInstrumented(Map<String, Object> inputs) throws ProgramExecutionException {
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
    return new HardInstrumentedNamedProgram(
        function,
        inputNamedTypes,
        outputNamedTypes,
        inputNamedTypes.keySet().stream().sorted().toList(),
        outputNamedTypes.keySet().stream().sorted().toList()
    );
  }
}
