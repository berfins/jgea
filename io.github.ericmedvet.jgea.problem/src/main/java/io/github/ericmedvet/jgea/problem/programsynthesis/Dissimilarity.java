package io.github.ericmedvet.jgea.problem.programsynthesis;

import io.github.ericmedvet.jgea.core.distance.Distance;
import io.github.ericmedvet.jgea.core.representation.programsynthesis.type.Type;

import java.util.List;
import java.util.stream.IntStream;

public class Dissimilarity implements Distance<List<Object>> {
  private final List<Type> types;

  public Dissimilarity(List<Type> types) {
    this.types = types;
  }

  @Override
  public Double apply(List<Object> os1, List<Object> os2) {
    if (os1 == null && os2 == null) {
      return 0d;
    }
    if (os1 == null) {
      return Double.POSITIVE_INFINITY;
    }
    if (os2 == null) {
      return Double.POSITIVE_INFINITY;
    }
    if (os1.size() != types.size() || os2.size() != types.size()) {
      throw new IllegalArgumentException("Wrong sizes: %d expected, %d and %d found".formatted(
          types.size(),
          os1.size(),
          os2.size()
      ));
    }
    return IntStream.range(0, types.size())
        .mapToDouble(i -> types.get(i).dissimilarity(os1.get(i), os2.get(i)))
        .average()
        .orElse(0d);
  }
}
