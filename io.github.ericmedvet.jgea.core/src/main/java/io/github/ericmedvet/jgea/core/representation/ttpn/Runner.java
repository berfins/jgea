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

    public int nOfTokens(Type type) {
      return wireStates.stream().filter(ws -> ws.type.equals(type)).mapToInt(ws -> ws.nOfTokens).sum();
    }

    public int nOfTokens() {
      return wireStates.stream().mapToInt(ws -> ws.nOfTokens).sum();
    }
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
    SortedMap<Integer, Type> outputTypes = new TreeMap<>(IntStream.range(0, network.gates().size())
        .filter(gi -> network.gates().get(gi) instanceof Gate.OutputGate)
        .boxed()
        .collect(Collectors.toMap(
            gi -> gi,
            gi -> ((Gate.OutputGate) network.gates().get(gi)).type()
        )));
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

      System.out.printf("BEFORE:%n%s%n".formatted(
          current.entrySet().stream().map(e -> "\t%s\t%2d : %s".formatted(
              e.getKey(), e.getValue().size(), e.getValue()
          )).collect(Collectors.joining("%n"))
      ));

      for (int i = 0; i < network.gates().size(); i++) {
        int gi = i;
        Gate g = network.gates().get(gi);
        if (g instanceof Gate.InputGate) {
          if (!networkInputsQueue.isEmpty()) {
            network.wiresFrom(gi, 0).forEach(w -> next.put(w, List.of(networkInputsQueue.remove())));
          }
        } else if (g instanceof Gate.OutputGate) {
          network.wireTo(gi, 0).flatMap(w -> takeOne(current.get(w))).ifPresent(token -> outputs.put(gi, token));
        } else {
          List<Queue<Object>> inputQueues = IntStream.range(0, g.inputPorts().size())
              .mapToObj(pi -> network.wireTo(gi, pi).map(current::get).orElse(emptyQueue()))
              .toList();
          // check conditions and possibly apply function
          if (IntStream.range(0, g.inputPorts().size()).allMatch(pi -> inputQueues.get(pi).size() >= g.inputPorts().get(
              pi).n())) {
            Gate.Data localIn = Gate.Data.of(IntStream.range(0, g.inputPorts().size())
                .mapToObj(pi -> switch (g.inputPorts().get(pi).condition()) {
                  case EXACTLY -> takeExactly(inputQueues.get(pi), g.inputPorts().get(pi).n());
                  case AT_LEAST -> takeAll(inputQueues.get(pi));
                })
                .toList());
            try {
              Gate.Data localOut = g.operator().apply(localIn);
              // check number of outputs
              if (localOut.lines().size() != g.outputTypes().size()) {
                throw new RunnerException("Unexpected wrong number of outputs: %d expected, %d found".formatted(
                    g.outputTypes()
                        .size(), localOut.lines().size()
                ));
              }
              // put outputs
              IntStream.range(0, localOut.lines().size())
                  .forEach(pi -> network.wiresFrom(gi, pi).forEach(w -> next.put(w, localOut.lines().get(pi))));
            } catch (RuntimeException e) {
              throw new RunnerException("Cannot run %s on %s".formatted(g, localIn), e);
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
      State state = new State(
          current.entrySet()
              .stream()
              .map(e -> new State.WireState(e.getKey(), actualTypes.get(e.getKey()), e.getValue().size()))
              .toList()
      );
      if (state.nOfTokens() > maxTokens) {
        throw new RunnerException("Exceeded number of tokens: %d > %d".formatted(state.nOfTokens(), maxTokens));
      }
      states.add(state);
      // increment k
      k = k + 1;
    }
    // check output
    if (!outputTypes.keySet().equals(outputs.keySet())) {
      throw new RunnerException("Unexpected output gates: %s expected, %s found".formatted(
          outputTypes.keySet(),
          outputs.keySet()
      ));
    }
    for (int gi : outputTypes.keySet()) {
      if (!outputTypes.get(gi).matches(outputs.get(gi))) {
        throw new RunnerException("Invalid output type for input %d of type %s: %s".formatted(
            gi,
            outputTypes.get(gi),
            outputs.get(gi).getClass()
        ));
      }
    }
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
