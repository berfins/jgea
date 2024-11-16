package io.github.ericmedvet.jgea.core.representation.ttpn.type;

public interface Composed extends Type {

  static Pair pair(Type firstType, Type secondType) {
    return new Pair(firstType, secondType);
  }

  static Sequence sequence(Type type) {
    return new Sequence(type);
  }
}
