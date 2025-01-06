package io.github.ericmedvet.jgea.problem.booleanfunction.synthetic;

import io.github.ericmedvet.jgea.core.util.IndexedProvider;
import io.github.ericmedvet.jgea.problem.booleanfunction.BooleanFunction;
import io.github.ericmedvet.jgea.problem.booleanfunction.BooleanUtils;
import io.github.ericmedvet.jsdynsym.core.numerical.MultivariateRealFunction;

import java.util.List;
import java.util.stream.IntStream;

public class EvenParity extends PrecomputedSyntheticBRProblem {
  public EvenParity(List<Metric> metrics, int n) {
    super(
        BooleanFunction.from(inputs -> new boolean[]{IntStream.range(0, inputs.length).map(i -> inputs[i] ? 1 : 0).sum() % 2 == 0}, n, 1),
        IndexedProvider.from(BooleanUtils.buildCompleteObservations(MultivariateRealFunction.varNames("x", n).toArray(String[]::new))),
        IndexedProvider.from(BooleanUtils.buildCompleteObservations(MultivariateRealFunction.varNames("x", n).toArray(String[]::new))),
        metrics
    );
  }
}
