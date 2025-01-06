package io.github.ericmedvet.jgea.problem.regression.univariate.synthetic;

import io.github.ericmedvet.jgea.core.problem.PrecomputedTargetEBProblem;
import io.github.ericmedvet.jgea.core.representation.NamedUnivariateRealFunction;
import io.github.ericmedvet.jgea.core.util.IndexedProvider;
import io.github.ericmedvet.jgea.problem.regression.univariate.UnivariateRegressionProblem;

import java.util.List;
import java.util.Map;
import java.util.SequencedMap;
import java.util.function.Function;

public class PrecomputedSyntheticURProblem extends PrecomputedTargetEBProblem<NamedUnivariateRealFunction, Map<String, Double>, Double, UnivariateRegressionProblem.Outcome, SequencedMap<String, Double>> implements SyntheticURProblem {

  private final List<Metric> metrics;

  public PrecomputedSyntheticURProblem(
      Function<? super Map<String, Double>, ? extends Double> target,
      IndexedProvider<Map<String, Double>> inputProvider,
      IndexedProvider<Map<String, Double>> validationInputProvider,
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
