package io.github.ericmedvet.jgea.problem.regression.univariate.synthetic;

import io.github.ericmedvet.jgea.core.representation.NamedUnivariateRealFunction;
import io.github.ericmedvet.jgea.core.util.IndexedProvider;
import io.github.ericmedvet.jgea.problem.regression.univariate.UnivariateRegressionFitness;
import io.github.ericmedvet.jgea.problem.regression.univariate.UnivariateRegressionProblem;
import io.github.ericmedvet.jsdynsym.core.numerical.MultivariateRealFunction;
import io.github.ericmedvet.jsdynsym.core.numerical.UnivariateRealFunction;

import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class SyntheticURP implements UnivariateRegressionProblem {

  private final UnivariateRegressionFitness qualityFunction;
  private final UnivariateRegressionFitness validationQualityFunction;

  public SyntheticURP(
      ToDoubleFunction<double[]> function,
      List<double[]> points,
      List<double[]> validationPoints,
      UnivariateRegressionFitness.Metric metric
  ) {
    NamedUnivariateRealFunction target = NamedUnivariateRealFunction.from(
        UnivariateRealFunction.from(function, points.getFirst().length),
        MultivariateRealFunction.varNames("x", points.getFirst().length),
        "y"
    );
    UnivariateRegressionProblem problem = UnivariateRegressionProblem.from(
        metric,
        target,
        tupleProvider(points),
        tupleProvider(validationPoints)
    );
    qualityFunction = problem.qualityFunction();
    validationQualityFunction = problem.validationQualityFunction();
  }

  private static IndexedProvider<Map<String, Double>> tupleProvider(List<double[]> points) {
    List<String> names = MultivariateRealFunction.varNames("x", points.getFirst().length);
    return IndexedProvider.from(points.stream().map(p -> IntStream.range(0, p.length)
            .boxed()
            .collect(Collectors.toMap(
                names::get,
                j -> p[j]
            )))
        .toList());
  }

  @Override
  public UnivariateRegressionFitness qualityFunction() {
    return qualityFunction;
  }

  @Override
  public UnivariateRegressionFitness validationQualityFunction() {
    return validationQualityFunction;
  }
}
