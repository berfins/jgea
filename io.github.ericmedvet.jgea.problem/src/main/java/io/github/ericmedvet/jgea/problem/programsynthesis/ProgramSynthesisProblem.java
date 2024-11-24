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
import io.github.ericmedvet.jgea.core.problem.ProblemWithValidation;
import io.github.ericmedvet.jgea.core.problem.TotalOrderQualityBasedProblem;
import io.github.ericmedvet.jgea.core.representation.programsynthesis.Program;
import io.github.ericmedvet.jgea.core.representation.programsynthesis.ProgramExecutionException;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.random.RandomGenerator;
import java.util.stream.Stream;

public class ProgramSynthesisProblem implements TotalOrderQualityBasedProblem<Program, Double>, ProblemWithValidation<Program, Double>, ProblemWithExampleSolution<Program> {

  private final Program targetProgram;
  private final ProgramSynthesisFitness fitness;
  private final ProgramSynthesisFitness validationFitness;

  public ProgramSynthesisProblem(
      int nOfCases,
      int nOfValidationCases,
      double maxExceptionCaseRate,
      DataFactory dataFactory,
      RandomGenerator rnd,
      Program targetProgram,
      ProgramSynthesisFitness.Metric metric
  ) {
    this(
        buildRandomCases(dataFactory, targetProgram, nOfCases, maxExceptionCaseRate, rnd),
        buildRandomCases(dataFactory, targetProgram, nOfValidationCases, maxExceptionCaseRate, rnd),
        targetProgram,
        metric
    );
  }

  private static List<List<Object>> buildRandomCases(
      DataFactory df,
      Program targetProgram,
      int n,
      double maxExceptionCaseRate,
      RandomGenerator rnd
  ) {
    return Stream.concat(
        Stream.generate(() -> buildRandomCase(df, targetProgram, false, rnd))
            .limit((long) (n * (1d - maxExceptionCaseRate))),
        Stream.generate(() -> buildRandomCase(df, targetProgram, true, rnd)).limit((long) (n * maxExceptionCaseRate))
    ).toList();
  }

  private static List<Object> buildRandomCase(
      DataFactory df,
      Program targetProgram,
      boolean acceptException,
      RandomGenerator rnd
  ) {
    List<Object> inputs = targetProgram.inputTypes().stream().map(t -> df.apply(t, rnd)).toList();
    if (acceptException) {
      return inputs;
    }
    try {
      targetProgram.run(inputs);
      return inputs;
    } catch (ProgramExecutionException e) {
      return buildRandomCase(df, targetProgram, acceptException, rnd);
    }
  }

  public ProgramSynthesisProblem(
      List<List<Object>> cases,
      List<List<Object>> validationCases,
      Program targetProgram,
      ProgramSynthesisFitness.Metric metric
  ) {
    this.targetProgram = targetProgram;
    fitness = new ProgramSynthesisFitness(targetProgram, cases, metric);
    validationFitness = new ProgramSynthesisFitness(targetProgram, validationCases, metric);
  }

  @Override
  public Program example() {
    return targetProgram;
  }

  @Override
  public Function<Program, Double> validationQualityFunction() {
    return validationFitness;
  }

  @Override
  public Comparator<Double> totalOrderComparator() {
    return (v1, v2) -> Double.compare(v2, v1);
  }

  @Override
  public Function<Program, Double> qualityFunction() {
    return fitness;
  }
}
