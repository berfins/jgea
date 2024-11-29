package io.github.ericmedvet.jgea.core.fitness;

import io.github.ericmedvet.jgea.core.util.IndexedProvider;

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
      IndexedProvider<Example<EI, EO>> caseProvider
  ) {
    record HardExampleBasedFitness<S, EI, EO, EQ, AQ>(
        Function<List<EQ>, AQ> aggregateFunction,
        BiFunction<S, EI, EO> predictFunction,
        BiFunction<EO, EO, EQ> errorFunction,
        IndexedProvider<Example<EI, EO>> caseProvider
    ) implements ExampleBasedFitness<S, EI, EO, EQ, AQ> {}
    return new HardExampleBasedFitness<>(aggregateFunction, predictFunction, errorFunction, caseProvider);
  }

  static <S, EI, EO, EQ, AQ> ExampleBasedFitness<S, EI, EO, EQ, AQ> from(
      Function<List<EQ>, AQ> aggregateFunction,
      BiFunction<S, EI, EO> predictFunction,
      BiFunction<EO, EO, EQ> errorFunction,
      IndexedProvider<EI> inputs,
      Function<EI, EO> oracle
  ) {
    return from(
        aggregateFunction,
        predictFunction,
        errorFunction,
        inputs.then(ei -> new Example<>(ei, oracle.apply(ei)))
    );
  }

  static <S, EI, EO, EQ, AQ> ExampleBasedFitness<S, EI, EO, EQ, AQ> from(
      Function<List<EQ>, AQ> aggregateFunction,
      BiFunction<S, EI, EO> predictFunction,
      BiFunction<EO, EO, EQ> errorFunction,
      IndexedProvider<EI> inputs,
      S target
  ) {
    Function<EI, EO> oracle = ei -> predictFunction.apply(target, ei);
    return from(aggregateFunction, predictFunction, errorFunction, inputs, oracle);
  }

  @Override
  default BiFunction<S, Example<EI, EO>, EQ> caseFunction() {
    return (s, e) -> errorFunction().apply(e.output, predictFunction().apply(s, e.input));
  }

}
