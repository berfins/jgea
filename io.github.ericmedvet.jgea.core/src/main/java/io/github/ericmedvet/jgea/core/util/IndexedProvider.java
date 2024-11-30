package io.github.ericmedvet.jgea.core.util;

import java.util.*;
import java.util.function.Function;
import java.util.random.RandomGenerator;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public interface IndexedProvider<T> {
  T get(int i);

  List<Integer> indexes();

  static <T> IndexedProvider<T> from(List<T> ts) {
    List<Integer> indexes = Collections.synchronizedList(IntStream.range(0, ts.size()).boxed().toList());
    List<T> safeTs = Collections.synchronizedList(Collections.unmodifiableList(ts));
    return new IndexedProvider<>() {
      @Override
      public T get(int i) {
        return safeTs.get(i);
      }

      @Override
      public List<Integer> indexes() {
        return indexes;
      }
    };
  }

  default List<T> all() {
    return stream().toList();
  }

  default T first() {
    return get(indexes().getFirst());
  }

  default IndexedProvider<T> fold(int j, int n) {
    IndexedProvider<T> thisIndexedProvider = this;
    List<Integer> foldIndexes = Misc.fold(indexes(), j, n);
    return new IndexedProvider<>() {
      @Override
      public T get(int i) {
        return thisIndexedProvider.get(i);
      }

      @Override
      public List<Integer> indexes() {
        return foldIndexes;
      }
    };
  }

  default IndexedProvider<T> negatedFold(int j, int n) {
    IndexedProvider<T> thisIndexedProvider = this;
    List<Integer> foldIndexes = Misc.negatedFold(indexes(), j, n);
    return new IndexedProvider<>() {
      @Override
      public T get(int i) {
        return thisIndexedProvider.get(i);
      }

      @Override
      public List<Integer> indexes() {
        return foldIndexes;
      }
    };
  }

  default IndexedProvider<T> shuffled(RandomGenerator rnd) {
    IndexedProvider<T> thisIndexedProvider = this;
    List<Integer> shuffledIndexes = new ArrayList<>(indexes());
    Collections.shuffle(shuffledIndexes, rnd);
    List<Integer> finalShuffledIndexes = Collections.unmodifiableList(shuffledIndexes);
    return new IndexedProvider<T>() {
      @Override
      public T get(int i) {
        return thisIndexedProvider.get(i);
      }

      @Override
      public List<Integer> indexes() {
        return finalShuffledIndexes;
      }
    };
  }

  default int size() {
    return indexes().size();
  }

  default Stream<T> stream() {
    return indexes().stream().map(this::get);
  }

  default <K> IndexedProvider<K> then(Function<? super T, ? extends K> function) {
    IndexedProvider<T> thisIndexedProvider = this;
    Map<Integer, K> computed = Collections.synchronizedMap(new HashMap<>(size()));
    return new IndexedProvider<>() {
      @Override
      public K get(int i) {
        return computed.computeIfAbsent(i, j -> function.apply(thisIndexedProvider.get(j)));
      }

      @Override
      public List<Integer> indexes() {
        return thisIndexedProvider.indexes();
      }
    };
  }
}
