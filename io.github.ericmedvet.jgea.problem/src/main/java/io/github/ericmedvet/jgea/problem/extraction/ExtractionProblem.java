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

package io.github.ericmedvet.jgea.problem.extraction;

import io.github.ericmedvet.jgea.core.problem.SimpleMultiHomogeneousObjectiveProblem;
import io.github.ericmedvet.jgea.core.representation.graph.finiteautomata.Extractor;
import io.github.ericmedvet.jgea.core.util.IntRange;
import io.github.ericmedvet.jgea.core.util.Misc;
import io.github.ericmedvet.jnb.datastructure.Pair;
import java.util.*;
import java.util.function.Function;

public class ExtractionProblem<S> implements SimpleMultiHomogeneousObjectiveProblem<Extractor<S>, Double> {

  private final ExtractionFitness<S> fitnessFunction;
  private final ExtractionFitness<S> validationFunction;
  private final SequencedMap<String, Comparator<Double>> comparators;

  public ExtractionProblem(
      Set<Extractor<S>> extractors,
      List<S> sequence,
      int folds,
      int i,
      ExtractionFitness.Metric... metrics
  ) {
    Pair<List<S>, Set<IntRange>> validationDataset = buildDataset(extractors, sequence, folds, i, false);
    fitnessFunction = new ExtractionFitness<>(
        buildDataset(extractors, sequence, folds, i, true).first(),
        buildDataset(extractors, sequence, folds, i, true).second(),
        metrics
    );
    validationFunction = new ExtractionFitness<>(validationDataset.first(), validationDataset.second(), metrics);
    comparators = Arrays.stream(metrics)
        .collect(
            Misc.toSequencedMap(
                Enum::toString,
                m -> Double::compareTo
            )
        );
  }

  private static <S> Pair<List<S>, Set<IntRange>> buildDataset(
      Set<Extractor<S>> extractors,
      List<S> sequence,
      int folds,
      int i,
      boolean takeAllButIth
  ) {
    List<S> builtSequence = new ArrayList<>();
    double foldLength = (double) sequence.size() / (double) folds;
    for (int n = 0; n < folds; n++) {
      List<S> piece = sequence.subList(
          (int) Math.round(foldLength * (double) n),
          (n == folds - 1) ? sequence.size() : ((int) Math.round(foldLength * (double) (n + 1)))
      );
      if (takeAllButIth && (n != i)) {
        builtSequence.addAll(piece);
      } else if (!takeAllButIth && (n == i)) {
        builtSequence.addAll(piece);
      }
    }
    Set<IntRange> desiredExtractions = extractors.stream()
        .map(e -> e.extractNonOverlapping(builtSequence))
        .reduce(Misc::union)
        .orElse(Set.of());
    return new Pair<>(builtSequence, desiredExtractions);
  }

  public ExtractionFitness<S> validationQualityFunction() {
    return validationFunction;
  }

  @Override
  public SequencedMap<String, Comparator<Double>> comparators() {
    return comparators;
  }

  @Override
  public Function<Extractor<S>, Map<String, Double>> outcomeFunction() {
    return fitnessFunction::apply;
  }
}
