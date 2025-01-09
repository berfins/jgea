/*-
 * ========================LICENSE_START=================================
 * jgea-problem
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
package io.github.ericmedvet.jgea.problem.programsynthesis;

import io.github.ericmedvet.jgea.core.distance.Distance;
import io.github.ericmedvet.jgea.core.problem.ProblemWithExampleSolution;
import io.github.ericmedvet.jgea.core.problem.SimpleEBMOProblem;
import io.github.ericmedvet.jgea.core.representation.programsynthesis.InstrumentedProgram;
import io.github.ericmedvet.jgea.core.representation.programsynthesis.Program;
import io.github.ericmedvet.jgea.core.representation.programsynthesis.ProgramExecutionException;
import io.github.ericmedvet.jgea.core.representation.programsynthesis.RunProfile;
import io.github.ericmedvet.jgea.core.representation.programsynthesis.ttpn.Network;
import io.github.ericmedvet.jgea.core.representation.programsynthesis.type.Type;
import io.github.ericmedvet.jgea.core.util.Misc;
import io.github.ericmedvet.jnb.datastructure.TriFunction;
import io.github.ericmedvet.jsdynsym.core.composed.Composed;
import java.util.List;
import java.util.Objects;
import java.util.SequencedMap;
import java.util.function.BiFunction;
import java.util.stream.IntStream;

public interface ProgramSynthesisProblem extends SimpleEBMOProblem<Program, List<Object>, InstrumentedProgram.Outcome, ProgramSynthesisProblem.Outcome, Double>, ProblemWithExampleSolution<Program> {

  record Outcome(List<Object> actual, InstrumentedProgram.Outcome executionOutcome) {}

  enum Metric implements BiFunction<List<Outcome>, Distance<List<Object>>, Double> {
    // TODO add those related to RunProfile
    FAIL_RATE(
        (outcomes, d) -> (double) outcomes.stream()
            .filter(outcome -> !Objects.equals(outcome.actual, outcome.executionOutcome.outputs()))
            .count() / (double) outcomes.size()
    ), AVG_RAW_DISSIMILARITY(
        (outcomes, d) -> outcomes.stream()
            .mapToDouble(outcome -> d.apply(outcome.actual, outcome.executionOutcome.outputs()))
            .average()
            .orElseThrow()
    ), EXCEPTION_ERROR_RATE(
        (outcomes, d) -> (double) outcomes.stream()
            .filter(outcome -> !Objects.equals(outcome.actual == null, outcome.executionOutcome.outputs() == null))
            .count() / (double) outcomes.size()
    ), TTPN_BLOCKED_OUTPUTS_RATE(
        (outcomes, d) -> Composed.deepest(outcomes.getFirst().executionOutcome().instrumentedProgram(), Network.class)
            .map(
                n -> (double) n.outputGates()
                    .keySet()
                    .stream()
                    .filter(gi -> n.isGateAutoBlocked(gi) || !n.isWiredToInput(gi))
                    .count() / (double) n.outputTypes().size()
            )
            .orElse(Double.NaN)
    ), TTPN_BLOCKED_GATES_RATE(
        (outcomes, d) -> Composed.deepest(outcomes.getFirst().executionOutcome().instrumentedProgram(), Network.class)
            .map(
                n -> (double) IntStream.range(0, n.gates().size())
                    .filter(n::isGateAutoBlocked)
                    .count() / (double) n.gates().size()
            )
            .orElse(Double.NaN)
    ), PROFILE_AVG_STEPS(
        (outcomes, d) -> outcomes.stream()
            .mapToDouble(outcome -> (double) outcome.executionOutcome.profile().states().size())
            .average()
            .orElse(Double.NaN)
    ), PROFILE_AVG_TOT_SIZE(
        (outcomes, d) -> outcomes.stream()
            .mapToDouble(outcome -> outcome.executionOutcome.profile().totSize())
            .average()
            .orElse(Double.NaN)
    );

    private final BiFunction<List<Outcome>, Distance<List<Object>>, Double> function;

    Metric(BiFunction<List<Outcome>, Distance<List<Object>>, Double> function) {
      this.function = function;
    }

    @Override
    public Double apply(List<Outcome> ys, Distance<List<Object>> d) {
      return function.apply(ys, d);
    }

    @Override
    public String toString() {
      return name().toLowerCase();
    }
  }

  List<Metric> metrics();

  List<Type> inputTypes();

  List<Type> outputTypes();

  Distance<List<Object>> outputDistance();

  @Override
  default SequencedMap<String, Objective<List<Outcome>, Double>> aggregateObjectives() {
    Distance<List<Object>> outputDistance = outputDistance();
    return metrics().stream()
        .collect(
            Misc.toSequencedMap(
                Enum::toString,
                m -> new Objective<>(outcomes -> m.apply(outcomes, outputDistance), Double::compareTo)
            )
        );
  }

  @Override
  default TriFunction<List<Object>, InstrumentedProgram.Outcome, InstrumentedProgram.Outcome, Outcome> errorFunction() {
    return (inputs, actualExecutionOutcome, executionOutcome) -> new Outcome(
        actualExecutionOutcome.outputs(),
        executionOutcome
    );
  }

  @Override
  default BiFunction<Program, List<Object>, InstrumentedProgram.Outcome> predictFunction() {
    return (ProgramSynthesisProblem::safelyExecute);
  }

  @Override
  default Program example() {
    return Program.from(inputs -> List.of(), inputTypes(), outputTypes());
  }

  static InstrumentedProgram.Outcome safelyExecute(Program program, List<Object> inputs) {
    if (program instanceof InstrumentedProgram instrumentedProgram) {
      try {
        return instrumentedProgram.runInstrumented(inputs);
      } catch (ProgramExecutionException e) {
        return new InstrumentedProgram.Outcome(null, new RunProfile(List.of()), instrumentedProgram);
      }
    }
    try {
      return new InstrumentedProgram.Outcome(program.run(inputs), new RunProfile(List.of()), null);
    } catch (ProgramExecutionException e) {
      return new InstrumentedProgram.Outcome(null, new RunProfile(List.of()), null);
    }
  }
}
