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

import io.github.ericmedvet.jgea.core.problem.ProblemWithExampleSolution;
import io.github.ericmedvet.jgea.core.problem.TotalOrderQualityBasedProblem;
import io.github.ericmedvet.jgea.core.representation.programsynthesis.Program;
import io.github.ericmedvet.jgea.core.util.IndexedProvider;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.random.RandomGenerator;

public interface ProgramSynthesisProblem extends OLDExampleBasedProblem<Program, List<Object>, List<Object>, ProgramSynthesisFitness.Outcome, Double>, TotalOrderQualityBasedProblem<Program, Double>, ProblemWithExampleSolution<Program> {

  @Override
  ProgramSynthesisFitness qualityFunction();

  @Override
  ProgramSynthesisFitness validationQualityFunction();

  private static List<List<Object>> buildInputs(
      int n,
      double maxExceptionRate,
      Program program,
      DataFactory dataFactory,
      RandomGenerator rnd
  ) {
    List<List<Object>> cases = new ArrayList<>();
    while (cases.size() < (n * (1d - maxExceptionRate))) {
      List<Object> inputs = program.inputTypes().stream().map(t -> dataFactory.apply(t, rnd)).toList();
      if (program.safelyRun(inputs) != null) {
        cases.add(inputs);
      }
    }
    while (cases.size() < n) {
      cases.add(program.inputTypes().stream().map(t -> dataFactory.apply(t, rnd)).toList());
    }
    return cases;
  }

  static ProgramSynthesisProblem from(
      ProgramSynthesisFitness.Metric metric,
      ProgramSynthesisFitness.Dissimilarity dissimilarity,
      double maxDissimilarity,
      Program example,
      IndexedProvider<ExampleBasedFitness.Example<List<Object>, List<Object>>> caseProvider,
      IndexedProvider<ExampleBasedFitness.Example<List<Object>, List<Object>>> validationCaseProvider
  ) {
    return from(
        example,
        ProgramSynthesisFitness.from(metric, dissimilarity, maxDissimilarity, example.outputTypes(), caseProvider),
        ProgramSynthesisFitness.from(
            metric,
            dissimilarity,
            maxDissimilarity,
            example.outputTypes(),
            validationCaseProvider
        )
    );
  }

  static ProgramSynthesisProblem from(
      Program target,
      ProgramSynthesisFitness.Metric metric,
      ProgramSynthesisFitness.Dissimilarity dissimilarity,
      double maxDissimilarity,
      IndexedProvider<List<Object>> caseProvider,
      IndexedProvider<List<Object>> validationCaseProvider
  ) {
    return from(
        metric,
        dissimilarity,
        maxDissimilarity,
        target,
        caseProvider.then(inputs -> new ExampleBasedFitness.Example<>(inputs, target.safelyRun(inputs))),
        validationCaseProvider.then(inputs -> new ExampleBasedFitness.Example<>(inputs, target.safelyRun(inputs)))
    );
  }

  static ProgramSynthesisProblem from(
      Program target,
      ProgramSynthesisFitness.Metric metric,
      ProgramSynthesisFitness.Dissimilarity dissimilarity,
      double maxDissimilarity,
      DataFactory dataFactory,
      RandomGenerator rnd,
      int nOfCases,
      int nOfValidationCases,
      double maxExceptionRate
  ) {
    return from(
        target,
        metric,
        dissimilarity,
        maxDissimilarity,
        IndexedProvider.from(buildInputs(nOfCases, maxExceptionRate, target, dataFactory, rnd)),
        IndexedProvider.from(buildInputs(nOfValidationCases, maxExceptionRate, target, dataFactory, rnd))
    );
  }

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

  @Override
  default Comparator<Double> totalOrderComparator() {
    return Double::compareTo;
  }

}
