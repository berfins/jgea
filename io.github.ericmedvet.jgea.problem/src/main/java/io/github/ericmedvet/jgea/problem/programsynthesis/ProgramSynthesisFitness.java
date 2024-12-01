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
import io.github.ericmedvet.jgea.core.fitness.ExampleBasedFitness;
import io.github.ericmedvet.jgea.core.representation.programsynthesis.Program;
import io.github.ericmedvet.jgea.core.representation.programsynthesis.type.Type;
import io.github.ericmedvet.jgea.core.util.IndexedProvider;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public interface ProgramSynthesisFitness extends ExampleBasedFitness<Program, List<Object>, List<Object>, ProgramSynthesisFitness.Outcome, Double> {
  record Outcome(boolean exception, double distance) {}

  enum Metric { FAIL_RATE, AVG_DISSIMILARITY }

  enum Dissimilarity { RAW, NORMALIZED }

  Distance<List<Object>> outputsDistance();

  static ProgramSynthesisFitness from(
      Distance<List<Object>> outputsDistance,
      Function<List<Outcome>, Double> aggregateFunction,
      IndexedProvider<Example<List<Object>, List<Object>>> caseProvider
  ) {
    record HardProgramSynthesisFitness(
        Distance<List<Object>> outputsDistance,
        Function<List<Outcome>, Double> aggregateFunction,
        IndexedProvider<Example<List<Object>, List<Object>>> caseProvider
    ) implements ProgramSynthesisFitness {}
    return new HardProgramSynthesisFitness(outputsDistance, aggregateFunction, caseProvider);
  }

  static ProgramSynthesisFitness from(
      Metric metric,
      Dissimilarity dissimilarity,
      double maxDissimilarity,
      List<Type> types,
      IndexedProvider<Example<List<Object>, List<Object>>> caseProvider
  ) {
    Distance<List<Object>> outputsDistance = switch (dissimilarity) {
      case RAW -> new io.github.ericmedvet.jgea.problem.programsynthesis.Dissimilarity(types, maxDissimilarity);
      case NORMALIZED -> new NormalizedDissimilarity(
          types,
          maxDissimilarity,
          caseProvider.then(Example::output)
      );
    };
    Function<List<Outcome>, Double> aggregateFunction = switch (metric) {
      case FAIL_RATE -> os -> (double) os.stream().filter(o -> o.distance == 0).count() / (double) os.size();
      case AVG_DISSIMILARITY -> os -> os.stream().mapToDouble(Outcome::distance).average().orElseThrow();
    };
    return from(outputsDistance, aggregateFunction, caseProvider);
  }

  @Override
  default BiFunction<List<Object>, List<Object>, Outcome> errorFunction() {
    return (actualOutputs, predictedOutputs) -> new Outcome(
        predictedOutputs == null,
        outputsDistance().apply(actualOutputs, predictedOutputs)
    );
  }

  @Override
  default BiFunction<Program, List<Object>, List<Object>> predictFunction() {
    return Program::safelyRun;
  }
}
