package io.github.ericmedvet.jgea.core.problem;

import io.github.ericmedvet.jgea.core.fitness.ExampleBasedFitness;

public interface ExampleBasedProblem<S, EI, EO, EQ, AQ> extends ProblemWithExampleSolution<S>,
    ProblemWithValidation<S, AQ> {
  @Override
  ExampleBasedFitness<S, EI, EO, EQ, AQ> qualityFunction();

  @Override
  ExampleBasedFitness<S, EI, EO, EQ, AQ> validationQualityFunction();

}
