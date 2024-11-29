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

import io.github.ericmedvet.jgea.core.util.LinkedHashMultiset;
import io.github.ericmedvet.jgea.core.util.Multiset;
import io.github.ericmedvet.jnb.datastructure.Pair;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class ClassificationFitnessCL<O, L extends Enum<L>> extends CLListCaseBasedFitness<Classifier<O, L>, O, L, List<Double>> {

  private final List<Pair<O, L>> data;

  public ClassificationFitnessCL(List<Pair<O, L>> data, Metric errorMetric) {
    super(
        data.stream().map(Pair::first).toList(),
        Classifier::classify,
        getAggregator(data.stream().map(Pair::second).toList(), errorMetric)
    );
    this.data = data;
  }

  public enum Metric {
    CLASS_ERROR_RATE, ERROR_RATE, BALANCED_ERROR_RATE
  }

  private record ClassErrorRate<E extends Enum<E>>(
      List<E> actualLabels
  ) implements Function<List<E>, List<Pair<Integer, Integer>>> {

    @SuppressWarnings("unchecked")
    @Override
    public List<Pair<Integer, Integer>> apply(List<E> predictedLabels) {
      E protoLabel = actualLabels.getFirst();
      E[] allLabels = (E[]) protoLabel.getClass().getEnumConstants();
      Multiset<E> counts = new LinkedHashMultiset<>();
      Multiset<E> errors = new LinkedHashMultiset<>();
      for (int i = 0; i < actualLabels.size(); i++) {
        counts.add(actualLabels.get(i));
        if (!actualLabels.get(i).equals(predictedLabels.get(i))) {
          errors.add(actualLabels.get(i));
        }
      }
      List<Pair<Integer, Integer>> pairs = new ArrayList<>(allLabels.length);
      for (E currentLabel : allLabels) {
        pairs.add(new Pair<>(errors.count(currentLabel), counts.count(currentLabel)));
      }
      return pairs;
    }
  }

  private static <E extends Enum<E>> Function<List<E>, List<Double>> getAggregator(
      List<E> actualLabels,
      Metric metric
  ) {
    final ClassErrorRate<E> classErrorRate = new ClassErrorRate<>(actualLabels);
    if (metric.equals(Metric.CLASS_ERROR_RATE)) {
      return (List<E> predictedLabels) -> {
        List<Pair<Integer, Integer>> pairs = classErrorRate.apply(predictedLabels);
        return pairs.stream()
            .map(p -> ((double) p.first() / (double) p.second()))
            .toList();
      };
    }
    if (metric.equals(Metric.ERROR_RATE)) {
      return (List<E> predictedLabels) -> {
        List<Pair<Integer, Integer>> pairs = classErrorRate.apply(predictedLabels);
        int errors = pairs.stream()
            .map(Pair::first)
            .mapToInt(Integer::intValue)
            .sum();
        int count = pairs.stream()
            .map(Pair::second)
            .mapToInt(Integer::intValue)
            .sum();
        return List.of((double) errors / (double) count);
      };
    }
    if (metric.equals(Metric.BALANCED_ERROR_RATE)) {
      return (List<E> predictedLabels) -> {
        List<Pair<Integer, Integer>> pairs = classErrorRate.apply(predictedLabels);
        return List.of(
            pairs.stream()
                .map(p -> ((double) p.first() / (double) p.second()))
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(Double.NaN)
        );
      };
    }
    return null;
  }

  public ClassificationFitnessCL<O, L> changeMetric(Metric metric) {
    return new ClassificationFitnessCL<>(data, metric);
  }
}
