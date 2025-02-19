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

package io.github.ericmedvet.jgea.experimenter.builders;

import io.github.ericmedvet.jgea.core.problem.*;
import io.github.ericmedvet.jgea.core.util.Misc;
import io.github.ericmedvet.jnb.core.*;
import io.github.ericmedvet.jnb.datastructure.NamedFunction;
import io.github.ericmedvet.jsdynsym.control.Simulation;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Stream;

@Discoverable(prefixTemplate = "ea.problem|p")
public class Problems {

  private Problems() {
  }

  public enum OptimizationType {
    @SuppressWarnings("unused") MINIMIZE, MAXIMIZE
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static <S, Q, O> TotalOrderQualityBasedProblem<S, Q> moToSo(
      @Param(value = "name", iS = "{moProblem.name}[{objective}]") String name,
      @Param("objective") String objective,
      @Param("moProblem") MultiObjectiveProblem<S, Q, O> moProblem
  ) {
    return moProblem.toTotalOrderQualityBasedProblem(objective);
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static <S> SimpleMOProblem<S, Double> mtToMo(
      @Param(value = "name", iS = "mt2mo[{mtProblem.name}]") String name,
      @Param("mtProblem") MultiTargetProblem<S> mtProblem
  ) {
    return mtProblem.toMHOProblem();
  }

  public static <S, B extends Simulation.Outcome<BS>, BS, O extends Comparable<O>> SimpleBBMOProblem<S, B, O> simToSbbmo(
      @Param(value = "name", iS = "{simulation.name}") String name,
      @Param("simulation") Simulation<S, BS, B> simulation,
      @Param("toMinObjectives") List<Function<B, O>> toMinObjectives,
      @Param("toMaxObjectives") List<Function<B, O>> toMaxObjectives
  ) {
    SequencedMap<String, MultiObjectiveProblem.Objective<B, O>> behaviorObjectives = Stream.concat(
        toMinObjectives.stream().map(f -> new MultiObjectiveProblem.Objective<>(f, Comparable::compareTo)),
        toMaxObjectives.stream()
            .map(f -> new MultiObjectiveProblem.Objective<>(f, ((Comparator<O>) Comparable::compareTo).reversed()))
    )
        .collect(
            Misc.toSequencedMap(
                o -> NamedFunction.name(o.function()),
                o -> o
            )
        );
    if (behaviorObjectives.size() != (toMinObjectives.size() + toMaxObjectives.size())) {
      Logger.getLogger(Problems.class.getName())
          .warning(
              "Objectives have conflicting names: to minimize is %s, to maximize is %s".formatted(
                  toMinObjectives.stream().map(NamedFunction::name).toList(),
                  toMaxObjectives.stream().map(NamedFunction::name).toList()
              )
          );
    }
    return new SimpleBBMOProblem<>() {
      @Override
      public Function<? super S, ? extends B> behaviorFunction() {
        return simulation::simulate;
      }

      @Override
      public SequencedMap<String, Objective<B, O>> behaviorObjectives() {
        return behaviorObjectives;
      }

      @Override
      public Optional<S> example() {
        return simulation.example();
      }

      @Override
      public String toString() {
        return "%s[%s]".formatted(name, String.join(";", behaviorObjectives.keySet()));
      }
    };
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static <S, O> SimpleMOProblem<S, O> smoToSubsettedSmo(
      @Param(value = "name", iS = "{smoProblem.name}") String name,
      @Param("objectives") List<String> objectives,
      @Param("smoProblem") SimpleMOProblem<S, O> smoProblem
  ) {
    return smoProblem.toReducedSimpleMOProblem(new HashSet<>(objectives));
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static <S, Q, C extends Comparable<C>> TotalOrderQualityBasedProblem<S, Q> totalOrder(
      @Param(value = "name", dS = "{qFunction}") String name,
      @Param("qFunction") Function<S, Q> qualityFunction,
      @Param(value = "cFunction", dNPM = "f.identity()") Function<Q, C> comparableFunction,
      @Param(value = "type", dS = "minimize") OptimizationType type
  ) {
    return TotalOrderQualityBasedProblem.from(
        qualityFunction,
        null,
        type.equals(OptimizationType.MAXIMIZE) ? Comparator.comparing(comparableFunction)
            .reversed() : Comparator.comparing(comparableFunction),
        Optional.empty()
    );
  }

}
