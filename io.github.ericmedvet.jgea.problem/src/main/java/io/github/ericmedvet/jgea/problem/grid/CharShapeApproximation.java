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

package io.github.ericmedvet.jgea.problem.grid;

import io.github.ericmedvet.jgea.core.problem.ComparableQualityBasedProblem;
import io.github.ericmedvet.jgea.core.problem.ProblemWithExampleSolution;
import io.github.ericmedvet.jnb.datastructure.Grid;
import io.github.ericmedvet.jnb.datastructure.GridUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class CharShapeApproximation implements ComparableQualityBasedProblem<Grid<Character>, Double>, ProblemWithExampleSolution<Grid<Character>> {

  private final Grid<Character> target;
  private final Grid<Boolean> smoothedTarget;
  private final boolean translation;
  private final boolean smoothed;
  private final boolean weighted;
  private final int targetSize;

  public CharShapeApproximation(Grid<Character> target, boolean translation, boolean smoothed, boolean weighted) {
    this.target = target;
    this.translation = translation;
    this.smoothed = smoothed;
    smoothedTarget = target.map(c -> c == null ? null : true);
    targetSize = GridUtils.count(target, Objects::nonNull);
    this.weighted = weighted;
  }

  public CharShapeApproximation(
      String syntheticTargetName,
      boolean translation,
      boolean smoothed,
      boolean weighted
  ) throws IOException {
    this(loadGrid(syntheticTargetName), translation, smoothed, weighted);
  }

  private static Grid<Character> loadGrid(String syntheticTargetName) throws IOException {
    try (BufferedReader br = new BufferedReader(
        new InputStreamReader(
            Objects.requireNonNull(
                CharShapeApproximation.class.getResourceAsStream("/grids/" + syntheticTargetName + ".txt")
            )
        )
    )) {
      List<List<Character>> rows = br.lines()
          .map(l -> l.chars().mapToObj(c -> (char) c).toList())
          .toList();
      List<Integer> ws = rows.stream().map(List::size).toList();
      if (ws.stream().distinct().count() != 1) {
        throw new IllegalArgumentException("The file has an invalid shape: %s".formatted(ws));
      }
      int w = ws.getFirst();
      int h = ws.size();
      return Grid.create(w, h, (x, y) -> {
        Character c = rows.get(y).get(x);
        return c.equals('·') ? null : c;
      });
    }
  }

  @Override
  public Grid<Character> example() {
    return Grid.create(
        1,
        1,
        target.values()
            .stream()
            .filter(Objects::nonNull)
            .findAny()
            .orElseThrow(() -> new IllegalArgumentException("Empty target grid"))
    );
  }

  public Grid<Character> getTarget() {
    return target;
  }

  @Override
  public Function<Grid<Character>, Double> qualityFunction() {
    return grid -> {
      double sum = GridUtils.hammingDistance(grid, target, translation);
      if (smoothed) {
        sum = sum / 2d + GridUtils.hammingDistance(
            grid.map(c -> c == null ? null : true),
            smoothedTarget,
            translation
        ) / 2d;
      }
      if (weighted) {
        sum = sum / (double) Math.max(targetSize, GridUtils.count(grid, Objects::nonNull));
      }
      return sum;
    };
  }
}
