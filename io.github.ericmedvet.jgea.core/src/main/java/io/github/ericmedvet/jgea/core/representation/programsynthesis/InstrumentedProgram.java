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
import io.github.ericmedvet.jsdynsym.core.composed.Composed;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public interface InstrumentedProgram extends Program {
  record Outcome(List<Object> outputs, RunProfile profile, InstrumentedProgram instrumentedProgram) {
    @Override
    public String toString() {
      return "%s with %s".formatted(outputs, profile);
    }
  }

  Outcome runInstrumented(List<Object> inputs) throws ProgramExecutionException;

  @Override
  default List<Object> run(List<Object> inputs) throws ProgramExecutionException {
    return runInstrumented(inputs).outputs;
  }

  static InstrumentedProgram from(
      Function<List<Object>, Outcome> function,
      List<Type> inputTypes,
      List<Type> outputTypes
  ) {
    record HardInstrumentedProgram(
        Function<List<Object>, Outcome> function,
        List<Type> inputTypes,
        List<Type> outputTypes
    ) implements InstrumentedProgram, Composed<Function<List<Object>, Outcome>> {
      @Override
      public Outcome runInstrumented(List<Object> inputs) throws ProgramExecutionException {
        return Program.safelyRunFunction(function, inputs);
      }

      @Override
      public Function<List<Object>, Outcome> inner() {
        return function;
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
    return new HardInstrumentedProgram(function, inputTypes, outputTypes);
  }
}
