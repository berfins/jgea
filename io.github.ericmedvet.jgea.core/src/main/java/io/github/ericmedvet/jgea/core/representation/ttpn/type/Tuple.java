package io.github.ericmedvet.jgea.core.representation.ttpn.type;

import io.github.ericmedvet.jgea.core.util.Misc;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record Tuple(List<Type> types) implements Composed {
  public Tuple(List<Type> types) {
    this.types = Collections.unmodifiableList(types);
  }

  @Override
  public boolean canTakeValuesOf(Type other) {
    if (other instanceof Tuple(List<Type> otherTypes)) {
      if (otherTypes.size()!=types.size()) {
        for (int i = 0; i<types.size(); i++) {
          if (!types.get(i).canTakeValuesOf(otherTypes.get(i))) {
            return false;
          }
        }
        return true;
      }
    }
    return false;
  }

  @Override
  public Set<Generic> generics() {
    return types.stream()
        .map(Type::generics)
        .reduce(Misc::union)
        .orElse(Set.of());
  }

  @Override
  public boolean matches(Object o) {
    if (o instanceof List<?> list) {
      if (list.size() == types.size()) {
        for (int i = 0; i<types.size(); i++) {
          if (!types.get(i).matches(list.get(i))) {
            return false;
          }
        }
      }
    }
    return false;
  }

  @Override
  public Map<Generic, Type> resolveGenerics(Type concreteType) throws TypeException {
    if (concreteType instanceof Tuple(List<Type> otherTypes)) {
      if (otherTypes.size()!=types.size()) {
        throw new TypeException("Inconsistent tuple size: %d != %d".formatted(types.size(), otherTypes.size()));
      }
      List<Map<Generic, Type>> maps = new ArrayList<>(types.size());
      for (int i = 0; i<types.size(); i++) {
        maps.add(types.get(i).resolveGenerics(otherTypes.get(i)));
      }
      Map<Generic, Set<Type>> merged = Misc.merge(maps);
      Optional<Map.Entry<Generic, Set<Type>>> oneWrongEntry = merged.entrySet().stream()
          .filter(e -> e.getValue().size() > 1)
          .findAny();
      if (oneWrongEntry.isPresent()) {
        throw new TypeException("Inconsistent types for %s: %s".formatted(
            oneWrongEntry.get().getKey(),
            oneWrongEntry.get().getValue().stream().map(Object::toString).collect(Collectors.joining(", "))
        ));
      }
      return merged.entrySet().stream()
          .collect(Collectors.toMap(
              Map.Entry::getKey,
              e -> e.getValue().stream().findFirst().orElseThrow()
          ));
    }
    throw new TypeException("Wrong concrete type %s".formatted(concreteType));
  }

  @Override
  public Type concrete(Map<Generic, Type> genericTypeMap) throws TypeException {
    if (!isGenerics()) {
      return this;
    }
    List<Type> concreteTypes = new ArrayList<>(types.size());
    for (Type type : types) {
      concreteTypes.add(type.concrete(genericTypeMap));
    }
    return Composed.tuple(concreteTypes);
  }

  @Override
  public String toString() {
    return "<%s>".formatted(types.stream().map(Object::toString).collect(Collectors.joining(",")));
  }
}
