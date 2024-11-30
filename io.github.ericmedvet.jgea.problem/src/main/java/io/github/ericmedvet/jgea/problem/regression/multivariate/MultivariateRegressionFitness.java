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

package io.github.ericmedvet.jgea.problem.regression.multivariate;

import io.github.ericmedvet.jgea.core.fitness.CaseBasedFitness;
import io.github.ericmedvet.jgea.core.representation.NamedMultivariateRealFunction;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MultivariateRegressionFitness implements CaseBasedFitness<NamedMultivariateRealFunction, Map<String, Double>, Map<String, Double>, Double> {
  private final NumericalDatasetOLD dataset;
  private final UnivariateRegressionFitnessOLD.Metric metric;

  private Map<String, List<Double>> actualYs;

  public MultivariateRegressionFitness(NumericalDatasetOLD dataset, UnivariateRegressionFitnessOLD.Metric metric) {
    this.dataset = dataset;
    this.metric = metric;
    actualYs = null;
  }

  @Override
  public Function<List<Map<String, Double>>, Double> aggregateFunction() {
    return outputs -> {
      if (actualYs == null) {
        actualYs = dataset.yVarNames()
            .stream()
            .collect(
                Collectors.toMap(
                    yName -> yName,
                    yName -> IntStream.range(0, dataset.size())
                        .mapToObj(
                            i -> dataset.namedExampleProvider()
                                .apply(i)
                                .y()
                                .get(yName)
                        )
                        .toList()
                )
            );
      }
      Map<String, List<Double>> predictedYs = dataset.yVarNames()
          .stream()
          .collect(
              Collectors.toMap(
                  yName -> yName,
                  yName -> outputs.stream().map(o -> o.get(yName)).toList()
              )
          );
      return predictedYs.entrySet()
          .stream()
          .mapToDouble(e -> metric.apply(UnivariateRegressionFitnessOLD.pairs(e.getValue(), actualYs.get(e.getKey()))))
          .average()
          .orElse(Double.NaN);
    };
  }

  @Override
  public BiFunction<NamedMultivariateRealFunction, Map<String, Double>, Map<String, Double>> caseFunction() {
    return NamedMultivariateRealFunction::compute;
  }

  @Override
  public IntFunction<Map<String, Double>> caseProvider() {
    return i -> dataset.namedExampleProvider().apply(i).x();
  }

  @Override
  public int nOfCases() {
    return dataset.size();
  }

  public NumericalDatasetOLD getDataset() {
    return dataset;
  }

  public UnivariateRegressionFitnessOLD.Metric getMetric() {
    return metric;
  }
}
