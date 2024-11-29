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

// TODO use Map<String, Double> also as EO here
// TODO make NumericalDataset extend IndexedProvider<Map<String, Double>>

public interface UnivariateRegressionFitness extends ExampleBasedFitness<NamedUnivariateRealFunction, Map<String,
    Double>, Map.Entry<String,
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

  static UnivariateRegressionFitness from(
      Function<List<Outcome>, Double> aggregateFunction,
      IndexedProvider<Example<Map<String, Double>, Map.Entry<String, Double>>> caseProvider
  ) {
    record HardUnivariateRegressionFitness(
        Function<List<Outcome>, Double> aggregateFunction,
        IndexedProvider<Example<Map<String, Double>, Map.Entry<String, Double>>> caseProvider
    ) implements UnivariateRegressionFitness {}
    return new HardUnivariateRegressionFitness(aggregateFunction, caseProvider);
  }

  static UnivariateRegressionFitness from(
      Metric metric,
      IndexedProvider<Example<Map<String, Double>, Map.Entry<String, Double>>> caseProvider
  ) {
    return from(NamedFunction.from(metric, Misc.enumString(metric)), caseProvider);
  }

  @Override
  default BiFunction<Map.Entry<String,
      Double>, Map.Entry<String,
      Double>, Outcome> errorFunction() {
    return (ny1, ny2) -> new Outcome(ny1.getValue(), ny2.getValue());
  }

  @Override
  default BiFunction<NamedUnivariateRealFunction, Map<String, Double>, Map.Entry<String, Double>> predictFunction() {
    return (nurf, inputs) -> Map.entry(nurf.yVarName(), nurf.computeAsDouble(inputs));
  }

}
