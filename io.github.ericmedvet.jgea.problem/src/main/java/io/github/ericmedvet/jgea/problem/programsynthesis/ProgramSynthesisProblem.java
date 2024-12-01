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

import io.github.ericmedvet.jgea.core.fitness.ExampleBasedFitness;
import io.github.ericmedvet.jgea.core.problem.ExampleBasedProblem;
import io.github.ericmedvet.jgea.core.problem.ProblemWithExampleSolution;
import io.github.ericmedvet.jgea.core.problem.TotalOrderQualityBasedProblem;
import io.github.ericmedvet.jgea.core.representation.programsynthesis.Program;
import io.github.ericmedvet.jgea.core.util.IndexedProvider;
import java.util.Comparator;
import java.util.List;

public interface ProgramSynthesisProblem extends ExampleBasedProblem<Program, List<Object>, List<Object>, ProgramSynthesisFitness.Outcome, Double>, TotalOrderQualityBasedProblem<Program, Double>, ProblemWithExampleSolution<Program> {

  @Override
  ProgramSynthesisFitness qualityFunction();

  @Override
  ProgramSynthesisFitness validationQualityFunction();

  static ProgramSynthesisProblem from(
      Program example,
      ProgramSynthesisFitness qualityFunction,
      ProgramSynthesisFitness validationQualityFunction
  ) {
    record HardProgramSynthesisProblem(
        Program example,
        ProgramSynthesisFitness qualityFunction,
        ProgramSynthesisFitness validationQualityFunction
    ) implements ProgramSynthesisProblem {}
    return new HardProgramSynthesisProblem(example, qualityFunction, validationQualityFunction);
  }

  static ProgramSynthesisProblem from(
      ProgramSynthesisFitness.Metric metric,
      ProgramSynthesisFitness.Dissimilarity dissimilarity,
      Program example,
      IndexedProvider<ExampleBasedFitness.Example<List<Object>, List<Object>>> caseProvider,
      IndexedProvider<ExampleBasedFitness.Example<List<Object>, List<Object>>> validationCaseProvider
  ) {
    return from(
        example,
        ProgramSynthesisFitness.from(metric, dissimilarity, example.outputTypes(), caseProvider),
        ProgramSynthesisFitness.from(metric, dissimilarity, example.outputTypes(), validationCaseProvider)
    );
  }

  static ProgramSynthesisProblem from(
      Program target,
      ProgramSynthesisFitness.Metric metric,
      ProgramSynthesisFitness.Dissimilarity dissimilarity,
      IndexedProvider<List<Object>> caseProvider,
      IndexedProvider<List<Object>> validationCaseProvider
  ) {
    return from(
        metric,
        dissimilarity,
        target,
        caseProvider.then(inputs -> new ExampleBasedFitness.Example<>(inputs, target.safelyRun(inputs))),
        validationCaseProvider.then(inputs -> new ExampleBasedFitness.Example<>(inputs, target.safelyRun(inputs)))
    );
  }

  @Override
  default Comparator<Double> totalOrderComparator() {
    return Double::compareTo;
  }

}
