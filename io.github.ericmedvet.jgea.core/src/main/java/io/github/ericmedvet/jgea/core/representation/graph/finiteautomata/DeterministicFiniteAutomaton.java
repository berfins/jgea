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

package io.github.ericmedvet.jgea.core.representation.graph.finiteautomata;

import io.github.ericmedvet.jgea.core.IndependentFactory;
import io.github.ericmedvet.jgea.core.representation.graph.Graph;
import io.github.ericmedvet.jgea.core.representation.graph.Node;
import io.github.ericmedvet.jgea.core.util.IntRange;
import io.github.ericmedvet.jgea.core.util.Misc;
import io.github.ericmedvet.jgea.core.util.Sized;
import java.io.Serializable;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;

public class DeterministicFiniteAutomaton<S> implements Extractor<S>, Sized, Serializable {

  private final Graph<State, Set<S>> graph;
  private final State startingState;

  public DeterministicFiniteAutomaton(Graph<State, Set<S>> graph) {
    check(graph);
    this.graph = graph;
    this.startingState = graph.nodes()
        .stream()
        .filter(s -> s.getIndex() == 0)
        .findFirst()
        .orElseThrow();
  }

  public static class State extends Node {
    private final boolean accepting;

    public State(int index, boolean accepting) {
      super(index);
      this.accepting = accepting;
    }

    public boolean isAccepting() {
      return accepting;
    }

    @Override
    public String toString() {
      return "S" + index + (accepting ? "*" : "");
    }
  }

  public static <R> Function<Graph<State, Set<R>>, DeterministicFiniteAutomaton<R>> builder() {
    return DeterministicFiniteAutomaton::new;
  }

  public static <K> void check(Graph<State, Set<K>> graph) {
    if (graph.nodes().stream().filter(s -> s.getIndex() == 0).count() != 1) {
      throw new IllegalArgumentException(
          String.format(
              "Invalid graph: wrong number of starting nodes: %d instead of 1",
              graph.nodes().stream().filter(s -> s.getIndex() == 0).count()
          )
      );
    }
    for (State state : graph.nodes()) {
      Set<Set<K>> outgoingArcValues = graph.arcs()
          .stream()
          .filter(a -> a.getSource().equals(state))
          .map(graph::getArcValue)
          .collect(Collectors.toSet());
      if (outgoingArcValues.size() > 1) {
        Set<K> intersection = outgoingArcValues.stream().reduce(Misc::intersection).orElse(new HashSet<>());
        if (!intersection.isEmpty()) {
          throw new IllegalArgumentException(
              String.format(
                  "Invalid graph: state %s has one or more outgoing symbols " + "(%s)",
                  state,
                  intersection
              )
          );
        }
      }
    }
  }

  public static <K> Predicate<Graph<State, Set<K>>> checker() {
    return graph -> {
      try {
        check(graph);
      } catch (IllegalArgumentException e) {
        return false;
      }
      return true;
    };
  }

  public static IndependentFactory<State> sequentialStateFactory(final int startingIndex, double acceptanceRate) {
    return new IndependentFactory<>() {
      private int localStartingIndex = startingIndex;

      @Override
      public State build(RandomGenerator random) {
        localStartingIndex = localStartingIndex + 1;
        return new State(localStartingIndex - 1, random.nextDouble() < acceptanceRate);
      }
    };
  }

  @Override
  public Set<IntRange> extract(List<S> sequence) {
    Set<IntRange> ranges = new LinkedHashSet<>();
    State current = startingState;
    int lastStart = 0;
    for (int i = 0; i < sequence.size(); i++) {
      if (current.isAccepting()) {
        ranges.add(new IntRange(lastStart, i));
      }
      current = next(current, sequence.get(i));
      if (current == null) {
        current = startingState;
        i = lastStart;
        lastStart = i + 1;
      }
    }
    return ranges;
  }

  @Override
  public boolean match(List<S> sequence) {
    State current = startingState;
    for (S s : sequence) {
      State next = next(current, s);
      if (next == null) {
        return false;
      }
      current = next;
    }
    return current.isAccepting();
  }

  private State next(State current, S s) {
    Set<State> successors = graph.successors(current);
    for (State successor : successors) {
      Set<S> symbols = graph.getArcValue(current, successor);
      if (symbols != null && symbols.contains(s)) {
        return successor;
      }
    }
    return null;
  }

  @Override
  public int size() {
    return graph.size();
  }

  @Override
  public String toString() {
    return graph.arcs()
        .stream()
        .map(
            a -> String.format(
                "%s-[%s]->%s",
                a.getSource(),
                graph.getArcValue(a)
                    .stream()
                    .sorted()
                    .map(Objects::toString)
                    .collect(Collectors.joining()),
                a.getTarget()
            )
        )
        .collect(Collectors.joining(","));
  }
}
