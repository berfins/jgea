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

import io.github.ericmedvet.jgea.core.problem.TotalOrderQualityBasedProblem;
import io.github.ericmedvet.jgea.core.util.IndexedProvider;
import java.util.Comparator;

public interface TextFlaggingProblem extends ClassificationProblem<String, TextFlaggingProblem.Label>, TotalOrderQualityBasedProblem<Classifier<String, TextFlaggingProblem.Label>, Double> {

  enum Label {
    FOUND, NOT_FOUND
  }

  static TextFlaggingProblem from(
      ClassificationFitness<String, Label> qualityFunction,
      ClassificationFitness<String, Label> validationQualityFunction
  ) {
    record HardTextFlaggingProblem(
        ClassificationFitness<String, Label> qualityFunction,
        ClassificationFitness<String, Label> validationQualityFunction
    ) implements TextFlaggingProblem {}
    return new HardTextFlaggingProblem(qualityFunction, validationQualityFunction);
  }

  static TextFlaggingProblem from(
      ClassificationFitness.Metric metric,
      IndexedProvider<ClassificationFitness.Example<String, Label>> caseProvider,
      IndexedProvider<ClassificationFitness.Example<String, Label>> validationCaseProvider
  ) {
    return from(
        ClassificationFitness.from(metric, caseProvider),
        ClassificationFitness.from(metric, validationCaseProvider)
    );
  }

  @Override
  default Classifier<String, Label> example() {
    return s -> Label.NOT_FOUND;
  }

  @Override
  default Comparator<Double> totalOrderComparator() {
    return Double::compareTo;
  }

}
