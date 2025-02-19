/*-
 * ========================LICENSE_START=================================
 * jgea-core
 * %%
 * Copyright (C) 2018 - 2024 Eric Medvet
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */

package io.github.ericmedvet.jgea.core.listener;

import io.github.ericmedvet.jgea.core.util.Misc;
import io.github.ericmedvet.jgea.core.util.StringUtils;
import io.github.ericmedvet.jnb.datastructure.FormattedNamedFunction;
import io.github.ericmedvet.jnb.datastructure.NamedFunction;
import io.github.ericmedvet.jnb.datastructure.Pair;
import java.io.PrintStream;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class TabularPrinter<E, K> implements ListenerFactory<E, K> {

  private static final String COLOR_RESET = "\u001B[0m";
  private static final String COLOR_DOWN = "\u001B[31m";
  private static final String COLOR_UP = "\u001B[32m";
  private static final String COLOR_SAME = "\u001B[33m";
  private static final String COLOR_NOT_RELEVANT = "\033[0;90m";
  private static final String COLOR_ERR = "\u001B[31m";

  private static final String SEP = " ";
  private static final String COMPUTE_ERROR_STRING = "cErr";
  private static final String FORMAT_ERROR_STRING = "fErr";

  private static final Logger L = Logger.getLogger(TabularPrinter.class.getName());

  private final List<Pair<? extends FormattedNamedFunction<? super E, ?>, Integer>> ePairs;
  private final List<Pair<? extends FormattedNamedFunction<? super K, ?>, Integer>> kPairs;
  private final PrintStream ps;
  private final int headerInterval;
  private final int legendInterval;
  private final boolean showLegend;
  private final boolean showVariation;
  private final boolean useColors;
  private final boolean logExceptions;

  private final String header;
  private final String legend;

  private int lineCounter = 0;

  public TabularPrinter(
      List<? extends Function<? super E, ?>> eFunctions,
      List<? extends Function<? super K, ?>> kFunctions,
      boolean logExceptions
  ) {
    this(eFunctions, kFunctions, System.out, 25, 100, true, true, true, logExceptions);
  }

  public TabularPrinter(
      List<? extends Function<? super E, ?>> eFunctions,
      List<? extends Function<? super K, ?>> kFunctions,
      PrintStream ps,
      int headerInterval,
      int legendInterval,
      boolean showLegend,
      boolean showVariation,
      boolean useColors,
      boolean logExceptions
  ) {
    ePairs = eFunctions.stream()
        .map(FormattedNamedFunction::from)
        .map(
            f -> new Pair<>(
                f,
                Math.max(StringUtils.collapse(f.name()).length(), StringUtils.formatSize(f.format()))
            )
        )
        .collect(Collectors.toList());
    kPairs = kFunctions.stream()
        .map(FormattedNamedFunction::from)
        .map(
            f -> new Pair<>(
                f,
                Math.max(StringUtils.collapse(f.name()).length(), StringUtils.formatSize(f.format()))
            )
        )
        .collect(Collectors.toList());
    this.ps = ps;
    this.headerInterval = headerInterval;
    this.legendInterval = legendInterval;
    this.showLegend = showLegend;
    this.showVariation = showVariation;
    this.useColors = useColors;
    this.logExceptions = logExceptions;
    List<String> kHeaders = kPairs.stream()
        .map(p -> StringUtils.justify(StringUtils.collapse(p.first().name()), p.second()))
        .toList();
    List<String> eHeaders = ePairs.stream()
        .map(p -> StringUtils.justify(StringUtils.collapse(p.first().name()), p.second()) + (showVariation ? " " : ""))
        .toList();
    header = String.join(SEP, Misc.concat(List.of(kHeaders, eHeaders)));
    int w = ePairs.stream()
        .mapToInt(p -> StringUtils.collapse(p.first().name()).length())
        .max()
        .orElse(1);
    legend = "Legend:\n" + Misc.concat(List.of(kPairs, ePairs))
        .stream()
        .map(
            p -> String.format(
                "%" + w + "." + w + "s → %s [%s]",
                StringUtils.collapse(p.first().name()),
                p.first().name(),
                p.first().format()
            )
        )
        .collect(Collectors.joining("\n"));
  }

  private static String format(Object currentValue, String format, int l) {
    return StringUtils.justify(String.format(format, currentValue), l);
  }

  @Override
  public Listener<E> build(K k) {
    List<?> fixedValues = kPairs.stream().map(p -> p.first().apply(k)).toList();
    final String fixedS = IntStream.range(0, kPairs.size())
        .mapToObj(
            i -> format(
                fixedValues.get(i),
                kPairs.get(i).first().format(),
                kPairs.get(i).second()
            )
        )
        .collect(Collectors.joining(SEP));
    return new Listener<>() {
      final Object[] lastValues = new Object[ePairs.size()];
      final Object[] secondLastValues = new Object[ePairs.size()];

      @Override
      public void listen(E e) {
        List<?> values = ePairs.stream()
            .map(p -> {
              try {
                return p.first().apply(e);
              } catch (Exception ex) {
                if (logExceptions) {
                  L.warning(
                      "%s raised computing exception %s"
                          .formatted(NamedFunction.name(p.first()), ex)
                  );
                }
                return ex;
              }
            })
            .toList();
        String s = IntStream.range(0, ePairs.size())
            .mapToObj(
                i -> format(
                    NamedFunction.name(ePairs.get(i).first()),
                    values.get(i),
                    lastValues[i],
                    secondLastValues[i],
                    ePairs.get(i).first().format(),
                    ePairs.get(i).second()
                )
            )
            .collect(Collectors.joining(SEP));
        IntStream.range(0, ePairs.size()).forEach(i -> {
          secondLastValues[i] = lastValues[i];
          lastValues[i] = values.get(i);
        });
        if (s.isEmpty()) {
          return;
        }
        synchronized (ps) {
          if (showLegend && legendInterval > 1 && (lineCounter % legendInterval == 0)) {
            ps.println(legend);
          }
          if (headerInterval > 1 && (lineCounter % headerInterval == 0)) {
            ps.println(header);
          }
          ps.println((fixedS.isEmpty() ? "" : (fixedS + SEP)) + s);
          lineCounter = lineCounter + 1;
        }
      }

      @Override
      public String toString() {
        return "tabular(%s)"
            .formatted(
                Stream.concat(
                    ePairs.stream().map(p -> p.first().name()),
                    kPairs.stream().map(p -> p.first().name())
                )
                    .collect(Collectors.joining(";"))
            );
      }
    };
  }

  private String format(
      String fName,
      Object currentValue,
      Object lastValue,
      Object secondLastValue,
      String format,
      int l
  ) {
    if (currentValue instanceof Exception) {
      if (useColors) {
        return COLOR_ERR + StringUtils.justify(COMPUTE_ERROR_STRING, l + (showVariation ? 1 : 0)) + COLOR_RESET;
      }
      return StringUtils.justify(COMPUTE_ERROR_STRING, l + (showVariation ? 1 : 0));
    }
    char currentVariation = showVariation ? StringUtils.variation(currentValue, lastValue) : ' ';
    char lastVariation = showVariation ? StringUtils.variation(lastValue, secondLastValue) : ' ';
    try {
      String s = StringUtils.justify(String.format(format, currentValue), l);
      if (showVariation) {
        if (useColors) {
          String color;
          if (currentVariation != lastVariation) {
            if (currentVariation == StringUtils.VARIATION_DOWN) {
              color = COLOR_DOWN;
            } else if (currentVariation == StringUtils.VARIATION_UP) {
              color = COLOR_UP;
            } else {
              color = COLOR_SAME;
            }
          } else {
            color = COLOR_NOT_RELEVANT;
          }
          return s + color + currentVariation + COLOR_RESET;
        }
        return s + currentVariation;
      }
      return s;
    } catch (Exception ex) {
      if (logExceptions) {
        L.warning("%s raised format exception %s".formatted(fName, ex));
      }
      if (useColors) {
        return COLOR_ERR + StringUtils.justify(FORMAT_ERROR_STRING, l + (showVariation ? 1 : 0)) + COLOR_RESET;
      }
      return StringUtils.justify(FORMAT_ERROR_STRING, l + (showVariation ? 1 : 0));
    }
  }
}
