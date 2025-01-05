package io.github.ericmedvet.jgea.core.problem;

import io.github.ericmedvet.jgea.core.util.Misc;

import java.util.Comparator;
import java.util.List;
import java.util.SequencedMap;
import java.util.function.Function;

public interface SimpleEBMOProblem<S, EI, EO, EQ, O> extends EBMOProblem<S, EI, EO, EQ, SequencedMap<String, O>, O>,
    SimpleMOProblem<S, O> {

  SequencedMap<String, Objective<List<EQ>, O>> aggregateObjectives();

  @Override
  default Function<List<EQ>, SequencedMap<String, O>> aggregateFunction() {
    return eqs -> Misc.sequencedTransformValues(aggregateObjectives(), obj -> obj.function().apply(eqs));
  }

  @Override
  default SequencedMap<String, Comparator<O>> comparators() {
    return Misc.sequencedTransformValues(aggregateObjectives(), Objective::comparator);
  }
}
