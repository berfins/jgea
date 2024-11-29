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

import io.github.ericmedvet.jgea.problem.extraction.string.RegexGrammar;
import io.github.ericmedvet.jnb.datastructure.Pair;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BinaryTextFlaggingProblemOLD extends GrammarBasedTextFlaggingProblem {

  private static final String[] REGEXES = new String[]{"101010...010101", "11111...11111", "(11110000)++"};
  private static final String ALPHABET = "01";

  public BinaryTextFlaggingProblemOLD(
      int size,
      int length,
      long seed,
      int folds,
      int i,
      ClassificationFitnessCL.Metric learningErrorMetric,
      ClassificationFitnessCL.Metric validationErrorMetric,
      RegexGrammar.Option... options
  ) {
    super(
        new TreeSet<>(ALPHABET.chars().mapToObj(c -> (char) c).collect(Collectors.toSet())),
        new LinkedHashSet<>(Arrays.asList(options)),
        buildData(REGEXES, ALPHABET, length, size, new Random(seed)),
        folds,
        i,
        learningErrorMetric,
        validationErrorMetric
    );
  }

  private static List<Pair<String, Label>> buildData(
      String[] regexes,
      String alphabet,
      int length,
      int size,
      Random random
  ) {
    List<String> positives = new ArrayList<>();
    List<String> negatives = new ArrayList<>();
    List<Pattern> patterns = Stream.of(regexes).map(Pattern::compile).toList();
    while ((positives.size() < size) || (negatives.size() < size)) {
      StringBuilder sb = new StringBuilder();
      while (sb.length() < length) {
        sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
      }
      if (patterns.stream().anyMatch((Pattern p) -> (p.matcher(sb).find()))) {
        if (positives.size() < size) {
          positives.add(sb.toString());
        }
      } else {
        if (negatives.size() < size) {
          negatives.add(sb.toString());
        }
      }
    }
    // return
    List<Pair<String, Label>> data = new ArrayList<>();
    data.addAll(positives.stream().map(s -> new Pair<>(s, Label.FOUND)).toList());
    data.addAll(negatives.stream().map(s -> new Pair<>(s, Label.NOT_FOUND)).toList());
    return data;
  }
}
