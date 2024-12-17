/*-
 * ========================LICENSE_START=================================
 * jgea-experimenter
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
package io.github.ericmedvet.jgea.experimenter.builders;

import io.github.ericmedvet.jgea.core.representation.programsynthesis.Program;
import io.github.ericmedvet.jgea.core.util.IntRange;
import io.github.ericmedvet.jgea.problem.programsynthesis.DataFactory;
import io.github.ericmedvet.jgea.problem.programsynthesis.Problems;
import io.github.ericmedvet.jgea.problem.programsynthesis.ProgramSynthesisFitness;
import io.github.ericmedvet.jgea.problem.programsynthesis.ProgramSynthesisProblem;
import io.github.ericmedvet.jnb.core.Cacheable;
import io.github.ericmedvet.jnb.core.Discoverable;
import io.github.ericmedvet.jnb.core.Param;
import io.github.ericmedvet.jnb.datastructure.DoubleRange;
import java.util.List;
import java.util.random.RandomGenerator;

@Discoverable(prefixTemplate = "ea.problem|p.programSynthesis|ps")
public class ProgramSynthesisProblems {

  private ProgramSynthesisProblems() {
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static ProgramSynthesisProblem synthetic(
      @Param("name") String name,
      @Param(value = "metric", dS = "avg_dissimilarity") ProgramSynthesisFitness.Metric metric,
      @Param(value = "dissimilarity", dS = "normalized") ProgramSynthesisFitness.Dissimilarity dissimilarity,
      @Param(value = "maxDissimilarity", dD = 100d) double maxDissimilarity,
      @Param(value = "randomGenerator", dNPM = "m.defaultRG()") RandomGenerator randomGenerator,
      @Param(value = "nOfCases", dI = 10) int nOfCases,
      @Param(value = "nOfValidationCases", dI = 100) int nOfValidationCases,
      @Param(value = "maxExceptionRate", dD = 0.1d) double maxExceptionRate,
      @Param(value = "ints", dIs = {1, 2, 3, 5, 10}) List<Integer> ints,
      @Param(value = "reals", dDs = {1d, 2d, 3d, 1.5, 2.5, 3.14}) List<Double> reals,
      @Param(value = "strings", dSs = {"cat", "dog", "Hello World!", "mummy"}) List<String> strings,
      @Param(value = "intRange", dNPM = "m.range(min=-10;max=100)") DoubleRange intRange,
      @Param(value = "realRange", dNPM = "m.range(min=-10;max=10)") DoubleRange realRange,
      @Param(value = "stringLengthRange", dNPM = "m.range(min=2;max=20)") DoubleRange stringLengthRange,
      @Param(value = "sequenceSizeRange", dNPM = "m.range(min=1;max=8)") DoubleRange sequenceSizeRange
  ) {
    try {
      Program tProgram = Program.from(Problems.class.getMethod(name, List.class, List.class));
      DataFactory dataFactory = new DataFactory(
          ints,
          reals,
          strings,
          new IntRange((int) intRange.min(), (int) intRange.max()),
          realRange,
          new IntRange((int) stringLengthRange.min(), (int) stringLengthRange.max()),
          new IntRange((int) sequenceSizeRange.min(), (int) sequenceSizeRange.max())
      );
      return ProgramSynthesisProblem.from(
          tProgram,
          metric,
          dissimilarity,
          maxDissimilarity,
          dataFactory,
          randomGenerator,
          nOfCases,
          nOfValidationCases,
          maxExceptionRate
      );
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
  }

}
