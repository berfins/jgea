package io.github.ericmedvet.jgea.problem.classification;

import io.github.ericmedvet.jgea.core.fitness.ExampleBasedFitness;
import io.github.ericmedvet.jnb.datastructure.Pair;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public interface ClassificationFitness<X,Y extends Enum<Y>> extends ExampleBasedFitness<Classifier<X,Y>,X,Y, ClassificationFitness.Outcome<Y>,Double> {

  record Outcome<Y extends Enum<Y>>(Y actual, Y predicted) {}

  enum Metric implements Function<List<Outcome<? extends Enum<?>>>, Double> {
    ERROR(null), WEIGHTED_ERROR(null); //TODO
    private final Function<List<Outcome<? extends Enum<?>>>, Double> f;

    Metric(Function<List<Outcome<? extends Enum<?>>>, Double> f) {
      this.f = f;
    }

    @Override
    public Double apply(List<Outcome<? extends Enum<?>>> outcomes) {
      return f.apply(outcomes);
    }
  }

  @Override
  default Function<List<Outcome<Y>>, Double> aggregateFunction() {
    return null;
  }

  @Override
  default BiFunction<Classifier<X, Y>, X, Y> predictFunction() {
    return Classifier::classify;
  }

  @Override
  default BiFunction<Y, Y, Outcome<Y>> errorFunction() {
    return Outcome::new;
  }
}
