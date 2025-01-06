package io.github.ericmedvet.jgea.core.problem;

import io.github.ericmedvet.jgea.core.util.IndexedProvider;

import java.util.function.Function;

public abstract class PrecomputedTargetEBProblem<S, EI, EO, EQ, Q> implements TargetEBProblem<S, EI, EO, EQ, Q> {

  private final Function<? super EI, ? extends EO> target;
  private final IndexedProvider<EI> inputProvider;
  private final IndexedProvider<EI> validationInputProvider;
  private final IndexedProvider<Example<EI, EO>> caseProvider;
  private final IndexedProvider<Example<EI, EO>> validationCaseProvider;

  public PrecomputedTargetEBProblem(
      Function<? super EI, ? extends EO> target,
      IndexedProvider<EI> inputProvider,
      IndexedProvider<EI> validationInputProvider
  ) {
    this.target = target;
    this.inputProvider = inputProvider;
    this.validationInputProvider = validationInputProvider;
    caseProvider = inputProvider.then(ei -> new Example<>(ei,target.apply(ei)));
    validationCaseProvider = validationInputProvider.then(ei -> new Example<>(ei,target.apply(ei)));
  }

  @Override
  public Function<? super EI, ? extends EO> target() {
    return target;
  }

  @Override
  public IndexedProvider<EI> inputProvider() {
    return inputProvider;
  }

  @Override
  public IndexedProvider<EI> validationInputProvider() {
    return validationInputProvider;
  }

  @Override
  public IndexedProvider<Example<EI, EO>> caseProvider() {
    return caseProvider;
  }

  @Override
  public IndexedProvider<Example<EI, EO>> validationCaseProvider() {
    return validationCaseProvider;
  }
}
