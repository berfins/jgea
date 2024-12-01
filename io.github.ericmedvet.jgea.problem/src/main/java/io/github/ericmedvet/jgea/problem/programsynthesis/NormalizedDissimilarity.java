package io.github.ericmedvet.jgea.problem.programsynthesis;

import io.github.ericmedvet.jgea.core.distance.Distance;
import io.github.ericmedvet.jgea.core.representation.programsynthesis.type.Type;
import io.github.ericmedvet.jgea.core.util.IndexedProvider;
import io.github.ericmedvet.jnb.datastructure.DoubleRange;

import java.util.ArrayList;
import java.util.List;

public class NormalizedDissimilarity implements Distance<List<Object>> {

  private final Distance<List<Object>> rawDistance;
  private final DoubleRange rawRange;


  public NormalizedDissimilarity(List<Type> types, IndexedProvider<List<Object>> valueProvider) {
    rawDistance = new Dissimilarity(types);
    List<Double> dists = new ArrayList<>();
    for (int i = 0; i < valueProvider.size(); i++) {
      for (int j = 0; j < i; j++) {
        dists.add(rawDistance.apply(valueProvider.get(i), valueProvider.get(i)));
      }
    }
    rawRange = new DoubleRange(0, dists.stream().mapToDouble(d -> d).max().orElseThrow());
    if (rawRange.extent()==0) {
      throw new IllegalArgumentException("Extent of dissimilarity range on cases is 0");
    }
  }

  @Override
  public Double apply(List<Object> os1, List<Object> os2) {
    return rawRange.normalize(rawDistance.apply(os1, os2));
  }
}
