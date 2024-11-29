package io.github.ericmedvet.jgea.core.fitness;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;

public interface OracleBasedFitness<S, EI, EO, EQ, AF> extends ExampleBasedFitness<S, EI, EO, EQ, AF> {
  IntFunction<EI> inputCaseProvider();

  Function<EI, EO> target();

  @Override
  default IntFunction<Example<EI, EO>> caseProvider() {
    return i -> new Example<>(inputCaseProvider().apply(i), target().apply(inputCaseProvider().apply(i)));
  }

  static <S, EI, EO, EQ, AQ> OracleBasedFitness<S, EI, EO, EQ, AQ> from(
      Function<List<EQ>, AQ> aggregateFunction,
      BiFunction<S, EI, EO> predictFunction,
      BiFunction<EO, EO, EQ> errorFunction,
      Function<EI, EO> target,
      IntFunction<EI> inputCaseProvider,
      int nOfCases
  ) {
    record HardOracleBasedFitness<S, EI, EO, EQ, AQ>(
        Function<List<EQ>, AQ> aggregateFunction,
        BiFunction<S, EI, EO> predictFunction,
        BiFunction<EO, EO, EQ> errorFunction,
        Function<EI, EO> target,
        IntFunction<EI> inputCaseProvider,
        int nOfCases
    ) implements OracleBasedFitness<S, EI, EO, EQ, AQ> {}
    return new HardOracleBasedFitness<>(
        aggregateFunction,
        predictFunction,
        errorFunction,
        target,
        inputCaseProvider,
        nOfCases
    );
  }

  static <S, EI, EO, EQ, AQ> OracleBasedFitness<S, EI, EO, EQ, AQ> from(
      Function<List<EQ>, AQ> aggregateFunction,
      BiFunction<S, EI, EO> predictFunction,
      BiFunction<EO, EO, EQ> errorFunction,
      Function<EI, EO> target,
      List<EI> inputCases
  ) {
    return from(aggregateFunction, predictFunction, errorFunction, target, inputCases::get, inputCases.size());
  }

  static <S, EI, EO, EQ, AQ> OracleBasedFitness<S, EI, EO, EQ, AQ> from(
      Function<List<EQ>, AQ> aggregateFunction,
      BiFunction<S, EI, EO> predictFunction,
      BiFunction<EO, EO, EQ> errorFunction,
      S target,
      List<EI> inputCases
  ) {
    return from(
        aggregateFunction,
        predictFunction,
        errorFunction,
        i -> predictFunction.apply(target, i),
        inputCases::get,
        inputCases.size()
    );
  }

}
