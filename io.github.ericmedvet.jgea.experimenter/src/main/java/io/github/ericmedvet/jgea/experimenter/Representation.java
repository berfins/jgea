/*-
 * ========================LICENSE_START=================================
 * jgea-experimenter
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
package io.github.ericmedvet.jgea.experimenter;

import io.github.ericmedvet.jgea.core.Factory;
import io.github.ericmedvet.jgea.core.operator.Crossover;
import io.github.ericmedvet.jgea.core.operator.GeneticOperator;
import io.github.ericmedvet.jgea.core.operator.Mutation;
import io.github.ericmedvet.jgea.core.util.Misc;
import io.github.ericmedvet.jnb.datastructure.Pair;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public record Representation<G>(Factory<G> factory, List<Mutation<G>> mutations, List<Crossover<G>> crossovers) {

  public Representation(Factory<G> factory, Mutation<G> mutation, Crossover<G> crossover) {
    this(factory, List.of(mutation), List.of(crossover));
  }

  public static <G1, G2> Representation<Pair<G1, G2>> pair(Representation<G1> r1, Representation<G2> r2) {
    return new Representation<>(
        Factory.pair(r1.factory, r2.factory),
        r1.mutations.stream()
            .flatMap(m1 -> r2.mutations.stream().map(m2 -> Mutation.pair(m1, m2)))
            .toList(),
        r1.crossovers.stream()
            .flatMap(xo1 -> r2.crossovers.stream().map(xo2 -> Crossover.pair(xo1, xo2)))
            .toList()
    );
  }

  public Map<GeneticOperator<G>, Double> geneticOperators(double crossoverP) {
    return Stream.concat(
        mutations.stream().map(m -> Map.entry(m, (1d - crossoverP) / (double) mutations.size())),
        crossovers.stream().map(c -> Map.entry(c, crossoverP / (double) crossovers.size()))
    )
        .collect(Misc.toSequencedMap(Map.Entry::getKey, Map.Entry::getValue));
  }
}
