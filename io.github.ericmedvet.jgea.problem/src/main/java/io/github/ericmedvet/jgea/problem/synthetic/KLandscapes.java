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
import io.github.ericmedvet.jnb.datastructure.DoubleRange;
import io.github.ericmedvet.jnb.datastructure.Pair;
import java.util.*;
import java.util.function.Function;

public class KLandscapes implements GrammarBasedProblem<String, Tree<String>>, ComparableQualityBasedProblem<Tree<String>, Double> {

  private static final int ARITY = 2;
  private static final DoubleRange V_RANGE = DoubleRange.SYMMETRIC_UNIT;
  private static final DoubleRange W_RANGE = DoubleRange.UNIT;
  private static final int N_TERMINALS = 4;
  private static final int N_NON_TERMINALS = 2;

  private final int k;
  private final StringGrammar<String> grammar;
  private final int arity;
  private final DoubleRange vRange;
  private final DoubleRange wRange;
  private final int nTerminals;
  private final int nNonTerminals;

  private final Function<Tree<String>, Double> fitnessFunction;
  private final Function<Tree<String>, Tree<String>> solutionMapper;

  public KLandscapes(int k) {
    this(k, ARITY, V_RANGE, W_RANGE, N_TERMINALS, N_NON_TERMINALS);
  }

  public KLandscapes(int k, int arity, DoubleRange vRange, DoubleRange wRange, int nTerminals, int nNonTerminals) {
    this.k = k;
    this.arity = arity;
    this.vRange = vRange;
    this.wRange = wRange;
    this.nTerminals = nTerminals;
    this.nNonTerminals = nNonTerminals;
    grammar = buildGrammar(nTerminals, nNonTerminals, arity);
    fitnessFunction = buildFitnessFunction();
    solutionMapper = buildSolutionMapper();
  }

  private static StringGrammar<String> buildGrammar(int nTerminals, int nNonTerminals, int arity) {
    StringGrammar<String> grammar = new StringGrammar<>();
    grammar.setStartingSymbol("N");
    grammar.rules().put("N", l(c(l("n"), r(arity, "N")), l("t")));
    List<List<String>> nonTerminalConstOptions = new ArrayList<>();
    for (int i = 0; i < nNonTerminals; i++) {
      nonTerminalConstOptions.add(l("n" + i));
    }
    grammar.rules().put("n", nonTerminalConstOptions);
    List<List<String>> terminalConstOptions = new ArrayList<>();
    for (int i = 0; i < nTerminals; i++) {
      terminalConstOptions.add(l("t" + i));
    }
    grammar.rules().put("t", terminalConstOptions);
    return grammar;
  }

  private static Function<Tree<String>, Tree<String>> buildSolutionMapper() {
    return KLandscapes::convertTree;
  }

  @SafeVarargs
  private static <T> List<T> c(List<T>... tss) {
    List<T> list = new ArrayList<>();
    for (List<T> ts : tss) {
      list.addAll(ts);
    }
    return list;
  }

  private static Tree<String> convertTree(Tree<String> original) {
    if (original == null) {
      return null;
    }
    Tree<String> tree = Tree.of(original.child(0).child(0).content());
    if (original.height() > 1) {
      // is a non terminal node
      for (Tree<String> orginalChild : original) {
        if (orginalChild.content().equals("N")) {
          tree.addChild(convertTree(orginalChild));
        }
      }
    }
    return tree;
  }

  protected static double f(Tree<String> tree, int k, Map<String, Double> v, Map<Pair<String, String>, Double> w) {
    return 1d / (1d + (double) Math.abs(k - tree.height())) * maxFK(tree, k, v, w);
  }

  protected static double fK(Tree<String> tree, int k, Map<String, Double> v, Map<Pair<String, String>, Double> w) {
    if (k == 0) {
      return v.get(tree.content());
    }
    double sum = v.get(tree.content());
    for (Tree<String> child : tree) {
      final double weight = w.get(new Pair<>(tree.content(), child.content()));
      final double innerFK = fK(child, k - 1, v, w);
      sum = sum + (1 + weight) * innerFK;
    }
    return sum;
  }

  @SafeVarargs
  private static <T> List<T> l(T... ts) {
    return Arrays.asList(ts);
  }

  protected static Tree<String> levelEqualTree(int[] indexes, int arity) {
    if (indexes.length == 1) {
      return Tree.of("t" + indexes[0]);
    }
    Tree<String> tree = Tree.of("n" + indexes[0]);
    for (int i = 0; i < arity; i++) {
      tree.addChild(levelEqualTree(Arrays.copyOfRange(indexes, 1, indexes.length), arity));
    }
    return tree;
  }

  protected static double maxFK(
      Tree<String> tree,
      int k,
      Map<String, Double> v,
      Map<Pair<String, String>, Double> w
  ) {
    double max = fK(tree, k, v, w);
    for (Tree<String> child : tree) {
      max = Math.max(max, maxFK(child, k, v, w));
    }
    return max;
  }

  protected static Tree<String> optimum(
      int k,
      int nTerminals,
      int nNonTerminals,
      int arity,
      Map<String, Double> v,
      Map<Pair<String, String>, Double> w
  ) {
    Tree<String> optimum = null;
    double maxFitness = Double.NEGATIVE_INFINITY;
    for (int d = 1; d <= k + 1; d++) {
      int[] indexes = new int[d]; // indexes of the (non)Terminals to be used. terminal is the last index.
      while (true) {
        Tree<String> tree = levelEqualTree(indexes, arity);
        double fitness = f(tree, k, v, w);
        if ((optimum == null) || (fitness > maxFitness)) {
          optimum = tree;
          maxFitness = fitness;
        }
        indexes[indexes.length - 1] = indexes[indexes.length - 1] + 1;
        for (int j = indexes.length - 1; j > 0; j--) {
          int threshold = (j == (indexes.length - 1)) ? nTerminals : nNonTerminals;
          if (indexes[j] == threshold) {
            indexes[j] = 0;
            indexes[j - 1] = indexes[j - 1] + 1;
          }
        }
        if (indexes[0] == nNonTerminals) {
          break;
        }
      }
    }
    return optimum;
  }

  @SafeVarargs
  private static <T> List<T> r(int n, T... ts) {
    List<T> list = new ArrayList<>(n * ts.length);
    for (int i = 0; i < n; i++) {
      list.addAll(l(ts));
    }
    return list;
  }

  private Function<Tree<String>, Double> buildFitnessFunction() {
    Random random = new Random(1L);
    final Map<String, Double> v = new LinkedHashMap<>();
    final Map<Pair<String, String>, Double> w = new LinkedHashMap<>();
    // fill v map
    for (int i = 0; i < nTerminals; i++) {
      v.put("t" + i, vRange.denormalize(random.nextDouble()));
    }
    for (int i = 0; i < nNonTerminals; i++) {
      v.put("n" + i, vRange.denormalize(random.nextDouble()));
    }
    // fill w map
    for (int j = 0; j < nNonTerminals; j++) {
      for (int i = 0; i < nTerminals; i++) {
        w.put(new Pair<>("n" + j, "t" + i), wRange.denormalize(random.nextDouble()));
      }
      for (int i = 0; i < nNonTerminals; i++) {
        w.put(new Pair<>("n" + j, "n" + i), wRange.denormalize(random.nextDouble()));
      }
    }
    // prepare fitness
    final double optimumFitness = f(optimum(k, nTerminals, nNonTerminals, arity, v, w), k, v, w);
    // build function
    return t -> (1d - f(t, k, v, w) / optimumFitness);
  }

  @Override
  public StringGrammar<String> grammar() {
    return grammar;
  }

  @Override
  public Function<Tree<String>, Tree<String>> solutionMapper() {
    return solutionMapper;
  }

  @Override
  public Function<Tree<String>, Double> qualityFunction() {
    return fitnessFunction;
  }
}
