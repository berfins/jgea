package io.github.ericmedvet.jgea.core.problem;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SequencedMap;
import java.util.stream.Collectors;

public interface SimpleMultiHomogeneousObjectiveProblem<S, Q> extends MultiHomogeneousObjectiveProblem<S, Map<String,
    Q>, Q> {
  SequencedMap<String, Comparator<Q>> comparators();

  @Override
  default SequencedMap<String, Objective<Map<String, Q>, Q>> objectives() {
    return comparators().entrySet().stream().collect(Collectors.toMap(
        Map.Entry::getKey,
        e -> new Objective<>(m -> m.get(e.getKey()), e.getValue()),
        (o1, o2) -> o1,
        LinkedHashMap::new
    ));
  }

}
