package io.github.ericmedvet.jgea.core.representation.ttpn.type;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record Sequence(Type type) implements Composed {
  @Override
  public boolean canTakeValuesOf(Type other) {
    if (other instanceof Sequence(Type otherType)) {
      return type.canTakeValuesOf(otherType);
    }
    return false;
  }

  @Override
  public Set<Generic> generics() {
    return type.generics();
  }

  @Override
  public boolean matches(Object o) {
    if (o instanceof List<?> list) {
      return list.stream().allMatch(lO -> type().matches(lO));
    }
    return false;
  }

  @Override
  public Map<Generic, Type> resolveGenerics(Type concreteType) throws TypeException {
    if (concreteType instanceof Sequence(Type otherType)) {
      return type.resolveGenerics(otherType);
    }
    throw new TypeException("Wrong concrete type %s".formatted(concreteType));
  }

  @Override
  public Type concrete(Map<Generic, Type> genericTypeMap) throws TypeException {
    if (!isGenerics()) {
      return this;
    }
    return Composed.sequence(type.concrete(genericTypeMap));
  }

  @Override
  public String toString() {
    return "[%s]".formatted(type);
  }
}
