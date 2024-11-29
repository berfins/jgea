package io.github.ericmedvet.jgea.core.fitness;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;

public interface ExampleBasedFitness<S, EI, EO, CO, AF> extends CaseBasedFitness<S, ExampleBasedFitness.Example<EI,
    EO>, CO, AF> {
  record Example<I, O>(I input, O output) {}

  BiFunction<EO, EO, CO> errorFunction();

  BiFunction<S, EI, EO> predictFunction();

  static <S, EI, EO, CO, AF> ExampleBasedFitness<S, EI, EO, CO, AF> from(
      Function<List<CO>, AF> aggregateFunction,
      BiFunction<S, EI, EO> predictFunction,
      BiFunction<EO, EO, CO> errorFunction,
      IntFunction<Example<EI, EO>> caseProvider,
      int nOfCases
  ) {
    record HardExampleBasedFitness<S, EI, EO, CO, AF>(
        Function<List<CO>, AF> aggregateFunction,
        BiFunction<S, EI, EO> predictFunction,
        BiFunction<EO, EO, CO> errorFunction,
        IntFunction<Example<EI, EO>> caseProvider,
        int nOfCases
    ) implements ExampleBasedFitness<S, EI, EO, CO, AF> {}
    return new HardExampleBasedFitness<>(aggregateFunction, predictFunction, errorFunction, caseProvider, nOfCases);
  }

  static <S, EI, EO, CO, AF> ExampleBasedFitness<S, EI, EO, CO, AF> from(
      Function<List<CO>, AF> aggregateFunction,
      BiFunction<S, EI, EO> predictFunction,
      BiFunction<EO, EO, CO> errorFunction,
      List<Example<EI, EO>> examples
  ) {
    return from(aggregateFunction, predictFunction, errorFunction, examples::get, examples.size());
  }

  @Override
  default BiFunction<S, Example<EI, EO>, CO> caseFunction() {
    return (s, e) -> errorFunction().apply(e.output, predictFunction().apply(s, e.input));
  }

}
