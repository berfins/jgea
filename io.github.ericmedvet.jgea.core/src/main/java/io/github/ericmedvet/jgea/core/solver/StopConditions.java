/*-
 * ========================LICENSE_START=================================
 * jgea-core
 * %%
 * Copyright (C) 2018 - 2023 Eric Medvet
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

package io.github.ericmedvet.jgea.core.solver;

import io.github.ericmedvet.jgea.core.solver.state.POSetPopulationStateC;
import io.github.ericmedvet.jgea.core.solver.state.StateC;
import io.github.ericmedvet.jgea.core.util.Progress;
import java.util.function.Predicate;

public class StopConditions {

  private StopConditions() {}

  @SuppressWarnings("unused")
  public static ProgressBasedStopCondition<StateC> elapsedMillis(final long n) {
    return s -> new Progress(0, n, s.getElapsedMillis());
  }

  @SuppressWarnings("unused")
  public static ProgressBasedStopCondition<POSetPopulationStateC<?, ?, ?>> nOfBirths(final long n) {
    return s -> new Progress(0, n, s.getNOfBirths());
  }

  @SuppressWarnings("unused")
  public static ProgressBasedStopCondition<POSetPopulationStateC<?, ?, ?>> nOfFitnessEvaluations(
      final long n) {
    return s -> new Progress(0, n, s.getNOfFitnessEvaluations());
  }

  @SuppressWarnings("unused")
  public static ProgressBasedStopCondition<StateC> nOfIterations(final long n) {
    return s -> new Progress(0, n, s.getNOfIterations());
  }

  @SuppressWarnings("unused")
  public static <F extends Comparable<F>>
      Predicate<POSetPopulationStateC<?, ?, ? extends F>> targetFitness(final F targetF) {
    return s ->
        s.getPopulation().firsts().stream()
            .map(Individual::fitness)
            .anyMatch(f -> f.compareTo(targetF) <= 0);
  }
}
