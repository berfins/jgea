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

import io.github.ericmedvet.jnb.core.Cacheable;
import io.github.ericmedvet.jnb.core.Discoverable;
import io.github.ericmedvet.jnb.core.Param;
import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;

@Discoverable(prefixTemplate = "ea.problem|p.dataset|d.numerical|num")
public class NumericalDatasets {
  private NumericalDatasets() {
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static Supplier<NumericalDatasetOLD> empty(
      @Param("xVars") List<String> xVarNames,
      @Param("yVars") List<String> yVarNames
  ) {
    return () -> new ListNumericalDataset(List.of(), xVarNames, yVarNames);
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static Supplier<NumericalDatasetOLD> fromFile(
      @Param("filePath") String filePath,
      @Param(
          value = "folds", dIs = {0}) List<Integer> folds,
      @Param(value = "nFolds", dI = 1) int nFolds,
      @Param(value = "xVarNamePattern", dS = "x.*") String xVarNamePattern,
      @Param(value = "yVarNamePattern", dS = "y.*") String yVarNamePattern
  ) {
    return () -> {
      try {
        return new LazyNumericalDataset(filePath, xVarNamePattern, yVarNamePattern).folds(folds, nFolds);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    };
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static Supplier<NumericalDatasetOLD> fromProblem(
      @Param("problem") UnivariateRegressionProblemOLD<UnivariateRegressionFitnessOLD> problem
  ) {
    return () -> problem.qualityFunction().getDataset();
  }
}
