package io.github.ericmedvet.jgea.problem.classification;

import io.github.ericmedvet.jgea.core.problem.ExampleBasedProblem;

public interface ClassificationProblem<X,Y extends Enum<Y>> extends ExampleBasedProblem<Classifier<X,Y>,X,Y,Boolean,Double> {
  @Override
  ClassificationFitness<X,Y> qualityFunction();

  @Override
  ClassificationFitness<X,Y> validationQualityFunction();
}
