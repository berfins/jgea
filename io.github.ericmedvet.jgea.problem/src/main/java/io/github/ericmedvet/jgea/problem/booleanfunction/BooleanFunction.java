package io.github.ericmedvet.jgea.problem.booleanfunction;

import io.github.ericmedvet.jnb.datastructure.NamedFunction;

import java.util.function.Function;

public interface BooleanFunction extends Function<boolean[], boolean[]> {
  int nOfInputs();

  int nOfOutputs();

  static BooleanFunction from(Function<boolean[], boolean[]> function, int nOfInputs, int nOfOutputs) {
    record HardBooleanFunction(
        Function<boolean[], boolean[]> function, int nOfInputs, int nOfOutputs
    ) implements BooleanFunction {
      @Override
      public boolean[] apply(boolean[] inputs) {
        return function.apply(inputs);
      }

      @Override
      public String toString() {
        return NamedFunction.name(HardBooleanFunction.this.function);
      }
    }
    return new HardBooleanFunction(function, nOfInputs, nOfOutputs);
  }

}
