package io.github.ericmedvet.jgea.problem.regression.univariate.synthetic;

import io.github.ericmedvet.jgea.core.problem.TargetEBProblem;
import io.github.ericmedvet.jgea.core.representation.NamedUnivariateRealFunction;
import io.github.ericmedvet.jgea.core.util.IndexedProvider;
import io.github.ericmedvet.jgea.problem.regression.univariate.URegressionProblem;
import io.github.ericmedvet.jsdynsym.core.numerical.MultivariateRealFunction;
import io.github.ericmedvet.jsdynsym.core.numerical.UnivariateRealFunction;

import java.util.List;
import java.util.Map;
import java.util.SequencedMap;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public interface SyntheticURProblem extends URegressionProblem, TargetEBProblem<NamedUnivariateRealFunction, Map<String, Double>, Double, URegressionProblem.Outcome, SequencedMap<String, Double>> {
  @Override
  default String yVarName() {
    return "y";
  }

  static IndexedProvider<Map<String, Double>> tupleProvider(List<double[]> points) {
    List<String> names = MultivariateRealFunction.varNames("x", points.getFirst().length);
    return IndexedProvider.from(
        points.stream()
            .map(
                p -> IntStream.range(0, p.length)
                    .boxed()
                    .collect(
                        Collectors.toMap(
                            names::get,
                            j -> p[j]
                        )
                    )
            )
            .toList()
    );
  }

  static Function<? super Map<String, Double>, ? extends Double> function(ToDoubleFunction<double[]> f, int nOfInputs) {
    return function(f, MultivariateRealFunction.varNames("x", nOfInputs));
  }

  static Function<? super Map<String, Double>, ? extends Double> function(ToDoubleFunction<double[]> f, List<String> xVarNames) {
    return inputs -> f.applyAsDouble(xVarNames.stream().mapToDouble(inputs::get).toArray());
  }

}
