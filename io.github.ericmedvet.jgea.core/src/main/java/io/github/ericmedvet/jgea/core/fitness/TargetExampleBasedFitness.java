package io.github.ericmedvet.jgea.core.fitness;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;

public interface TargetExampleBasedFitness<S, EI, EO, CO, AF> extends ExampleBasedFitness<S, EI, EO, CO, AF> {
  IntFunction<EI> inputCaseProvider();

  Function<EI, EO> target();

  @Override
  default IntFunction<Example<EI, EO>> caseProvider() {
    return i -> new Example<>(inputCaseProvider().apply(i), target().apply(inputCaseProvider().apply(i)));
  }

  static <S, EI, EO, CO, AF> TargetExampleBasedFitness<S, EI, EO, CO, AF> from(
      Function<List<CO>, AF> aggregateFunction,
      BiFunction<S, EI, EO> predictFunction,
      BiFunction<EO, EO, CO> errorFunction,
      Function<EI, EO> target,
      IntFunction<EI> inputCaseProvider,
      int nOfCases
  ) {
    record HardTargetExampleBasedFitness<S, EI, EO, CO, AF>(
        Function<List<CO>, AF> aggregateFunction,
        BiFunction<S, EI, EO> predictFunction,
        BiFunction<EO, EO, CO> errorFunction,
        Function<EI, EO> target,
        IntFunction<EI> inputCaseProvider,
        int nOfCases
    ) implements TargetExampleBasedFitness<S, EI, EO, CO, AF> {}
    return new HardTargetExampleBasedFitness<>(
        aggregateFunction,
        predictFunction,
        errorFunction,
        target,
        inputCaseProvider,
        nOfCases
    );
  }

  static <S, EI, EO, CO, AF> TargetExampleBasedFitness<S, EI, EO, CO, AF> from(
      Function<List<CO>, AF> aggregateFunction,
      BiFunction<S, EI, EO> predictFunction,
      BiFunction<EO, EO, CO> errorFunction,
      Function<EI, EO> target,
      List<EI> inputCases
  ) {
    return from(aggregateFunction, predictFunction, errorFunction, target, inputCases::get, inputCases.size());
  }

  static <S, EI, EO, CO, AF> TargetExampleBasedFitness<S, EI, EO, CO, AF> from(
      Function<List<CO>, AF> aggregateFunction,
      BiFunction<S, EI, EO> predictFunction,
      BiFunction<EO, EO, CO> errorFunction,
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
