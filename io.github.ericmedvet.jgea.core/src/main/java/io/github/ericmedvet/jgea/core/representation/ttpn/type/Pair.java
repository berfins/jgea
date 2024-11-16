package io.github.ericmedvet.jgea.core.representation.ttpn.type;

import io.github.ericmedvet.jgea.core.util.Misc;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record Pair(Type firstType, Type secondType) implements Composed {
  @Override
  public boolean canTakeValuesOf(Type other) {
    if (other instanceof Pair(
        Type otherFirstType, Type otherSecondType
    )) {
      return firstType.canTakeValuesOf(otherFirstType) && secondType.canTakeValuesOf(otherSecondType);
    }
    return false;
  }

  @Override
  public boolean matches(Object o) {
    if (o instanceof List<?> list) {
      if (list.size() == 2) {
        if (firstType.matches(list.getFirst())) {
          return secondType.matches(list.getLast());
        }
      }
    }
    return false;
  }

  @Override
  public Set<Generic> generics() {
    return Misc.union(firstType.generics(), secondType.generics());
  }

  @Override
  public String toString() {
    return "<%s,%s>".formatted(firstType, secondType);
  }

  @Override
  public Map<Generic, Type> resolveGenerics(Type concreteType) throws TypeException {
    if (concreteType instanceof Pair(Type otherFirstType, Type otherSecondType)) {
      Map<Generic, Type> first = firstType.resolveGenerics(otherFirstType);
      Map<Generic, Type> second = secondType.resolveGenerics(otherSecondType);
      Stream.concat(first.entrySet().stream(), second.entrySet().stream())
          .collect(Collectors.groupingBy(Map.Entry::getKey));
      // TODO
      // obtain a Map<Generic, Set<Type>>
      // throw if any Set<Type> is >1 size
      // return map by taking first of every set




    }
    throw new TypeException("Wrong concrete type %s".formatted(concreteType));
  }
}
