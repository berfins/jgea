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

import io.github.ericmedvet.jgea.core.problem.QualityBasedProblem;
import io.github.ericmedvet.jgea.core.representation.grammar.string.GrammarBasedProblem;
import io.github.ericmedvet.jgea.core.representation.grammar.string.StringGrammar;
import io.github.ericmedvet.jgea.core.representation.tree.Tree;
import io.github.ericmedvet.jgea.problem.extraction.string.RegexGrammar;
import io.github.ericmedvet.jnb.datastructure.Pair;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class GrammarBasedTextFlaggingProblem extends TextFlaggingProblem implements GrammarBasedProblem<String, Classifier<String, TextFlaggingProblem.Label>>, QualityBasedProblem<Classifier<String, TextFlaggingProblem.Label>, List<Double>> {

  private final StringGrammar<String> grammar;
  private final Function<Tree<String>, Classifier<String, TextFlaggingProblem.Label>> solutionMapper;

  public GrammarBasedTextFlaggingProblem(
      Set<Character> alphabet,
      Set<RegexGrammar.Option> options,
      List<Pair<String, Label>> data,
      int folds,
      int i,
      ClassificationFitnessCL.Metric learningErrorMetric,
      ClassificationFitnessCL.Metric validationErrorMetric
  ) {
    super(data, folds, i, learningErrorMetric, validationErrorMetric);
    solutionMapper = (Tree<String> tree) -> {
      String regex = tree.leaves().stream().map(Tree::content).collect(Collectors.joining());
      return (Classifier<String, Label>) s -> {
        Matcher matcher = Pattern.compile(regex).matcher(s);
        return matcher.find() ? Label.FOUND : Label.NOT_FOUND;
      };
    };
    if (alphabet == null) {
      grammar = new RegexGrammar(data.stream().map(Pair::first).toList(), options);
    } else {
      grammar = new RegexGrammar(alphabet, options);
    }
  }

  @Override
  public StringGrammar<String> getGrammar() {
    return grammar;
  }

  @Override
  public Function<Tree<String>, Classifier<String, Label>> getSolutionMapper() {
    return solutionMapper;
  }
}
