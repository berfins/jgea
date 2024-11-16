package io.github.ericmedvet.jgea.core.representation.ttpn.type;

import java.util.List;

public interface Composed extends Type {

  static Tuple tuple(List<Type> types) {
    return new Tuple(types);
  }

  static Sequence sequence(Type type) {
    return new Sequence(type);
  }
}
