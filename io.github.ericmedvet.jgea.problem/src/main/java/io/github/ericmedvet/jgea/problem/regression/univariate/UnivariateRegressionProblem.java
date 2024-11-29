package io.github.ericmedvet.jgea.problem.regression.univariate;

import io.github.ericmedvet.jgea.core.fitness.ExampleBasedFitness;
import io.github.ericmedvet.jgea.core.problem.ExampleBasedProblem;
import io.github.ericmedvet.jgea.core.problem.ProblemWithExampleSolution;
import io.github.ericmedvet.jgea.core.problem.TotalOrderQualityBasedProblem;
import io.github.ericmedvet.jgea.core.representation.NamedUnivariateRealFunction;
import io.github.ericmedvet.jgea.core.util.IndexedProvider;
import io.github.ericmedvet.jsdynsym.core.numerical.UnivariateRealFunction;

import java.util.Comparator;
import java.util.Map;

public interface UnivariateRegressionProblem extends ExampleBasedProblem<NamedUnivariateRealFunction, Map<String,
    Double>, Map.Entry<String, Double>, UnivariateRegressionFitness.Outcome,
    Double>, TotalOrderQualityBasedProblem<NamedUnivariateRealFunction, Double>,
    ProblemWithExampleSolution<NamedUnivariateRealFunction> {

  @Override
  UnivariateRegressionFitness qualityFunction();

  @Override
  UnivariateRegressionFitness validationQualityFunction();

  @Override
  default NamedUnivariateRealFunction example() {
    ExampleBasedFitness.Example<Map<String, Double>, Map.Entry<String, Double>> example =
        qualityFunction().caseProvider()
            .first();
    return NamedUnivariateRealFunction.from(
        UnivariateRealFunction.from(inputs -> 0d, example.input().size()),
        example.input().keySet().stream().sorted().toList(),
        example.output().getKey()
    );
  }

  @Override
  default Comparator<Double> totalOrderComparator() {
    return Double::compareTo;
  }

  static UnivariateRegressionProblem from(
      UnivariateRegressionFitness qualityFunction,
      UnivariateRegressionFitness validationQualityFunction
  ) {
    record HardUnivariateRegressionProblem(
        UnivariateRegressionFitness qualityFunction,
        UnivariateRegressionFitness validationQualityFunction
    ) implements UnivariateRegressionProblem {}
    return new HardUnivariateRegressionProblem(qualityFunction, validationQualityFunction);
  }

  static UnivariateRegressionProblem from(
      UnivariateRegressionFitness.Metric metric,
      IndexedProvider<ExampleBasedFitness.Example<Map<String, Double>, Map.Entry<String, Double>>> caseProvider,
      IndexedProvider<ExampleBasedFitness.Example<Map<String, Double>, Map.Entry<String, Double>>> validationCaseProvider
  ) {
    return from(
        UnivariateRegressionFitness.from(metric, caseProvider),
        UnivariateRegressionFitness.from(metric, validationCaseProvider)
    );
  }

  static UnivariateRegressionProblem from(
      UnivariateRegressionFitness.Metric metric,
      NamedUnivariateRealFunction target,
      IndexedProvider<Map<String, Double>> inputProvider,
      IndexedProvider<Map<String, Double>> validationInputProvider
  ) {
    return from(
        metric,
        inputProvider.then(i -> new ExampleBasedFitness.Example<>(
            i,
            Map.entry(target.yVarName(), target.computeAsDouble(i))
        )),
        validationInputProvider.then(i -> new ExampleBasedFitness.Example<>(
            i,
            Map.entry(target.yVarName(), target.computeAsDouble(i))
        ))
    );
  }
}
