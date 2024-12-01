package io.github.ericmedvet.jgea.experimenter.builders;

import io.github.ericmedvet.jgea.core.util.IndexedProvider;
import io.github.ericmedvet.jgea.problem.regression.NumericalDataset;
import io.github.ericmedvet.jnb.core.Cacheable;
import io.github.ericmedvet.jnb.core.Discoverable;
import io.github.ericmedvet.jnb.core.Param;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Discoverable(prefixTemplate = "ea.provider.numerical|num")
public class NumericalIndexedProviders {
  private NumericalIndexedProviders() {
  }

  @Cacheable
  public static NumericalDataset empty(
      @Param("xVars") List<String> xVarNames,
      @Param("yVars") List<String> yVarNames
  ) {
    return NumericalDataset.from(xVarNames, yVarNames, IndexedProvider.from(List.of()));
  }

  @Cacheable
  public static NumericalDataset fromFile(
      @Param("filePath") String filePath,
      @Param(value = "xVarNamePattern", dS = "x.*") String xVarNamePattern,
      @Param(value = "yVarNamePattern", dS = "y.*") String yVarNamePattern,
      @Param(value = "limit", dI = Integer.MAX_VALUE) int limit
  ) {
    try (InputStream is = new FileInputStream(filePath)) {
      return NumericalDataset.fromCSV(xVarNamePattern, yVarNamePattern, is, limit);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Cacheable
  public static NumericalDataset fromBundled(
      @Param("name") String name,
      @Param(value = "limit", dI = Integer.MAX_VALUE) int limit
  ) {
    try (InputStream is = NumericalDataset.class.getResourceAsStream("/datasets/regression/%s.csv".formatted(name))) {
      return switch (name) {
        case "concrete" -> NumericalDataset.fromCSV("*.", "strength", is, limit);
        case "wine" -> NumericalDataset.fromCSV("*.", "quality", is, limit);
        case "energy-efficiency" -> NumericalDataset.fromCSV("x[0-9]+", "y1", is, limit);
        case "xor" -> NumericalDataset.fromCSV("*.", "y", is, limit);
        default -> throw new IllegalArgumentException("Unknown bundled dataset: %s".formatted(name));
      };
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Cacheable
  public static NumericalDataset scaled(
      @Param("of") NumericalDataset dataset,
      @Param(value = "xScaling", dS = "none") NumericalDataset.Scaling xScaling,
      @Param(value = "yScaling", dS = "none") NumericalDataset.Scaling yScaling
  ) {
    return dataset.xScaled(xScaling).yScaled(yScaling);
  }
}
