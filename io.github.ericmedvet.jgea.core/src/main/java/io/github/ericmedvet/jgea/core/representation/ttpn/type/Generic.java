package io.github.ericmedvet.jgea.core.representation.ttpn.type;

import java.util.Map;
import java.util.Set;

public record Generic(String name) implements Type {
  public static Generic of(String name) {
    return new Generic(name);
  }

  @Override
  public boolean canTakeValuesOf(Type other) {
    return true;
  }

  @Override
  public boolean matches(Object o) {
    return true;
  }

  @Override
  public Set<Generic> generics() {
    return Set.of(this);
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public Map<Generic, Type> resolveGenerics(Type concreteType) {
    return Map.of(this, concreteType);
  }
}
