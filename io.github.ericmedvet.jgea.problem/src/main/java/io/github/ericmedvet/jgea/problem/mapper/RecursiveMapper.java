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

package io.github.ericmedvet.jgea.problem.mapper;

import io.github.ericmedvet.jgea.core.representation.grammar.string.StringGrammar;
import io.github.ericmedvet.jgea.core.representation.grammar.string.ge.WeightedHierarchicalMapper;
import io.github.ericmedvet.jgea.core.representation.sequence.bit.BitString;
import io.github.ericmedvet.jgea.core.representation.tree.Tree;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class RecursiveMapper<T> extends WeightedHierarchicalMapper<T> {

  private final Tree<Element> optionChooser;
  private final Tree<Element> genoAssigner;
  private final int maxMappingDepth;

  public RecursiveMapper(
      Tree<Element> optionChooser,
      Tree<Element> genoAssigner,
      int maxMappingDepth,
      int maxDepth,
      StringGrammar<T> grammar
  ) {
    super(maxDepth, grammar);
    this.maxMappingDepth = maxMappingDepth;
    this.optionChooser = optionChooser;
    this.genoAssigner = genoAssigner;
  }

  @Override
  public Tree<T> apply(BitString genotype) {
    AtomicInteger mappingGlobalCounter = new AtomicInteger();
    AtomicInteger finalizationGlobalCounter = new AtomicInteger();
    return mapRecursively(grammar.startingSymbol(), genotype, mappingGlobalCounter, finalizationGlobalCounter, 0);
  }

  private Tree<T> mapRecursively(
      T symbol,
      BitString genotype,
      AtomicInteger mappingGlobalCounter,
      AtomicInteger finalizationGlobalCounter,
      int depth
  ) {
    Tree<T> tree = Tree.of(symbol);
    if (!grammar.rules().containsKey(symbol)) {
      return tree;
    }
    if (depth >= maxMappingDepth) {
      List<Integer> shortestOptionIndexTies = shortestOptionIndexesMap.get(symbol);
      List<T> shortestOption = grammar.rules()
          .get(symbol)
          .get(
              shortestOptionIndexTies.get(
                  finalizationGlobalCounter.getAndIncrement() % shortestOptionIndexTies.size()
              )
          );
      for (T optionSymbol : shortestOption) {
        tree.addChild(
            mapRecursively(
                optionSymbol,
                genotype,
                mappingGlobalCounter,
                finalizationGlobalCounter,
                depth + 1
            )
        );
      }
      return tree;
    }
    // choose option
    List<List<T>> options = grammar.rules().get(symbol);
    List<Double> expressivenesses = new ArrayList<>(options.size());
    for (List<T> option : options) {
      double expressiveness = 1d;
      for (T optionSymbol : option) {
        expressiveness = expressiveness * (double) weightsMap.getOrDefault(optionSymbol, 1);
      }
      expressivenesses.add(expressiveness);
    }
    int optionIndex = ((Double) MapperUtils.compute(
        optionChooser,
        genotype,
        expressivenesses,
        depth,
        mappingGlobalCounter
    ))
        .intValue();
    optionIndex = Math.min(optionIndex, options.size() - 1);
    optionIndex = Math.max(0, optionIndex);
    // split genotype
    expressivenesses.clear();
    for (T optionSymbol : options.get(optionIndex)) {
      expressivenesses.add((double) weightsMap.getOrDefault(optionSymbol, 1));
    }
    @SuppressWarnings("unchecked") List<BitString> pieces = ((List<BitString>) MapperUtils.compute(
        genoAssigner,
        genotype,
        expressivenesses,
        depth,
        mappingGlobalCounter
    ));
    for (int i = 0; i < options.get(optionIndex).size(); i++) {
      BitString piece;
      if (pieces.size() > i) {
        piece = pieces.get(i);
      } else {
        piece = new BitString(0);
      }
      tree.addChild(
          mapRecursively(
              options.get(optionIndex).get(i),
              piece,
              mappingGlobalCounter,
              finalizationGlobalCounter,
              depth + 1
          )
      );
    }
    return tree;
  }
}
