/*-
 * ========================LICENSE_START=================================
 * jgea-core
 * %%
 * Copyright (C) 2018 - 2023 Eric Medvet
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
package io.github.ericmedvet.jgea.core.util;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public interface Table<R, C, T> {

  void addColumn(C columnIndex, Map<R, T> values);

  void addRow(R rowIndex, Map<C, T> values);

  List<C> colIndexes();

  T get(R rowIndex, C columnIndex);

  void removeColumn(C columnIndex);

  void removeRow(R rowIndex);

  List<R> rowIndexes();

  void set(R rowIndex, C columnIndex, T t);

  static <R, C, T> Table<R, C, T> of(Map<R, Map<C, T>> map) {
    List<R> rowIndexes = map.keySet().stream().toList();
    List<C> colIndexes = map.values().stream()
        .map(Map::keySet)
        .flatMap(Set::stream)
        .distinct()
        .toList();
    return new Table<>() {
      @Override
      public void addColumn(C columnIndex, Map<R, T> values) {
        throw new UnsupportedOperationException("This is a read only table");
      }

      @Override
      public void addRow(R rowIndex, Map<C, T> values) {
        throw new UnsupportedOperationException("This is a read only table");
      }

      @Override
      public List<C> colIndexes() {
        return colIndexes;
      }

      @Override
      public T get(R rowIndex, C columnIndex) {
        return map.getOrDefault(rowIndex, Map.of()).get(columnIndex);
      }

      @Override
      public void removeColumn(C columnIndex) {
        throw new UnsupportedOperationException("This is a read only table");
      }

      @Override
      public void removeRow(R rowIndex) {
        throw new UnsupportedOperationException("This is a read only table");
      }

      @Override
      public List<R> rowIndexes() {
        return rowIndexes;
      }

      @Override
      public void set(R rowIndex, C columnIndex, T t) {
        throw new UnsupportedOperationException("This is a read only table");
      }
    };
  }

  default <T1, K> Table<R, C, T1> aggregate(
      Function<Map<C, T>, K> rowKey, Comparator<R> comparator, Function<List<Map<C, T>>, Map<C, T1>> aggregator) {
    Map<R, Map<C, T1>> map = rowIndexes().stream()
        .map(ri -> Map.entry(ri, row(ri)))
        .collect(Collectors.groupingBy(e -> rowKey.apply(e.getValue())))
        .values()
        .stream()
        .map(l -> {
          List<Map.Entry<R, Map<C, T>>> list = l.stream()
              .sorted((e1, e2) -> comparator.compare(e1.getKey(), e2.getKey()))
              .toList();
          R ri = list.stream().map(Map.Entry::getKey).min(comparator).orElseThrow();
          Map<C, T1> row = aggregator.apply(
              list.stream().map(Map.Entry::getValue).toList());
          return Map.entry(ri, row);
        })
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    return Table.of(map);
  }

  default <T1, K> Table<R, C, T1> aggregateByIndex(
      Function<R, K> rowKey, Comparator<R> comparator, Function<List<Map<C, T>>, Map<C, T1>> aggregator) {
    Map<R, Map<C, T1>> map = rowIndexes().stream()
        .map(ri -> Map.entry(ri, row(ri)))
        .collect(Collectors.groupingBy(e -> rowKey.apply(e.getKey())))
        .values()
        .stream()
        .map(l -> {
          List<Map.Entry<R, Map<C, T>>> list = l.stream()
              .sorted((e1, e2) -> comparator.compare(e1.getKey(), e2.getKey()))
              .toList();
          R ri = list.stream().map(Map.Entry::getKey).min(comparator).orElseThrow();
          Map<C, T1> row = aggregator.apply(
              list.stream().map(Map.Entry::getValue).toList());
          return Map.entry(ri, row);
        })
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    return Table.of(map);
  }

  default <T1, K> Table<R, C, T1> aggregateByIndexSingle(
      Function<R, K> rowKey, Comparator<R> comparator, BiFunction<C, List<T>, T1> aggregator) {
    Function<List<Map<C, T>>, Map<C, T1>> rowAggregator = maps -> maps.get(0).keySet().stream()
        .map(c -> Map.entry(
            c, aggregator.apply(c, maps.stream().map(m -> m.get(c)).toList())))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    return aggregateByIndex(rowKey, comparator, rowAggregator);
  }

  default <T1, K> Table<R, C, T1> aggregateByIndexSingle(
      Function<R, K> rowKey, Comparator<R> comparator, Function<List<T>, T1> aggregator) {
    Function<List<Map<C, T>>, Map<C, T1>> rowAggregator = maps -> maps.get(0).keySet().stream()
        .map(c -> Map.entry(
            c, aggregator.apply(maps.stream().map(m -> m.get(c)).toList())))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    return aggregateByIndex(rowKey, comparator, rowAggregator);
  }

  default <T1, K> Table<R, C, T1> aggregateSingle(
      Function<Map<C, T>, K> rowKey, Comparator<R> comparator, Function<List<T>, T1> aggregator) {
    Function<List<Map<C, T>>, Map<C, T1>> rowAggregator = maps -> maps.get(0).keySet().stream()
        .map(c -> Map.entry(
            c, aggregator.apply(maps.stream().map(m -> m.get(c)).toList())))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    return aggregate(rowKey, comparator, rowAggregator);
  }

  default <T1, K> Table<R, C, T1> aggregateSingle(
      Function<Map<C, T>, K> rowKey, Comparator<R> comparator, BiFunction<C, List<T>, T1> aggregator) {
    Function<List<Map<C, T>>, Map<C, T1>> rowAggregator = maps -> maps.get(0).keySet().stream()
        .map(c -> Map.entry(
            c, aggregator.apply(c, maps.stream().map(m -> m.get(c)).toList())))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    return aggregate(rowKey, comparator, rowAggregator);
  }

  default <R1> Table<R1, C, T> remapRowIndex(Function<R, R1> f) {
    return of(rowIndexes().stream()
        .map(ri -> Map.entry(f.apply(ri), row(ri)))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
  }

  default void clear() {
    rowIndexes().forEach(this::removeRow);
  }

  default Table<R, C, T> colSlide(int n, Function<List<T>, T> aggregator) {
    Table<R, C, T> table = new HashMapTable<>();
    rowIndexes().forEach(ri -> IntStream.range(n, colIndexes().size()).forEach(i -> {
      List<T> ts = IntStream.range(i - n, i)
          .mapToObj(j -> get(ri, colIndexes().get(j)))
          .toList();
      table.set(ri, colIndexes().get(i - 1), aggregator.apply(ts));
    }));
    return table;
  }

  default Map<R, T> column(C columnIndex) {
    return rowIndexes().stream().collect(Collectors.toMap(ri -> ri, ri -> get(ri, columnIndex)));
  }

  default List<T> columnValues(C columnIndex) {
    Map<R, T> column = column(columnIndex);
    return rowIndexes().stream().map(column::get).toList();
  }

  default <C1, T1> Table<R, C1, T1> expandColumn(C columnIndex, Function<T, Map<C1, T1>> f) {
    return of(rowIndexes().stream()
        .map(ri -> Map.entry(ri, f.apply(get(ri, columnIndex))))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
  }

  default Table<R, C, T> expandRowIndex(Function<R, Map<C, T>> f) {
    return of(rowIndexes().stream()
        .map(ri -> Map.entry(
            ri,
            Stream.of(f.apply(ri).entrySet(), row(ri).entrySet())
                .flatMap(Set::stream)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
  }

  default Table<R, C, T> with(C newColumnIndex, T newT) {
    List<C> newColIndexes = Stream.of(colIndexes(), List.of(newColumnIndex))
        .flatMap(List::stream)
        .toList();
    Table<R, C, T> thisTable = this;
    return new Table<R, C, T>() {
      @Override
      public void addColumn(C columnIndex, Map<R, T> values) {
        throw new UnsupportedOperationException("This is a read only table");
      }

      @Override
      public void addRow(R rowIndex, Map<C, T> values) {
        throw new UnsupportedOperationException("This is a read only table");
      }

      @Override
      public List<C> colIndexes() {
        return newColIndexes;
      }

      @Override
      public T get(R rowIndex, C columnIndex) {
        if (columnIndex.equals(newColumnIndex)) {
          return newT;
        }
        return thisTable.get(rowIndex, columnIndex);
      }

      @Override
      public void removeColumn(C columnIndex) {
        throw new UnsupportedOperationException("This is a read only table");
      }

      @Override
      public void removeRow(R rowIndex) {
        throw new UnsupportedOperationException("This is a read only table");
      }

      @Override
      public List<R> rowIndexes() {
        return thisTable.rowIndexes();
      }

      @Override
      public void set(R rowIndex, C columnIndex, T t) {
        throw new UnsupportedOperationException("This is a read only table");
      }
    };
  }

  default T get(int x, int y) {
    R ri = rowIndexes().get(y);
    C ci = colIndexes().get(x);
    if (ri == null || ci == null) {
      throw new IndexOutOfBoundsException(String.format(
          "Invalid %d,%d coords in a %d,%d table",
          x, y, colIndexes().size(), rowIndexes().size()));
    }
    return get(ri, ci);
  }

  default int nColumns() {
    return colIndexes().size();
  }

  default int nRows() {
    return rowIndexes().size();
  }

  default String prettyPrint(Function<R, String> rFormat, Function<C, String> cFormat, Function<T, String> tFormat) {
    if (nColumns() == 0) {
      return "";
    }
    String colSep = " ";
    Map<C, Integer> widths = colIndexes().stream()
        .collect(Collectors.toMap(
            ci -> ci,
            ci -> Math.max(
                cFormat.apply(ci).length(),
                rowIndexes().stream()
                    .mapToInt(
                        ri -> tFormat.apply(get(ri, ci)).length())
                    .max()
                    .orElse(0))));
    int riWidth = rowIndexes().stream()
        .mapToInt(ri -> rFormat.apply(ri).length())
        .max()
        .orElse(0);
    StringBuilder sb = new StringBuilder();
    // print header
    sb.append(StringUtils.justify("", riWidth));
    sb.append(riWidth > 0 ? colSep : "");
    sb.append(colIndexes().stream()
        .map(ci -> StringUtils.justify(cFormat.apply(ci), widths.get(ci)))
        .collect(Collectors.joining(colSep)));
    if (nRows() == 0) {
      return sb.toString();
    }
    sb.append("\n");
    // print rows
    sb.append(rowIndexes().stream()
        .map(ri -> {
          String s = StringUtils.justify(rFormat.apply(ri), riWidth);
          s = s + (riWidth > 0 ? colSep : "");
          s = s
              + colIndexes().stream()
                  .map(ci -> StringUtils.justify(tFormat.apply(get(ri, ci)), widths.get(ci)))
                  .collect(Collectors.joining(colSep));
          return s;
        })
        .collect(Collectors.joining("\n")));
    return sb.toString();
  }

  default String prettyPrint() {
    return prettyPrint("%s"::formatted, "%s"::formatted, "%s"::formatted);
  }

  default Map<C, T> row(R rowIndex) {
    return colIndexes().stream().collect(Collectors.toMap(ci -> ci, ci -> get(rowIndex, ci)));
  }

  default <T1> Table<R, C, T1> rowSlide(int n, Function<List<T>, T1> aggregator) {
    Table<R, C, T1> table = new HashMapTable<>();
    colIndexes().forEach(ci -> IntStream.range(n, rowIndexes().size()).forEach(i -> {
      List<T> ts = IntStream.range(i - n, i)
          .mapToObj(j -> get(rowIndexes().get(j), ci))
          .toList();
      table.set(rowIndexes().get(i - 1), ci, aggregator.apply(ts));
    }));
    return table;
  }

  default List<T> rowValues(R rowIndex) {
    Map<C, T> row = row(rowIndex);
    return colIndexes().stream().map(row::get).toList();
  }

  default void set(int x, int y, T t) {
    R ri = rowIndexes().get(y);
    C ci = colIndexes().get(x);
    if (ri == null || ci == null) {
      throw new IndexOutOfBoundsException(String.format(
          "Invalid %d,%d coords in a %d,%d table",
          x, y, colIndexes().size(), rowIndexes().size()));
    }
    set(ri, ci, t);
  }

  default Table<R, C, T> sorted(C c, Comparator<T> comparator) {
    Table<R, C, T> thisTable = this;
    return new Table<>() {
      @Override
      public void addColumn(C columnIndex, Map<R, T> values) {
        thisTable.addColumn(columnIndex, values);
      }

      @Override
      public void addRow(R rowIndex, Map<C, T> values) {
        thisTable.addRow(rowIndex, values);
      }

      @Override
      public List<C> colIndexes() {
        return thisTable.colIndexes();
      }

      @Override
      public T get(R rowIndex, C columnIndex) {
        return thisTable.get(rowIndex, columnIndex);
      }

      @Override
      public void removeColumn(C columnIndex) {
        thisTable.removeColumn(columnIndex);
      }

      @Override
      public void removeRow(R rowIndex) {
        thisTable.removeRow(rowIndex);
      }

      @Override
      public List<R> rowIndexes() {
        return thisTable.rowIndexes().stream()
            .sorted((ri1, ri2) -> comparator.compare(get(ri1, c), get(ri2, c)))
            .toList();
      }

      @Override
      public void set(R rowIndex, C columnIndex, T t) {
        thisTable.set(rowIndex, columnIndex, t);
      }
    };
  }

  default Table<R, C, T> sorted(Comparator<R> comparator) {
    Table<R, C, T> thisTable = this;
    return new Table<>() {
      @Override
      public void addColumn(C columnIndex, Map<R, T> values) {
        thisTable.addColumn(columnIndex, values);
      }

      @Override
      public void addRow(R rowIndex, Map<C, T> values) {
        thisTable.addRow(rowIndex, values);
      }

      @Override
      public List<C> colIndexes() {
        return thisTable.colIndexes();
      }

      @Override
      public T get(R rowIndex, C columnIndex) {
        return thisTable.get(rowIndex, columnIndex);
      }

      @Override
      public void removeColumn(C columnIndex) {
        thisTable.removeColumn(columnIndex);
      }

      @Override
      public void removeRow(R rowIndex) {
        thisTable.removeRow(rowIndex);
      }

      @Override
      public List<R> rowIndexes() {
        return thisTable.rowIndexes().stream().sorted(comparator).toList();
      }

      @Override
      public void set(R rowIndex, C columnIndex, T t) {
        thisTable.set(rowIndex, columnIndex, t);
      }
    };
  }

  static <R, C, T> Table<R, C, ? extends T> colLeftJoin(Table<R, C, ? extends T> t1, Table<R, C, ? extends T> t2) {
    return of(t1.rowIndexes().stream()
        .map(ri -> Map.entry(
            ri,
            Stream.of(t1.row(ri).entrySet(), t2.row(ri).entrySet())
                .flatMap(Set::stream)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
  }
}
