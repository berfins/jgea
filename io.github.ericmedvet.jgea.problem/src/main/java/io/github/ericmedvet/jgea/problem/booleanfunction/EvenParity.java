package io.github.ericmedvet.jgea.problem.booleanfunction;

import java.util.stream.IntStream;

public record EvenParity(int n) implements BooleanFunction {
  @Override
  public int nOfInputs() {
    return n;
  }

  @Override
  public int nOfOutputs() {
    return 1;
  }

  @Override
  public boolean[] apply(boolean[] inputs) {
    return new boolean[]{IntStream.range(0, inputs.length).map(i -> inputs[i] ? 1 : 0).sum() % 2 == 0};
  }

  @Override
  public String toString() {
    return "evenParity(%d)".formatted(n);
  }
}
