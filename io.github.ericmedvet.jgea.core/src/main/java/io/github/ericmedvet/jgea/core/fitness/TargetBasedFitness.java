package io.github.ericmedvet.jgea.core.fitness;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;

public interface TargetBasedFitness<S, EI, EO, EQ, AF> extends ExampleBasedFitness<S, EI, EO, EQ, AF> {
  IntFunction<EI> inputCaseProvider();

  S target();

  @Override
  default IntFunction<Example<EI, EO>> caseProvider() {
    return i -> new Example<>(inputCaseProvider().apply(i), predictFunction().apply(target(), inputCaseProvider().apply(i)));
  }

  static <S, EI, EO, EQ, AQ> TargetBasedFitness<S, EI, EO, EQ, AQ> from(
      Function<List<EQ>, AQ> aggregateFunction,
      BiFunction<S, EI, EO> predictFunction,
      BiFunction<EO, EO, EQ> errorFunction,
      S target,
      IntFunction<EI> inputCaseProvider,
      int nOfCases
  ) {
    record HardOracleBasedFitness<S, EI, EO, EQ, AQ>(
        Function<List<EQ>, AQ> aggregateFunction,
        BiFunction<S, EI, EO> predictFunction,
        BiFunction<EO, EO, EQ> errorFunction,
        S target,
        IntFunction<EI> inputCaseProvider,
        int nOfCases
    ) implements TargetBasedFitness<S, EI, EO, EQ, AQ> {}
    return new HardOracleBasedFitness<>(
        aggregateFunction,
        predictFunction,
        errorFunction,
        target,
        inputCaseProvider,
        nOfCases
    );
  }

  static <S, EI, EO, EQ, AQ> TargetBasedFitness<S, EI, EO, EQ, AQ> from(
      Function<List<EQ>, AQ> aggregateFunction,
      BiFunction<S, EI, EO> predictFunction,
      BiFunction<EO, EO, EQ> errorFunction,
      S target,
      List<EI> inputCases
  ) {
    return from(aggregateFunction, predictFunction, errorFunction, target, inputCases::get, inputCases.size());
  }

}
