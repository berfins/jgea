package io.github.ericmedvet.jgea.problem.programsynthesis;

import io.github.ericmedvet.jgea.core.distance.Distance;
import io.github.ericmedvet.jgea.core.fitness.ExampleBasedFitness;
import io.github.ericmedvet.jgea.core.representation.programsynthesis.Program;
import io.github.ericmedvet.jgea.core.representation.programsynthesis.type.Type;
import io.github.ericmedvet.jgea.core.util.IndexedProvider;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public interface ProgramSynthesisFitness extends ExampleBasedFitness<Program, List<Object>, List<Object>,
    ProgramSynthesisFitness.Outcome, Double> {
  record Outcome(boolean exception, double distance) {}

  enum Metric {FAIL_RATE, AVG_DISSIMILARITY}

  enum Dissimilarity {RAW, NORMALIZED}

  Distance<List<Object>> outputsDistance();

  static ProgramSynthesisFitness from(
      Distance<List<Object>> outputsDistance,
      Function<List<Outcome>, Double> aggregateFunction,
      IndexedProvider<Example<List<Object>, List<Object>>> caseProvider
  ) {
    record HardProgramSynthesisFitness(
        Distance<List<Object>> outputsDistance,
        Function<List<Outcome>, Double> aggregateFunction,
        IndexedProvider<Example<List<Object>, List<Object>>> caseProvider
    ) implements ProgramSynthesisFitness {}
    return new HardProgramSynthesisFitness(outputsDistance, aggregateFunction, caseProvider);
  }

  static ProgramSynthesisFitness from(
      Metric metric,
      Dissimilarity dissimilarity,
      List<Type> types,
      IndexedProvider<Example<List<Object>, List<Object>>> caseProvider
  ) {
    Distance<List<Object>> outputsDistance = switch (dissimilarity) {
      case RAW -> new io.github.ericmedvet.jgea.problem.programsynthesis.Dissimilarity(types);
      case NORMALIZED -> new NormalizedDissimilarity(
          types,
          caseProvider.then(Example::output)
      );
    };
    Function<List<Outcome>, Double> aggregateFunction = switch (metric) {
      case FAIL_RATE -> os -> (double) (os.stream().filter(Outcome::exception).count() / os.size());
      case AVG_DISSIMILARITY -> os -> os.stream().mapToDouble(Outcome::distance).average().orElseThrow();
    };
    return from(outputsDistance, aggregateFunction, caseProvider);
  }

  @Override
  default BiFunction<List<Object>, List<Object>, Outcome> errorFunction() {
    return (actualOutputs, predictedOutputs) -> {
      if (actualOutputs == null && predictedOutputs == null) {
        return new Outcome(true, 0d);
      }
      if (actualOutputs == null) {
        return new Outcome(false, Double.POSITIVE_INFINITY);
      }
      if (predictedOutputs == null) {
        return new Outcome(true, Double.POSITIVE_INFINITY);
      }
      return new Outcome(false, outputsDistance().apply(actualOutputs, predictedOutputs));
    };
  }

  @Override
  default BiFunction<Program, List<Object>, List<Object>> predictFunction() {
    return Program::safelyRun;
  }
}
