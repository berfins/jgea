package io.github.ericmedvet.jgea.core.problem;

import io.github.ericmedvet.jgea.core.util.IndexedProvider;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public interface CaseBasedProblem<S, C, CQ, Q> extends QualityBasedProblem<S, Q>, ProblemWithValidation<S, Q> {

  Function<List<CQ>, Q> aggregateFunction();

  BiFunction<S, C, CQ> caseFunction();

  IndexedProvider<C> caseProvider();

  IndexedProvider<C> validationCaseProvider();

  @Override
  default Function<S, Q> validationQualityFunction() {
    return s -> aggregateFunction().apply(validationCaseProvider().stream()
        .map(c -> caseFunction().apply(s, c))
        .toList());
  }

  @Override
  default Function<S, Q> qualityFunction() {
    return s -> aggregateFunction().apply(caseProvider().stream().map(c -> caseFunction().apply(s, c)).toList());
  }
}
