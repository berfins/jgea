package io.github.ericmedvet.jgea.problem.booleanfunction.synthetic;

import io.github.ericmedvet.jgea.core.problem.PrecomputedTargetEBProblem;
import io.github.ericmedvet.jgea.core.util.IndexedProvider;
import io.github.ericmedvet.jgea.problem.booleanfunction.BooleanFunction;
import io.github.ericmedvet.jgea.problem.booleanfunction.BooleanRegressionProblem;

import java.util.List;
import java.util.SequencedMap;
import java.util.function.Function;

public class PrecomputedSyntheticBRProblem extends PrecomputedTargetEBProblem<BooleanFunction, boolean[], boolean[], BooleanRegressionProblem.Outcome, SequencedMap<String, Double>> implements SyntheticBRProblem {

  private final List<Metric> metrics;

  public PrecomputedSyntheticBRProblem(
      Function<? super boolean[], ? extends boolean[]> target,
      IndexedProvider<boolean[]> inputProvider,
      IndexedProvider<boolean[]> validationInputProvider,
      List<Metric> metrics
  ) {
    super(target, inputProvider, validationInputProvider);
    this.metrics = metrics;
  }

  @Override
  public List<Metric> metrics() {
    return metrics;
  }
}
