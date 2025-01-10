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

package io.github.ericmedvet.jgea.core.solver;

import io.github.ericmedvet.jgea.core.Factory;
import io.github.ericmedvet.jgea.core.operator.Mutation;
import io.github.ericmedvet.jgea.core.order.PartialComparator;
import io.github.ericmedvet.jgea.core.problem.QualityBasedProblem;
import io.github.ericmedvet.jgea.core.selector.Selector;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;

public class MutationOnly<G, S, Q> extends StandardEvolver<G, S, Q> {

  private final Mutation<G> mutation;

  public MutationOnly(
      Function<? super G, ? extends S> solutionMapper,
      Factory<? extends G> genotypeFactory,
      int populationSize,
      Predicate<? super POCPopulationState<Individual<G, S, Q>, G, S, Q, QualityBasedProblem<S, Q>>> stopCondition,
      Selector<? super Individual<? super G, ? super S, ? super Q>> unsurvivalSelector,
      Mutation<G> mutation,
      List<PartialComparator<? super Individual<G,S,Q>>> additionalIndividualComparators
  ) {
    super(
        solutionMapper,
        genotypeFactory,
        populationSize,
        stopCondition,
        Map.of(mutation, 1d),
        null,
        unsurvivalSelector,
        0,
        true,
        0,
        false,
        additionalIndividualComparators
    );
    this.mutation = mutation;
  }

  @Override
  protected Collection<ChildGenotype<G>> buildOffspringToMapGenotypes(
      POCPopulationState<Individual<G, S, Q>, G, S, Q, QualityBasedProblem<S, Q>> state,
      RandomGenerator random
  ) {
    AtomicLong counter = new AtomicLong(state.nOfBirths());
    return state.pocPopulation()
        .all()
        .stream()
        .map(
            i -> new ChildGenotype<>(
                counter.getAndIncrement(),
                mutation.mutate(i.genotype(), random),
                List.of(i.id())
            )
        )
        .toList();
  }
}
