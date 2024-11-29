package io.github.ericmedvet.jgea.problem.booleanfunction;

import io.github.ericmedvet.jgea.core.fitness.ExampleBasedFitness;
import io.github.ericmedvet.jgea.core.util.IndexedProvider;
import io.github.ericmedvet.jgea.core.util.Misc;
import io.github.ericmedvet.jnb.datastructure.NamedFunction;

import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.IntStream;

public interface BooleanRegressionFitness extends ExampleBasedFitness<BooleanFunction, boolean[], boolean[], Integer,
    Double> {
  enum Metric {
    ERROR_RATE,
    AVG_DISTANCE
  }

  @Override
  default BiFunction<boolean[], boolean[], Integer> errorFunction() {
    return (actual, predicted) -> IntStream.range(0, Math.min(actual.length, predicted.length))
        .map(i -> actual[i] == predicted[i] ? 0 : 1).sum() + Math.abs(actual.length - predicted.length);
  }

  @Override
  default BiFunction<BooleanFunction, boolean[], boolean[]> predictFunction() {
    return Function::apply;
  }

  static BooleanRegressionFitness from(
      Function<List<Integer>, Double> aggregateFunction,
      IndexedProvider<Example<boolean[], boolean[]>> caseProvider
  ) {
    record HardBooleanRegressionFitness(
        Function<List<Integer>, Double> aggregateFunction,
        IndexedProvider<Example<boolean[], boolean[]>> caseProvider
    ) implements BooleanRegressionFitness {}
    return new HardBooleanRegressionFitness(aggregateFunction, caseProvider);
  }

  static BooleanRegressionFitness from(
      Metric metric,
      IndexedProvider<Example<boolean[], boolean[]>> caseProvider
  ) {
    Function<List<Integer>, Double> f = switch (metric) {
      case ERROR_RATE -> BooleanRegressionFitness::errorRate;
      case AVG_DISTANCE -> BooleanRegressionFitness::avgDistance;
    };
    return from(NamedFunction.from(f, Misc.enumString(metric)), caseProvider);
  }

  private static double errorRate(Collection<Integer> distances) {
    return (double) distances.stream().filter(d -> d != 0).count() / distances.size();
  }

  private static double avgDistance(Collection<Integer> distances) {
    return distances.stream().mapToDouble(Integer::doubleValue).average().orElseThrow();
  }

}
