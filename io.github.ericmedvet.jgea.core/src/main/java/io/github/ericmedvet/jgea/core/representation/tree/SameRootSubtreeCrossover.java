/*-
 * ========================LICENSE_START=================================
 * jgea-core
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

package io.github.ericmedvet.jgea.core.representation.tree;

import io.github.ericmedvet.jgea.core.operator.Crossover;
import io.github.ericmedvet.jgea.core.util.Misc;
import java.util.List;
import java.util.Set;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;

public class SameRootSubtreeCrossover<N> implements Crossover<Tree<N>> {

  private final int maxHeight;

  public SameRootSubtreeCrossover(int maxHeight) {
    this.maxHeight = maxHeight;
  }

  @Override
  public Tree<N> recombine(Tree<N> parent1, Tree<N> parent2, RandomGenerator random) {
    List<Tree<N>> subtrees1 = parent1.topSubtrees();
    List<Tree<N>> subtrees2 = parent2.topSubtrees();
    Set<N> roots = subtrees1.stream().map(Tree::content).collect(Collectors.toSet());
    roots.retainAll(subtrees1.stream().map(Tree::content).collect(Collectors.toSet()));
    subtrees1 = subtrees1.stream()
        .filter(t -> roots.contains(t.content()))
        .distinct()
        .toList();
    subtrees2 = subtrees2.stream()
        .filter(t -> roots.contains(t.content()))
        .distinct()
        .toList();
    subtrees1 = Misc.shuffle(subtrees1, random);
    subtrees2 = Misc.shuffle(subtrees2, random);
    for (Tree<N> subtree1 : subtrees1) {
      for (Tree<N> subtree2 : subtrees2) {
        if ((subtree1.content().equals(subtree2.content())) && (subtree1.depth() + subtree2.height() <= maxHeight)) {
          return TreeUtils.replaceFirst(parent1, subtree1, subtree2);
        }
      }
    }
    return Tree.copyOf(parent1);
  }
}
