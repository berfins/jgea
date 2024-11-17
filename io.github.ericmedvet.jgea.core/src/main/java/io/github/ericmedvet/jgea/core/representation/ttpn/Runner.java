package io.github.ericmedvet.jgea.core.representation.ttpn;

import io.github.ericmedvet.jgea.core.representation.ttpn.type.Type;
import io.github.ericmedvet.jgea.core.representation.ttpn.type.TypeException;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Runner {

  private final int maxSteps;
  private final int maxTokens;

  public record State(List<WireState> wireStates) {
    public record WireState(Wire wire, Type type, int nOfTokens) {}
  }

  public record Outcome(List<Object> outputs, List<State> states) {}

  public Runner(int maxSteps, int maxTokens) {
    this.maxSteps = maxSteps;
    this.maxTokens = maxTokens;
  }

  public Outcome run(Network network, List<Object> inputs) throws RunnerException {
    // check validity
    List<Type> inputTypes = network.gates()
        .stream()
        .filter(g -> g instanceof Gate.InputGate)
        .map(g -> ((Gate.InputGate) g).type())
        .toList();
    List<Type> outputTypes = network.gates()
        .stream()
        .filter(g -> g instanceof Gate.OutputGate)
        .map(g -> ((Gate.OutputGate) g).type())
        .toList();
    if (inputs.size() != inputTypes.size()) {
      throw new RunnerException("Wrong number of inputs: %d expected, %d found".formatted(
          inputTypes.size(),
          inputs.size()
      ));
    }
    for (int i = 0; i < inputTypes.size(); i++) {
      if (!inputTypes.get(i).matches(inputs.get(i))) {
        throw new RunnerException("Invalid input type for input %d of type %s: %s".formatted(
            i,
            inputTypes.get(i),
            inputs.get(i).getClass()
        ));
      }
    }
    // prepare memory
    Map<Wire, Queue<Object>> current = new HashMap<>();
    network.wires().forEach(w -> current.put(w, new ArrayDeque<>()));
    Map<Wire, List<Object>> next = new HashMap<>();
    Map<Wire, Type> actualTypes = new HashMap<>();
    for (Wire w : network.wires()) {
      try {
        actualTypes.put(w, network.actualType(w));
      } catch (TypeException e) {
        throw new RunnerException("Cannot get actual type for wire %s".formatted(w), e);
      }
    }
    // prepare state, counter, and output map
    int k = 0;
    List<State> states = new ArrayList<>();
    Queue<Object> networkInputsQueue = new ArrayDeque<>(inputs);
    SortedMap<Integer, Object> outputs = new TreeMap<>();
    // iterate
    while (k < maxSteps) {
      for (int i = 0; i < network.gates().size(); i++) {
        int gi = i;
        Gate g = network.gates().get(gi);
        if (g instanceof Gate.InputGate && !networkInputsQueue.isEmpty()) {
          network.wiresFrom(gi, 0).forEach(w -> next.put(w, List.of(networkInputsQueue.remove())));
        } else if (g instanceof Gate.OutputGate) {
          network.wireTo(gi, 0).flatMap(w -> takeOne(current.get(w))).ifPresent(token -> outputs.put(gi, token));
        } else {
          List<Queue<Object>> inputQueues = IntStream.range(0, g.inputPorts().size())
              .mapToObj(pi -> network.wireTo(gi, pi).map(current::get).orElse(emptyQueue()))
              .toList();
          // check conditions and possibly apply function
          if (IntStream.range(0, g.inputPorts().size()).allMatch(pi -> inputQueues.get(pi).size() >= g.inputPorts().get(
              pi).n())) {
            List<List<Object>> localInputs = IntStream.range(0, g.inputPorts().size())
                .mapToObj(pi -> switch (g.inputPorts().get(pi).condition()) {
                  case EXACTLY -> takeExactly(inputQueues.get(pi), g.inputPorts().get(pi).n());
                  case AT_LEAST -> takeAll(inputQueues.get(pi));
                })
                .toList();
            try {
              List<List<Object>> localOutputs = g.processingFunction().apply(localInputs);
              // check number of outputs
              if (localOutputs.size() != g.outputTypes().size()) {
                throw new RunnerException("Unexpected wrong number of outputs: %d expected, %d found".formatted(
                    g.outputTypes()
                        .size(), localOutputs.size()
                ));
              }
              // put outputs
              IntStream.range(0, localOutputs.size())
                  .forEach(pi -> network.wiresFrom(gi, pi).forEach(w -> next.put(w, localOutputs.get(pi))));
            } catch (RuntimeException e) {
              throw new RunnerException("Cannot run %s on %s".formatted(g, localInputs), e);
            }
          }
        }
      }
      // check no new tokens
      if (next.values().stream().mapToInt(List::size).sum() == 0) {
        break;
      }
      // add tokens to current
      next.forEach((w, ts) -> current.get(w).addAll(ts));
      next.clear();
      // build state
      states.add(new State(
          current.entrySet().stream().map(e -> new State.WireState(e.getKey(), actualTypes.get(e.getKey()), e.getValue().size())).toList()
      ));
      // increment k
      k = k + 1;
    }
    // check output

    return new Outcome(outputs.values().stream().toList(), states);
  }

  private static <T> Optional<T> takeOne(Queue<T> queue) {
    if (queue.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(queue.remove());
  }

  private static <T> List<T> takeExactly(Queue<T> queue, int n) {
    return IntStream.range(0, n).mapToObj(i -> queue.remove()).toList();
  }

  private static <T> List<T> takeAll(Queue<T> queue) {
    List<T> list = new ArrayList<>(queue);
    queue.clear();
    return list;
  }

  private static <T> Queue<T> emptyQueue() {
    return new ArrayDeque<>();
  }

}
