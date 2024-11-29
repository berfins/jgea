package io.github.ericmedvet.jgea.problem.booleanfunction;

public record MultipleOutputParallelMultiplier(int n) implements BooleanFunction {
  @Override
  public int nOfInputs() {
    return 2 * n;
  }

  @Override
  public int nOfOutputs() {
    return 2 * n;
  }

  @Override
  public boolean[] apply(boolean[] inputs) {
    boolean[] a1 = new boolean[n];
    boolean[] a2 = new boolean[n];
    System.arraycopy(inputs, 0, a1, 0, n);
    System.arraycopy(inputs, n, a2, 0, n);
    int n1 = BooleanUtils.fromBinary(a1);
    int n2 = BooleanUtils.fromBinary(a2);
    return BooleanUtils.toBinary(n1 * n2, 2 * n);
  }

  @Override
  public String toString() {
    return "mopm(%d)".formatted(n);
  }
}
