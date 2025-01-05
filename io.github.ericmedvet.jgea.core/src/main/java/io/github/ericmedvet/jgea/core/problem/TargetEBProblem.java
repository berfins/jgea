package io.github.ericmedvet.jgea.core.problem;

import io.github.ericmedvet.jgea.core.util.IndexedProvider;

import java.util.function.Function;

public interface TargetEBProblem<S, EI, EO, EQ, Q> extends ExampleBasedProblem<S, EI, EO, EQ, Q> {
  Function<? super EI, ? extends EO> target();

  IndexedProvider<EI> inputProvider();

  IndexedProvider<EI> validationInputProvider();

  @Override
  default IndexedProvider<Example<EI, EO>> caseProvider() {
    return inputProvider().then(ei -> new Example<>(ei, target().apply(ei)));
  }

  @Override
  default IndexedProvider<Example<EI, EO>> validationCaseProvider() {
    return validationInputProvider().then(ei -> new Example<>(ei, target().apply(ei)));
  }
}
