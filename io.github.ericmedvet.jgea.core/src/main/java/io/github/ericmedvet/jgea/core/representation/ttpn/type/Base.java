package io.github.ericmedvet.jgea.core.representation.ttpn.type;

import java.util.Map;
import java.util.Set;

public enum Base implements Type {
  BOOLEAN(Boolean.class),
  INT(Integer.class),
  REAL(Double.class),
  STRING(String.class);

  private final Class<?> javaClass;

  Base(Class<?> javaClass) {
    this.javaClass = javaClass;
  }

  @Override
  public boolean canTakeValuesOf(Type other) {
    return equals(other);
  }

  @Override
  public boolean matches(Object o) {
    return javaClass.isInstance(o);
  }

  @Override
  public Map<Generic, Type> resolveGenerics(Type concreteType) {
    return Map.of();
  }

  @Override
  public Type concrete(Map<Generic, Type> genericTypeMap) {
    return this;
  }

  @Override
  public Set<Generic> generics() {
    return Set.of();
  }



}
