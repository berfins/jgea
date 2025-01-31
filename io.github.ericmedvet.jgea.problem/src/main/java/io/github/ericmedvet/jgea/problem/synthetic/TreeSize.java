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

package io.github.ericmedvet.jgea.problem.synthetic;

import io.github.ericmedvet.jgea.core.problem.ComparableQualityBasedProblem;
import io.github.ericmedvet.jgea.core.representation.grammar.string.GrammarBasedProblem;
import io.github.ericmedvet.jgea.core.representation.grammar.string.StringGrammar;
import io.github.ericmedvet.jgea.core.representation.tree.Tree;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class TreeSize implements GrammarBasedProblem<Boolean, Tree<Boolean>>, ComparableQualityBasedProblem<Tree<Boolean>, Double> {

  private static final Function<Tree<Boolean>, Double> FITNESS_FUNCTION = t -> 1d / (double) t.size();
  private final StringGrammar<Boolean> grammar;

  public TreeSize(int nonTerminals, int terminals) {
    this.grammar = new StringGrammar<>();
    grammar.setStartingSymbol(false);
    grammar.rules().put(false, List.of(r(nonTerminals, false), r(terminals, true)));
  }

  @SafeVarargs
  private static <T> List<T> r(int n, T... ts) {
    List<T> list = new ArrayList<>(n * ts.length);
    for (int i = 0; i < n; i++) {
      list.addAll(List.of(ts));
    }
    return list;
  }

  @Override
  public StringGrammar<Boolean> grammar() {
    return grammar;
  }

  @Override
  public Function<Tree<Boolean>, Tree<Boolean>> solutionMapper() {
    return Function.identity();
  }

  @Override
  public Function<Tree<Boolean>, Double> qualityFunction() {
    return FITNESS_FUNCTION;
  }
}
