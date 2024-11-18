/*-
 * ========================LICENSE_START=================================
 * jgea-experimenter
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
package io.github.ericmedvet.jgea.experimenter.listener.plot;

import io.github.ericmedvet.jgea.core.listener.Accumulator;
import io.github.ericmedvet.jnb.datastructure.FormattedFunction;
import io.github.ericmedvet.jnb.datastructure.HashMapTable;
import io.github.ericmedvet.jnb.datastructure.NamedFunction;
import io.github.ericmedvet.jnb.datastructure.Table;
import io.github.ericmedvet.jviz.core.plot.XYPlot;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

public abstract class AbstractSingleEPAF<E, P extends XYPlot<D>, R, D, X> implements PlotAccumulatorFactory<E, P, R, D> {

  protected final Function<? super R, String> titleFunction;
  protected final Function<? super E, X> predicateValueFunction;
  private final Predicate<? super X> predicate;
  private final boolean unique;

  public AbstractSingleEPAF(
      Function<? super R, String> titleFunction,
      Function<? super E, X> predicateValueFunction,
      Predicate<? super X> predicate,
      boolean unique
  ) {
    this.titleFunction = titleFunction;
    this.predicateValueFunction = predicateValueFunction;
    this.predicate = predicate;
    this.unique = unique;
  }

  protected abstract List<Map.Entry<String, D>> buildData(E e, R r);

  protected abstract P buildPlot(Table<String, String, D> data, R r);

  @Override
  public Accumulator<E, P> build(R r) {
    Table<String, String, D> table = new HashMapTable<>();
    Set<X> predicateValues = new HashSet<>();
    return new Accumulator<>() {
      @Override
      public P get() {
        synchronized (table) {
          return buildPlot(table, r);
        }
      }

      @Override
      public void listen(E e) {
        X predicateValue = predicateValueFunction.apply(e);
        if (predicate.test(predicateValue) && !predicateValues.contains(predicateValue)) {
          if (unique) {
            predicateValues.add(predicateValue);
          }
          List<Map.Entry<String, D>> newEntries = buildData(e, r);
          synchronized (table) {
            newEntries.forEach(
                me -> table.set(
                    me.getKey(),
                    "%s = %s"
                        .formatted(
                            NamedFunction.name(predicateValueFunction),
                            FormattedFunction.format(predicateValueFunction)
                                .formatted(predicateValue)
                        ),
                    me.getValue()
                )
            );
          }
        }
      }

      @Override
      public String toString() {
        return name();
      }
    };
  }

  private String name() {
    return toString();
  }
}
