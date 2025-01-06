package io.github.ericmedvet.jgea.problem.regression.univariate;

import io.github.ericmedvet.jgea.core.problem.ProblemWithExampleSolution;
import io.github.ericmedvet.jgea.core.problem.SimpleEBMOProblem;
import io.github.ericmedvet.jgea.core.representation.NamedUnivariateRealFunction;
import io.github.ericmedvet.jgea.core.util.Misc;
import io.github.ericmedvet.jnb.datastructure.TriFunction;
import io.github.ericmedvet.jsdynsym.core.numerical.UnivariateRealFunction;

import java.util.List;
import java.util.Map;
import java.util.SequencedMap;
import java.util.function.BiFunction;
import java.util.function.Function;

public interface UnivariateRegressionProblem extends SimpleEBMOProblem<NamedUnivariateRealFunction, Map<String, Double>,
    Double, UnivariateRegressionProblem.Outcome, Double>, ProblemWithExampleSolution<NamedUnivariateRealFunction> {
  record Outcome(double actual, double predicted) {}

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

    @Override
    public String toString() {
      return name().toLowerCase();
    }
  }

  List<Metric> metrics();
  String yVarName();

  @Override
  default SequencedMap<String, Objective<List<Outcome>, Double>> aggregateObjectives() {
    return metrics().stream().collect(Misc.toSequencedMap(
        Enum::toString,
        m -> new Objective<>(m, Double::compareTo)
    ));
  }

  @Override
  default TriFunction<Map<String, Double>, Double, Double, Outcome> errorFunction() {
    return (input, actualY, preditectY) -> new Outcome(actualY, preditectY);
  }

  @Override
  default BiFunction<NamedUnivariateRealFunction, Map<String, Double>, Double> predictFunction() {
    return NamedUnivariateRealFunction::computeAsDouble;
  }

  @Override
  default NamedUnivariateRealFunction example() {
    Example<Map<String, Double>, Double> example = caseProvider().first();
    return NamedUnivariateRealFunction.from(
        UnivariateRealFunction.from(inputs -> 0d, example.input().size()),
        example.input().keySet().stream().sorted().toList(),
        yVarName()
    );
  }
}
