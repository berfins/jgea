package io.github.ericmedvet.jgea.problem.programsynthesis;

import io.github.ericmedvet.jgea.core.fitness.CaseBasedFitness;
import io.github.ericmedvet.jgea.core.representation.programsynthesis.Program;
import io.github.ericmedvet.jgea.core.representation.programsynthesis.ProgramExecutionException;
import io.github.ericmedvet.jgea.core.representation.programsynthesis.type.Type;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

public class ProgramSynthesisFitness implements CaseBasedFitness<Program, List<Object>, List<Object>, Double> {
  public enum Metric{SUCCESS_RATE, SIMILARIY}

  private final Program targetProgram;
  private final List<List<Object>> caseInputs;
  private final List<List<Object>> caseOutputs;
  private final Metric metric;

  public ProgramSynthesisFitness(Program targetProgram, List<List<Object>> caseInputs, Metric metric) {
    this.targetProgram = targetProgram;
    this.caseInputs = caseInputs;
    this.metric = metric;
    caseOutputs = caseInputs.stream().map(inputs -> safelyRun(targetProgram, inputs)).toList();
  }

  @Override
  public Function<List<List<Object>>, Double> aggregateFunction() {
    return computedOutputs -> {
      if (computedOutputs.size()==caseOutputs.size()) {
        throw new IllegalArgumentException("Computed and actual output have different sizes: %d and %d".formatted(computedOutputs.size(),caseOutputs.size()));
      }
      return IntStream.range(0, computedOutputs.size())
          .mapToDouble(i -> switch (metric) {
            case SUCCESS_RATE -> success(caseOutputs.get(i), computedOutputs.get(i), targetProgram.outputTypes());
            case SIMILARIY -> 1d; // TODO complete
          })
          .average().orElseThrow();
    };
  }

  private static double success(List<Object> actualOutputs, List<Object> computedOutputs, List<Type> types) {
    if (actualOutputs==null && computedOutputs==null) {
      return 1d;
    }
    if (actualOutputs==null && computedOutputs!=null) {
      return 0d;
    }
    if (actualOutputs!=null && computedOutputs==null) {
      return 0d;
    }
    if (actualOutputs.size()!=computedOutputs.size()) {
      return 0d;
    }
    if (actualOutputs.equals(computedOutputs)) {
      return 1d;
    }
    return 0d;
  }

  @Override
  public BiFunction<Program, List<Object>, List<Object>> caseFunction() {
    return ProgramSynthesisFitness::safelyRun;
  }

  @Override
  public IntFunction<List<Object>> caseProvider() {
    return caseInputs::get;
  }

  @Override
  public int nOfCases() {
    return caseInputs.size();
  }

  private static List<Object> safelyRun(Program program, List<Object> inputs) {
    try {
      return program.run(inputs);
    } catch (ProgramExecutionException e) {
      return null;
    }
  }

}
