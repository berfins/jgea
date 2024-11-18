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
package io.github.ericmedvet.jgea.core.solver.mapelites;

import io.github.ericmedvet.jgea.core.Factory;
import io.github.ericmedvet.jgea.core.distance.Distance;
import io.github.ericmedvet.jgea.core.operator.Mutation;
import io.github.ericmedvet.jgea.core.order.PartiallyOrderedCollection;
import io.github.ericmedvet.jgea.core.problem.QualityBasedProblem;
import io.github.ericmedvet.jgea.core.solver.AbstractPopulationBasedIterativeSolver;
import io.github.ericmedvet.jgea.core.solver.Individual;
import io.github.ericmedvet.jgea.core.solver.SolverException;
import io.github.ericmedvet.jgea.core.solver.mapelites.strategy.CoMEStrategy;
import io.github.ericmedvet.jgea.core.util.Misc;
import io.github.ericmedvet.jnb.datastructure.DoubleRange;
import io.github.ericmedvet.jnb.datastructure.Pair;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class CoMapElites<G1, G2, S1, S2, S, Q> extends AbstractPopulationBasedIterativeSolver<CoMEPopulationState<G1, G2, S1, S2, S, Q, QualityBasedProblem<S, Q>>, QualityBasedProblem<S, Q>, CoMEIndividual<G1, G2, S1, S2, S, Q>, Pair<G1, G2>, S, Q> {

  private final Factory<? extends G1> genotypeFactory1;
  private final Factory<? extends G2> genotypeFactory2;
  private final Mutation<G1> mutation1;
  private final Mutation<G2> mutation2;
  private final Function<? super G1, ? extends S1> solutionMapper1;
  private final Function<? super G2, ? extends S2> solutionMapper2;
  private final BiFunction<? super S1, ? super S2, ? extends S> solutionMerger;
  private final List<MapElites.Descriptor<G1, S1, Q>> descriptors1;
  private final List<MapElites.Descriptor<G2, S2, Q>> descriptors2;
  private final int populationSize;
  private final int nOfOffspring;
  private final Supplier<CoMEStrategy> strategySupplier;
  private final double neighborRadius;
  private final int maxNOfNeighbors;

  public CoMapElites(
      Predicate<? super CoMEPopulationState<G1, G2, S1, S2, S, Q, QualityBasedProblem<S, Q>>> stopCondition,
      Factory<? extends G1> genotypeFactory1,
      Factory<? extends G2> genotypeFactory2,
      Function<? super G1, ? extends S1> solutionMapper1,
      Function<? super G2, ? extends S2> solutionMapper2,
      BiFunction<? super S1, ? super S2, ? extends S> solutionMerger,
      List<MapElites.Descriptor<G1, S1, Q>> descriptors1,
      List<MapElites.Descriptor<G2, S2, Q>> descriptors2,
      Mutation<G1> mutation1,
      Mutation<G2> mutation2,
      int populationSize,
      int nOfOffspring,
      Supplier<CoMEStrategy> strategySupplier,
      double neighborRadius,
      int maxNOfNeighbors
  ) {
    super(null, null, stopCondition, false);
    this.genotypeFactory1 = genotypeFactory1;
    this.genotypeFactory2 = genotypeFactory2;
    this.solutionMapper1 = solutionMapper1;
    this.solutionMapper2 = solutionMapper2;
    this.solutionMerger = solutionMerger;
    this.descriptors1 = descriptors1;
    this.descriptors2 = descriptors2;
    this.mutation1 = mutation1;
    this.mutation2 = mutation2;
    this.populationSize = populationSize;
    this.nOfOffspring = nOfOffspring;
    this.strategySupplier = strategySupplier;
    this.neighborRadius = neighborRadius;
    this.maxNOfNeighbors = maxNOfNeighbors;
    if (descriptors1.size() != descriptors2.size()) {
      throw new IllegalArgumentException(
          "Unexpected different sizes of descriptors: %d vs. %d"
              .formatted(descriptors1.size(), descriptors2.size())
      );
    }
  }

  public static List<Integer> denormalizeCoords(
      List<Double> coordinates,
      List<? extends MapElites.Descriptor<?, ?, ?>> descriptors
  ) {
    if (coordinates.size() != descriptors.size()) {
      throw new IllegalArgumentException(
          "Unexpected different sizes of coords and descriptors: %d vs. %d"
              .formatted(coordinates.size(), descriptors.size())
      );
    }
    return IntStream.range(0, coordinates.size())
        .map(
            i -> (int) Math.round(
                new DoubleRange(0, descriptors.get(i).nOfBins() - 1).denormalize(coordinates.get(i))
            )
        )
        .boxed()
        .toList();
  }

  private static double euclideanDistance(List<Integer> coords1, List<Integer> coords2) {
    if (coords1.size() != coords2.size()) {
      throw new IllegalArgumentException("Coordinates must have the same dimensions.");
    }
    double sum = 0.0;
    for (int i = 0; i < coords1.size(); i++) {
      double diff = coords1.get(i) - coords2.get(i);
      sum += diff * diff;
    }
    return Math.sqrt(sum);
  }

  private static <X> Collection<X> findNeighbors(
      List<Integer> coords,
      Map<List<Integer>, X> archive,
      Distance<List<Integer>> distance,
      double neighborRadius
  ) {
    return archive.entrySet()
        .stream()
        .filter(e -> distance.apply(e.getKey(), coords) < neighborRadius)
        .map(Map.Entry::getValue)
        .toList();
  }

  private static List<Integer> getClosestCoordinate(List<Integer> coords, Map<List<Integer>, ?> mapOfElites) {
    return mapOfElites.keySet()
        .stream()
        .min(Comparator.comparingDouble(c -> euclideanDistance(c, coords)))
        .orElseThrow();
  }

  public static List<Double> normalizeCoords(
      List<Integer> coordinates,
      List<? extends MapElites.Descriptor<?, ?, ?>> descriptors
  ) {
    if (coordinates.size() != descriptors.size()) {
      throw new IllegalArgumentException(
          "Unexpected different size of coords and descriptors: %d vs. %d"
              .formatted(coordinates.size(), descriptors.size())
      );
    }
    return IntStream.range(0, coordinates.size())
        .mapToObj(i -> new DoubleRange(0, descriptors.get(i).nOfBins() - 1).normalize(coordinates.get(i)))
        .toList();
  }

  private static <GT, GO, ST, SO, S, Q> Callable<Pair<CoMEPartialIndividual<GT, ST, GT, GO, ST, SO, S, Q>, List<CoMEIndividual<GT, GO, ST, SO, S, Q>>>> reproduceCallable(
      Archive<? extends MEIndividual<GT, ST, Q>> thisArchive,
      Archive<? extends MEIndividual<GO, SO, Q>> otherArchive,
      Mutation<GT> mutation,
      Function<? super GT, ? extends ST> thisSolutionMapper,
      Function<? super GO, ? extends SO> otherSolutionMapper,
      BiFunction<? super ST, ? super SO, ? extends S> solutionMerger,
      List<MapElites.Descriptor<GT, ST, Q>> thisDescriptors,
      List<MapElites.Descriptor<GO, SO, Q>> otherDescriptors,
      CoMEStrategy strategy,
      double neighborRadius,
      int maxNOfNeighbors,
      QualityBasedProblem<S, Q> problem,
      RandomGenerator random,
      long iteration,
      AtomicLong counter
  ) {
    return () -> {
      MEIndividual<GT, ST, Q> parentT = Misc.pickRandomly(thisArchive.asMap().values(), random);
      ChildGenotype<GT> childGenotypeT = new ChildGenotype<>(
          counter.getAndIncrement(),
          mutation.mutate(parentT.genotype(), random),
          List.of(parentT.id())
      );
      CoMEPartialIndividual<GT, ST, GT, GO, ST, SO, S, Q> iT = CoMEPartialIndividual.from(
          Individual.from(childGenotypeT, thisSolutionMapper, sT -> null, iteration),
          thisDescriptors
      );
      List<Integer> thisCoords = iT.coordinates()
          .stream()
          .map(MapElites.Descriptor.Coordinate::bin)
          .toList();
      List<Integer> otherCoords = denormalizeCoords(
          strategy.getOtherCoords(normalizeCoords(thisCoords, thisDescriptors)),
          otherDescriptors
      );
      otherCoords = getClosestCoordinate(otherCoords, otherArchive.asMap());
      List<? extends MEIndividual<GO, SO, Q>> neighbors = new ArrayList<>(
          findNeighbors(otherCoords, otherArchive.asMap(), CoMapElites::euclideanDistance, neighborRadius)
      );
      Collections.shuffle(neighbors, random);
      List<CoMEIndividual<GT, GO, ST, SO, S, Q>> localCompositeIndividuals = neighbors.stream()
          .limit(maxNOfNeighbors)
          .map(iO -> {
            S s = solutionMerger.apply(iT.solution(), otherSolutionMapper.apply(iO.genotype()));
            return CoMEIndividual.of(
                counter.getAndIncrement(),
                s,
                problem.qualityFunction().apply(s),
                iteration,
                iteration,
                List.of(),
                iT,
                iO
            );
          })
          .toList();
      CoMEIndividual<GT, GO, ST, SO, S, Q> bestCompleteIndividual = PartiallyOrderedCollection.from(
          localCompositeIndividuals,
          problem.qualityComparator().comparing(CoMEIndividual::quality)
      )
          .firsts()
          .stream()
          .findAny()
          .orElseThrow();
      return new Pair<>(iT.updateWithCompleteIndividual(bestCompleteIndividual), localCompositeIndividuals);
    };
  }

  private Callable<CoMEIndividual<G1, G2, S1, S2, S, Q>> coMapCallable(
      ChildGenotype<G1> cg1,
      ChildGenotype<G2> cg2,
      CoMEPopulationState<G1, G2, S1, S2, S, Q, QualityBasedProblem<S, Q>> state,
      AtomicLong counter
  ) {
    return () -> {
      S1 s1 = solutionMapper1.apply(cg1.genotype());
      S2 s2 = solutionMapper2.apply(cg2.genotype());
      S s = solutionMerger.apply(s1, s2);
      Q q = state.problem().qualityFunction().apply(s);
      return CoMEIndividual.of(
          counter.getAndIncrement(),
          s,
          q,
          state.nOfIterations(),
          state.nOfIterations(),
          List.of(),
          MEIndividual.from(
              Individual.from(cg1, solutionMapper1, ss1 -> q, state.nOfIterations()),
              descriptors1
          ),
          MEIndividual.from(
              Individual.from(cg2, solutionMapper2, ss2 -> q, state.nOfIterations()),
              descriptors2
          )
      );
    };
  }

  @Override
  public CoMEPopulationState<G1, G2, S1, S2, S, Q, QualityBasedProblem<S, Q>> init(
      QualityBasedProblem<S, Q> problem,
      RandomGenerator random,
      ExecutorService executor
  ) throws SolverException {
    CoMEStrategy strategy1 = strategySupplier.get();
    CoMEStrategy strategy2 = strategySupplier.get();
    CoMEPopulationState<G1, G2, S1, S2, S, Q, QualityBasedProblem<S, Q>> newState = CoMEPopulationState.empty(
        problem,
        stopCondition(),
        descriptors1,
        descriptors2,
        strategy1,
        strategy2
    );
    AtomicLong counter = new AtomicLong(0);
    List<ChildGenotype<G1>> childGenotypes1 = genotypeFactory1.build(populationSize, random)
        .stream()
        .map(g -> new ChildGenotype<G1>(counter.getAndIncrement(), g, List.of()))
        .toList();
    List<ChildGenotype<G2>> childGenotypes2 = genotypeFactory2.build(populationSize, random)
        .stream()
        .map(g -> new ChildGenotype<G2>(counter.getAndIncrement(), g, List.of()))
        .toList();
    Collection<CoMEIndividual<G1, G2, S1, S2, S, Q>> coMEIndividuals = getAll(
        IntStream.range(0, populationSize)
            .mapToObj(i -> coMapCallable(childGenotypes1.get(i), childGenotypes2.get(i), newState, counter))
            .toList(),
        executor
    );
    // update strategies
    updateStrategies(newState, coMEIndividuals);
    // update archive
    Archive<CoMEPartialIndividual<G1, S1, G1, G2, S1, S2, S, Q>> archive1 = newState.archive1()
        .updated(
            coMEIndividuals.stream()
                .map(CoMEPartialIndividual::from1)
                .toList(),
            MEIndividual::bins,
            partialComparator(problem)
        );
    Archive<CoMEPartialIndividual<G2, S2, G1, G2, S1, S2, S, Q>> archive2 = newState.archive2()
        .updated(
            coMEIndividuals.stream()
                .map(CoMEPartialIndividual::from2)
                .toList(),
            MEIndividual::bins,
            partialComparator(problem)
        );
    // return state
    return newState.updatedWithIteration(populationSize, populationSize, archive1, archive2, strategy1, strategy2);
  }

  @Override
  public CoMEPopulationState<G1, G2, S1, S2, S, Q, QualityBasedProblem<S, Q>> update(
      RandomGenerator random,
      ExecutorService executor,
      CoMEPopulationState<G1, G2, S1, S2, S, Q, QualityBasedProblem<S, Q>> state
  ) throws SolverException {
    AtomicLong counter = new AtomicLong(state.nOfBirths());
    // reproduction 1
    Collection<Pair<CoMEPartialIndividual<G1, S1, G1, G2, S1, S2, S, Q>, List<CoMEIndividual<G1, G2, S1, S2, S, Q>>>> reproduction1 = getAll(
        IntStream.range(0, nOfOffspring / 2)
            .mapToObj(
                i -> reproduceCallable(
                    state.archive1(),
                    state.archive2(),
                    mutation1,
                    solutionMapper1,
                    solutionMapper2,
                    solutionMerger,
                    descriptors1,
                    descriptors2,
                    state.strategy1(),
                    neighborRadius,
                    maxNOfNeighbors,
                    state.problem(),
                    random,
                    state.nOfIterations(),
                    counter
                )
            )
            .toList(),
        executor
    );
    // reproduction 2
    Collection<Pair<CoMEPartialIndividual<G2, S2, G2, G1, S2, S1, S, Q>, List<CoMEIndividual<G2, G1, S2, S1, S, Q>>>> reproduction2 = getAll(
        IntStream.range(0, nOfOffspring / 2)
            .mapToObj(
                i -> reproduceCallable(
                    state.archive2(),
                    state.archive1(),
                    mutation2,
                    solutionMapper2,
                    solutionMapper1,
                    (s2, s1) -> solutionMerger.apply(s1, s2),
                    descriptors2,
                    descriptors1,
                    state.strategy2(),
                    neighborRadius,
                    maxNOfNeighbors,
                    state.problem(),
                    random,
                    state.nOfIterations(),
                    counter
                )
            )
            .toList(),
        executor
    );
    List<CoMEIndividual<G1, G2, S1, S2, S, Q>> coMEIndividuals1 = reproduction1.stream()
        .flatMap(p1 -> p1.second().stream())
        .toList();
    List<CoMEIndividual<G1, G2, S1, S2, S, Q>> coMEIndividuals2 = reproduction2.stream()
        .flatMap(p2 -> p2.second().stream().map(CoMEIndividual::swapped))
        .toList();
    List<CoMEIndividual<G1, G2, S1, S2, S, Q>> offspring = Stream.of(coMEIndividuals1, coMEIndividuals2)
        .flatMap(List::stream)
        .toList();
    // update strategies
    updateStrategies(state, offspring);
    // update archives
    Archive<CoMEPartialIndividual<G1, S1, G1, G2, S1, S2, S, Q>> archive1 = state.archive1()
        .updated(
            offspring.stream().map(CoMEPartialIndividual::from1).toList(),
            MEIndividual::bins,
            partialComparator(state.problem())
        );
    Archive<CoMEPartialIndividual<G2, S2, G1, G2, S1, S2, S, Q>> archive2 = state.archive2()
        .updated(
            offspring.stream().map(CoMEPartialIndividual::from2).toList(),
            MEIndividual::bins,
            partialComparator(state.problem())
        );
    // return state
    return state.updatedWithIteration(
        nOfOffspring,
        coMEIndividuals1.size() + coMEIndividuals2.size(),
        archive1,
        archive2,
        state.strategy1(),
        state.strategy2()
    );
  }

  private void updateStrategies(
      CoMEPopulationState<G1, G2, S1, S2, S, Q, QualityBasedProblem<S, Q>> state,
      Collection<CoMEIndividual<G1, G2, S1, S2, S, Q>> newIndividuals
  ) {
    state.strategy1()
        .update(
            newIndividuals.stream()
                .map(
                    ci -> new CoMEStrategy.Observation<>(
                        normalizeCoords(
                            ci.individual1()
                                .coordinates()
                                .stream()
                                .map(MapElites.Descriptor.Coordinate::bin)
                                .toList(),
                            descriptors1
                        ),
                        normalizeCoords(
                            ci.individual2()
                                .coordinates()
                                .stream()
                                .map(MapElites.Descriptor.Coordinate::bin)
                                .toList(),
                            descriptors2
                        ),
                        ci.quality()
                    )
                )
                .toList(),
            state.problem().qualityComparator()
        );
    state.strategy2()
        .update(
            newIndividuals.stream()
                .map(
                    ci -> new CoMEStrategy.Observation<>(
                        normalizeCoords(
                            ci.individual2()
                                .coordinates()
                                .stream()
                                .map(MapElites.Descriptor.Coordinate::bin)
                                .toList(),
                            descriptors2
                        ),
                        normalizeCoords(
                            ci.individual1()
                                .coordinates()
                                .stream()
                                .map(MapElites.Descriptor.Coordinate::bin)
                                .toList(),
                            descriptors1
                        ),
                        ci.quality()
                    )
                )
                .toList(),
            state.problem().qualityComparator()
        );
  }
}
