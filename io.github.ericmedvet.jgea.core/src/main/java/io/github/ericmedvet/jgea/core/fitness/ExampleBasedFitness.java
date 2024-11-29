package io.github.ericmedvet.jgea.core.fitness;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;

public interface ExampleBasedFitness<S, EI, EO, EQ, AQ> extends CaseBasedFitness<S, ExampleBasedFitness.Example<EI,
    EO>, EQ, AQ> {
  record Example<I, O>(I input, O output) {}

  BiFunction<EO, EO, EQ> errorFunction();

  BiFunction<S, EI, EO> predictFunction();

  static <S, EI, EO, EQ, AQ> ExampleBasedFitness<S, EI, EO, EQ, AQ> from(
      Function<List<EQ>, AQ> aggregateFunction,
      BiFunction<S, EI, EO> predictFunction,
      BiFunction<EO, EO, EQ> errorFunction,
      IntFunction<Example<EI, EO>> caseProvider,
      int nOfCases
  ) {
    record HardExampleBasedFitness<S, EI, EO, EQ, AQ>(
        Function<List<EQ>, AQ> aggregateFunction,
        BiFunction<S, EI, EO> predictFunction,
        BiFunction<EO, EO, EQ> errorFunction,
        IntFunction<Example<EI, EO>> caseProvider,
        int nOfCases
    ) implements ExampleBasedFitness<S, EI, EO, EQ, AQ> {}
    return new HardExampleBasedFitness<>(aggregateFunction, predictFunction, errorFunction, caseProvider, nOfCases);
  }

  static <S, EI, EO, EQ, AQ> ExampleBasedFitness<S, EI, EO, EQ, AQ> from(
      Function<List<EQ>, AQ> aggregateFunction,
      BiFunction<S, EI, EO> predictFunction,
      BiFunction<EO, EO, EQ> errorFunction,
      List<Example<EI, EO>> examples
  ) {
    return from(aggregateFunction, predictFunction, errorFunction, examples::get, examples.size());
  }

  @Override
  default BiFunction<S, Example<EI, EO>, EQ> caseFunction() {
    return (s, e) -> errorFunction().apply(e.output, predictFunction().apply(s, e.input));
  }

}
