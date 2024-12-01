package io.github.ericmedvet.jgea.problem.regression.univariate;

import io.github.ericmedvet.jgea.core.fitness.ExampleBasedFitness;
import io.github.ericmedvet.jgea.core.representation.NamedUnivariateRealFunction;
import io.github.ericmedvet.jgea.core.util.IndexedProvider;
import io.github.ericmedvet.jgea.core.util.Misc;
import io.github.ericmedvet.jnb.datastructure.NamedFunction;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

public interface UnivariateRegressionFitness extends ExampleBasedFitness<NamedUnivariateRealFunction, Map<String,
    Double>, Map<String,
    Double>, UnivariateRegressionFitness.Outcome, Double> {

  enum Metric implements Function<List<Outcome>, Double> {
    MAE(
        ys -> ys.stream()
            .mapToDouble(y -> Math.abs(y.predicted - y.actual))
            .average()
            .orElse(Double.NaN)
    ), MSE(
        ys -> ys.stream()
            .mapToDouble(y -> (y.predicted - y.actual) * (y.predicted - y.actual))
            .average()
            .orElse(Double.NaN)
    ), RMSE(
        ys -> Math.sqrt(
            ys.stream()
                .mapToDouble(y -> (y.predicted - y.actual) * (y.predicted - y.actual))
                .average()
                .orElse(Double.NaN)
        )
    ), NMSE(
        ys -> ys.stream()
            .mapToDouble(y -> (y.predicted - y.actual) * (y.predicted - y.actual))
            .average()
            .orElse(Double.NaN) / ys.stream().mapToDouble(y -> y.actual).average().orElse(1d)
    );

    private final Function<List<Outcome>, Double> function;

    Metric(Function<List<Outcome>, Double> function) {
      this.function = function;
    }

    @Override
    public Double apply(List<Outcome> ys) {
      return function.apply(ys);
    }
  }

  record Outcome(double actual, double predicted) {}

  String yVarName();

  static UnivariateRegressionFitness from(
      Function<List<Outcome>, Double> aggregateFunction,
      IndexedProvider<Example<Map<String, Double>, Map<String, Double>>> caseProvider,
      String yVarName
  ) {
    record HardUnivariateRegressionFitness(
        Function<List<Outcome>, Double> aggregateFunction,
        IndexedProvider<Example<Map<String, Double>, Map<String, Double>>> caseProvider,
        String yVarName
    ) implements UnivariateRegressionFitness {}
    return new HardUnivariateRegressionFitness(aggregateFunction, caseProvider, yVarName);
  }

  static UnivariateRegressionFitness from(
      Metric metric,
      IndexedProvider<Example<Map<String, Double>, Map<String, Double>>> caseProvider,
      String yVarName
  ) {
    return from(NamedFunction.from(metric, Misc.enumString(metric)), caseProvider, yVarName);
  }

  @Override
  default BiFunction<Map<String,
      Double>, Map<String,
      Double>, Outcome> errorFunction() {
    return (actualYs, predictedYs) -> new Outcome(actualYs.get(yVarName()), predictedYs.get(yVarName()));
  }

  @Override
  default BiFunction<NamedUnivariateRealFunction, Map<String, Double>, Map<String, Double>> predictFunction() {
    return NamedUnivariateRealFunction::compute;
  }

}
