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

import io.github.ericmedvet.jgea.core.fitness.ExampleBasedFitness;
import io.github.ericmedvet.jgea.core.problem.QualityBasedProblem;
import io.github.ericmedvet.jgea.core.representation.grammar.string.GrammarBasedProblem;
import io.github.ericmedvet.jgea.core.representation.grammar.string.StringGrammar;
import io.github.ericmedvet.jgea.core.representation.tree.Tree;
import io.github.ericmedvet.jgea.problem.extraction.string.RegexGrammar;
import io.github.ericmedvet.jgea.problem.regression.NumericalDataset;
import io.github.ericmedvet.jnb.datastructure.Pair;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public interface GrammarBasedTextFlaggingProblem extends TextFlaggingProblem, GrammarBasedProblem<String, Classifier<String, TextFlaggingProblem.Label>> {

  static TextFlaggingProblem from(
      ClassificationFitness<String, Label> qualityFunction,
      ClassificationFitness<String, Label> validationQualityFunction,
      StringGrammar<String> grammar
  ) {
    record HardGrammarBasedTextFlaggingProblem(
        ClassificationFitness<String, Label> qualityFunction,
        ClassificationFitness<String, Label> validationQualityFunction,
        StringGrammar<String> grammar,
        Function<Tree<String>, Classifier<String, TextFlaggingProblem.Label>> solutionMapper
    ) implements GrammarBasedTextFlaggingProblem {}
    return new HardGrammarBasedTextFlaggingProblem(
        qualityFunction,
        validationQualityFunction,
        grammar,
        tree -> {
          String regex =tree.leaves().stream().map(Tree::content).collect(Collectors.joining());
          return s -> {
            Matcher matcher = Pattern.compile(regex).matcher(s);
            return matcher.find() ? Label.FOUND : Label.NOT_FOUND;
          };
        }
    );
  }

  static TextFlaggingProblem from(
      ClassificationFitness.Metric metric,
      List<ExampleBasedFitness.Example<String, Label>> cases,
      List<ExampleBasedFitness.Example<String, Label>> validationCases,
      StringGrammar<String> grammar
  ) {
    return from(
        ClassificationFitness.from(metric,cases),
        ClassificationFitness.from(metric,validationCases),
        grammar
    );
  }

}
