package io.github.ericmedvet.jgea.core.problem;

import io.github.ericmedvet.jgea.core.fitness.TargetBasedFitness;

public interface TargetBasedProblem<S, EI, EO, EQ, AQ> extends ProblemWithExampleSolution<S>,
    ProblemWithValidation<S, AQ> {
  @Override
  TargetBasedFitness<S, EI, EO, EQ, AQ> qualityFunction();

  @Override
  TargetBasedFitness<S, EI, EO, EQ, AQ> validationQualityFunction();

  @Override
  default S example() {
    return qualityFunction().target();
  }
}
