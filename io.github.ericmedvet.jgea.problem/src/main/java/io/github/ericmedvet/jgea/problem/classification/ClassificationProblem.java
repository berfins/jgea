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

package io.github.ericmedvet.jgea.problem.classification;

import io.github.ericmedvet.jgea.core.order.ParetoDominance;
import io.github.ericmedvet.jgea.core.order.PartialComparator;
import io.github.ericmedvet.jgea.core.problem.ProblemWithValidation;
import io.github.ericmedvet.jgea.problem.DataUtils;
import io.github.ericmedvet.jnb.datastructure.Pair;
import java.util.ArrayList;
import java.util.List;

public class ClassificationProblem<O, L extends Enum<L>> implements ProblemWithValidation<Classifier<O, L>, List<Double>> {

  // TODO fix this: currently, it enforces just one objective/metric
  private static final PartialComparator<List<Double>> COMPARATOR = ParetoDominance.build(Double.class, 1);

  private final ClassificationFitnessCL<O, L> fitnessFunction;
  private final ClassificationFitnessCL<O, L> validationFunction;
  private final List<Pair<O, L>> learningData;
  private final List<Pair<O, L>> validationData;

  public ClassificationProblem(
      List<Pair<O, L>> data,
      int folds,
      int i,
      ClassificationFitnessCL.Metric learningMetric,
      ClassificationFitnessCL.Metric validationMetric
  ) {
    validationData = DataUtils.fold(data, i, folds);
    learningData = new ArrayList<>(data);
    learningData.removeAll(validationData);
    fitnessFunction = new ClassificationFitnessCL<>(learningData, learningMetric);
    validationFunction = new ClassificationFitnessCL<>(validationData, validationMetric);
  }

  public List<Pair<O, L>> getLearningData() {
    return learningData;
  }

  public List<Pair<O, L>> getValidationData() {
    return validationData;
  }

  @Override
  public PartialComparator<List<Double>> qualityComparator() {
    return COMPARATOR;
  }

  @Override
  public ClassificationFitnessCL<O, L> qualityFunction() {
    return fitnessFunction;
  }

  @Override
  public ClassificationFitnessCL<O, L> validationQualityFunction() {
    return validationFunction;
  }
}
