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
import io.github.ericmedvet.jgea.core.operator.GeneticOperator;
import io.github.ericmedvet.jgea.core.order.PartialComparator;
import io.github.ericmedvet.jgea.core.order.PartiallyOrderedCollection;
import io.github.ericmedvet.jgea.core.problem.QualityBasedProblem;
import io.github.ericmedvet.jgea.core.selector.Selector;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;

public class StandardEvolver<G, S, Q> extends AbstractStandardEvolver<POCPopulationState<Individual<G, S, Q>, G, S, Q, QualityBasedProblem<S, Q>>, QualityBasedProblem<S, Q>, Individual<G, S, Q>, G, S, Q> {
  public StandardEvolver(
      Function<? super G, ? extends S> solutionMapper,
      Factory<? extends G> genotypeFactory,
      int populationSize,
      Predicate<? super POCPopulationState<Individual<G, S, Q>, G, S, Q, QualityBasedProblem<S, Q>>> stopCondition,
      Map<GeneticOperator<G>, Double> operators,
      Selector<? super Individual<G, S, Q>> parentSelector,
      Selector<? super Individual<G, S, Q>> unsurvivalSelector,
      int offspringSize,
      boolean overlapping,
      int maxUniquenessAttempts,
      boolean remap,
      List<PartialComparator<? super Individual<G,S,Q>>> additionalIndividualComparators
  ) {
    super(
        solutionMapper,
        genotypeFactory,
        populationSize,
        stopCondition,
        operators,
        parentSelector,
        unsurvivalSelector,
        offspringSize,
        overlapping,
        maxUniquenessAttempts,
        remap,
        additionalIndividualComparators
    );
  }

  @Override
  protected POCPopulationState<Individual<G, S, Q>, G, S, Q, QualityBasedProblem<S, Q>> init(
      QualityBasedProblem<S, Q> problem
  ) {
    return POCPopulationState.empty(problem, stopCondition());
  }

  @Override
  protected Individual<G, S, Q> mapChildGenotype(
      ChildGenotype<G> childGenotype,
      POCPopulationState<Individual<G, S, Q>, G, S, Q, QualityBasedProblem<S, Q>> state,
      RandomGenerator random
  ) {
    return Individual.from(childGenotype, solutionMapper, state.problem().qualityFunction(), state.nOfIterations());
  }

  @Override
  protected Individual<G, S, Q> remapIndividual(
      Individual<G, S, Q> individual,
      POCPopulationState<Individual<G, S, Q>, G, S, Q, QualityBasedProblem<S, Q>> state,
      RandomGenerator random
  ) {
    return individual.updatedWithQuality(state);
  }

  @Override
  protected POCPopulationState<Individual<G, S, Q>, G, S, Q, QualityBasedProblem<S, Q>> update(
      POCPopulationState<Individual<G, S, Q>, G, S, Q, QualityBasedProblem<S, Q>> state,
      Collection<Individual<G, S, Q>> individuals,
      long nOfNewBirths,
      long nOfNewFitnessEvaluations
  ) {
    return state.updatedWithIteration(
        nOfNewBirths,
        nOfNewFitnessEvaluations,
        PartiallyOrderedCollection.from(individuals, partialComparator(state.problem()))
    );
  }
}
