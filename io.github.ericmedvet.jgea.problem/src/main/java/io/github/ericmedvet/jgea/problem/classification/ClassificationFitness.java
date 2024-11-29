package io.github.ericmedvet.jgea.problem.classification;

import io.github.ericmedvet.jgea.core.fitness.CaseBasedFitness;
import io.github.ericmedvet.jgea.core.fitness.ExampleBasedFitness;

import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

public interface ClassificationFitness<X, Y extends Enum<Y>> extends ExampleBasedFitness<Classifier<X, Y>, X, Y,
    ClassificationFitness.Outcome<Y>, Double> {

  enum Metric {
    ERROR_RATE,
    WEIGHTED_ERROR_RATE;
  }

  record Outcome<Y extends Enum<Y>>(Y actual, Y predicted) {}

  private static <Y extends Enum<Y>> double errorRate(Collection<Outcome<Y>> outcomes) {
    return outcomes.stream()
        .mapToDouble(o -> o.actual.equals(o.predicted) ? 0d : 1d)
        .average()
        .orElseThrow();
  }

  static <X, Y extends Enum<Y>> ClassificationFitness<X, Y> from(
      Metric metric,
      IntFunction<Example<X, Y>> caseProvider,
      int nOfCases
  ) {
    Function<List<Outcome<Y>>, Double> f = switch (metric) {
      case ERROR_RATE -> ClassificationFitness::errorRate;
      case WEIGHTED_ERROR_RATE -> ClassificationFitness::weightedErrorRate;
    };
    return from(f, caseProvider, nOfCases);
  }

  static <X, Y extends Enum<Y>> ClassificationFitness<X, Y> from(
      Metric metric,
      List<Example<X, Y>> cases
  ) {
    return from(metric, cases::get, cases.size());
  }

  static <X, Y extends Enum<Y>> ClassificationFitness<X, Y> from(
      Function<List<Outcome<Y>>, Double> aggregateFunction,
      IntFunction<Example<X, Y>> caseProvider,
      int nOfCases
  ) {
    record HardClassificationFitness<X, Y extends Enum<Y>>(
        Function<List<Outcome<Y>>, Double> aggregateFunction,
        IntFunction<Example<X, Y>> caseProvider,
        int nOfCases
    ) implements ClassificationFitness<X, Y> {}
    return new HardClassificationFitness<>(aggregateFunction, caseProvider, nOfCases);
  }

  private static <Y extends Enum<Y>> double weightedErrorRate(Collection<Outcome<Y>> outcomes) {
    return outcomes.stream()
        .collect(Collectors.groupingBy(o -> o.actual))
        .values()
        .stream()
        .mapToDouble(ClassificationFitness::errorRate)
        .average()
        .orElseThrow();
  }

  @Override
  default BiFunction<Y, Y, Outcome<Y>> errorFunction() {
    return Outcome::new;
  }

  @Override
  default BiFunction<Classifier<X, Y>, X, Y> predictFunction() {
    return Classifier::classify;
  }
}
