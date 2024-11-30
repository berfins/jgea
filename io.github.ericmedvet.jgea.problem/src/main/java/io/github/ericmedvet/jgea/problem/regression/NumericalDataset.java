package io.github.ericmedvet.jgea.problem.regression;

import io.github.ericmedvet.jgea.core.fitness.ExampleBasedFitness;
import io.github.ericmedvet.jgea.core.util.IndexedProvider;
import io.github.ericmedvet.jgea.core.util.Misc;
import io.github.ericmedvet.jnb.datastructure.DoubleRange;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.logging.Logger;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public interface NumericalDataset extends IndexedProvider<ExampleBasedFitness.Example<Map<String, Double>, Map<String
    , Double>>> {

  enum Scaling {
    NONE, MIN_MAX, SYMMETRIC_MIN_MAX, STANDARDIZATION
  }

  record VariableInfo(DoubleRange range, double mean, double sd, double q1, double q2, double q3) {
    static VariableInfo of(List<Double> values) {
      double mean = values.stream().mapToDouble(v -> v).average().orElseThrow();
      double sd = Math.sqrt(
          values.stream()
              .mapToDouble(v -> (v - mean) * (v - mean))
              .average()
              .orElseThrow()
      );
      return new VariableInfo(
          new DoubleRange(
              values.stream().min(Double::compareTo).orElseThrow(),
              values.stream().max(Double::compareTo).orElseThrow()
          ),
          mean,
          sd,
          Misc.percentile(values, Double::compareTo, 0.25),
          Misc.percentile(values, Double::compareTo, 0.50),
          Misc.percentile(values, Double::compareTo, 0.75)
      );
    }
  }

  IndexedProvider<double[]> dataPointProvider();

  List<String> xVarNames();

  List<String> yVarNames();

  static NumericalDataset from(
      List<String> xVarNames,
      List<String> yVarNames,
      IndexedProvider<double[]> dataPointProvider
  ) {
    record HardNumericalDataset(
        List<String> xVarNames,
        List<String> yVarNames,
        IndexedProvider<double[]> dataPointProvider
    ) implements NumericalDataset {}
    return new HardNumericalDataset(xVarNames, yVarNames, dataPointProvider);
  }

  static NumericalDataset loadFromCSV(
      String xVarNamePattern,
      String yVarNamePattern,
      InputStream inputStream,
      long limit
  ) throws IOException {
    Logger logger = Logger.getLogger(NumericalDataset.class.getName());
    try (inputStream) {
      CSVParser parser = CSVFormat.Builder.create().setDelimiter(";").build().parse(new InputStreamReader(inputStream));
      List<CSVRecord> records = parser.getRecords();
      List<String> varNames = records.getFirst().stream().toList();
      List<Integer> xIndexes = IntStream.range(0, varNames.size())
          .filter(i -> varNames.get(i).matches(xVarNamePattern))
          .boxed()
          .toList();
      List<Integer> yIndexes = IntStream.range(0, varNames.size())
          .filter(i -> varNames.get(i).matches(yVarNamePattern))
          .boxed()
          .toList();
      List<double[]> rows = new ArrayList<>();
      int lc = 0;
      for (CSVRecord record : records) {
        if (lc >= limit) {
          break;
        }
        if (lc != 0) {
          if (record.size() != varNames.size()) {
            logger.warning(
                "Line %d/%d has %d items instead of expected %d: skipping it"
                    .formatted(lc, records.size(), record.size(), varNames.size())
            );
          } else {
            double[] row = Stream.concat(xIndexes.stream(), yIndexes.stream()).mapToDouble(i -> Double.parseDouble(
                record.get(i))).toArray();
            rows.add(row);
          }
        }
        lc = lc + 1;
      }
      return from(
          varNames.stream().filter(n -> n.matches(xVarNamePattern)).toList(),
          varNames.stream().filter(n -> n.matches(yVarNamePattern)).toList(),
          IndexedProvider.from(rows)
      );
    }
  }

  @Override
  default ExampleBasedFitness.Example<Map<String, Double>, Map<String, Double>> get(int i) {
    return new ExampleBasedFitness.Example<>(
        IntStream.range(0, xVarNames().size()).boxed().collect(Collectors.toMap(
            j -> xVarNames().get(j),
            j -> dataPointProvider().get(i)[j]
        )),
        IntStream.range(0, yVarNames().size()).boxed().collect(Collectors.toMap(
            j -> yVarNames().get(j),
            j -> dataPointProvider().get(i)[xVarNames().size() + j]
        ))
    );
  }

  @Override
  default List<Integer> indexes() {
    return dataPointProvider().indexes();
  }

  @Override
  default NumericalDataset fold(int j, int n) {
    return from(xVarNames(), yVarNames(), dataPointProvider().fold(j, n));
  }

  @Override
  default IndexedProvider<ExampleBasedFitness.Example<Map<String, Double>, Map<String, Double>>> negatedFold(
      int j,
      int n
  ) {
    return from(xVarNames(), yVarNames(), dataPointProvider().negatedFold(j, n));
  }

  @Override
  default NumericalDataset shuffled(
      RandomGenerator rnd
  ) {
    return from(xVarNames(), yVarNames(), dataPointProvider().shuffled(rnd));
  }

  default String summary() {
    StringBuilder sb = new StringBuilder();
    sb.append(
        "n=%d p=%d (p.x=%d p.y=%d)%n"
            .formatted(
                size(),
                xVarNames().size() + yVarNames().size(),
                xVarNames().size(),
                yVarNames().size()
            )
    );
    sb.append("x vars:\n");
    xVarNames().forEach(n -> {
      VariableInfo vi = VariableInfo.of(xValues(n).all());
      sb.append(
          "\t%s:\tmin=%.3f\tmax=%.3f\tmean=%.3f\tsd=%.3f\tq1=%.3f\tq2=%.3f\tq3=%.3f%n"
              .formatted(n, vi.range.min(), vi.range.max(), vi.mean, vi.sd, vi.q1, vi.q2, vi.q3)
      );
    });
    sb.append("y vars:\n");
    yVarNames().forEach(n -> {
      VariableInfo vi = VariableInfo.of(yValues(n).all());
      sb.append(
          "\t%s:\tmin=%.3f\tmax=%.3f\tmean=%.3f\tsd=%.3f\tq1=%.3f\tq2=%.3f\tq3=%.3f%n"
              .formatted(n, vi.range.min(), vi.range.max(), vi.mean, vi.sd, vi.q1, vi.q2, vi.q3)
      );
    });
    return sb.toString();
  }

  default NumericalDataset xScaled(NumericalDataset.Scaling scaling) {
    if (scaling.equals(Scaling.NONE)) {
      return this;
    }
    List<VariableInfo> varInfos = xVarNames().stream().map(n -> VariableInfo.of(xValues(n).all())).toList();
    return from(
        xVarNames(),
        yVarNames(),
        dataPointProvider().then(
            vs -> IntStream.range(0, vs.length).mapToDouble(j -> {
                  if (j > xVarNames().size()) {
                    return vs[j];
                  }
                  return switch (scaling) {
                    case MIN_MAX -> varInfos.get(j).range.normalize(vs[j]);
                    case SYMMETRIC_MIN_MAX ->
                        DoubleRange.SYMMETRIC_UNIT.denormalize(varInfos.get(j).range.normalize(vs[j]));
                    case STANDARDIZATION -> (vs[j] - varInfos.get(j).mean) / varInfos.get(j).sd;
                    default -> vs[j];
                  };
                })
                .toArray())
    );
  }

  default IndexedProvider<Double> xValues(String xName) {
    int xIndex = xVarNames().indexOf(xName);
    return dataPointProvider().then(vs -> vs[xIndex]);
  }

  default NumericalDataset yScaled(NumericalDataset.Scaling scaling) {
    if (scaling.equals(Scaling.NONE)) {
      return this;
    }
    List<VariableInfo> varInfos = yVarNames().stream().map(n -> VariableInfo.of(yValues(n).all())).toList();
    return from(
        xVarNames(),
        yVarNames(),
        dataPointProvider().then(
            vs -> IntStream.range(0, vs.length).mapToDouble(j -> {
                  if (j < xVarNames().size()) {
                    return vs[j];
                  }
                  j = j - xVarNames().size();
                  return switch (scaling) {
                    case MIN_MAX -> varInfos.get(j).range.normalize(vs[j]);
                    case SYMMETRIC_MIN_MAX ->
                        DoubleRange.SYMMETRIC_UNIT.denormalize(varInfos.get(j).range.normalize(vs[j]));
                    case STANDARDIZATION -> (vs[j] - varInfos.get(j).mean) / varInfos.get(j).sd;
                    default -> vs[j];
                  };
                })
                .toArray())
    );
  }

  default IndexedProvider<Double> yValues(String yName) {
    int yIndex = yVarNames().indexOf(yName);
    int index = yIndex + xVarNames().size();
    return dataPointProvider().then(vs -> vs[index]);
  }

}
