package io.github.ericmedvet.jgea.core.problem;

import io.github.ericmedvet.jnb.datastructure.TriFunction;

import java.util.function.BiFunction;

public interface ExampleBasedProblem<S, EI, EO, EQ, Q> extends CaseBasedProblem<S, ExampleBasedProblem.Example<EI,
    EO>, EQ, Q> {
  record Example<I, O>(I input, O output) {}

  TriFunction<EI, EO, EO, EQ> errorFunction();

  BiFunction<S, EI, EO> predictFunction();

  @Override
  default BiFunction<S, Example<EI, EO>, EQ> caseFunction() {
    return (s, e) -> errorFunction().apply(e.input, e.output, predictFunction().apply(s, e.input));
  }
}
