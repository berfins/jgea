/*
 * Copyright 2024 eric
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.ericmedvet.jgea.core.representation.ttpn;

import java.util.List;
import java.util.function.Function;

public interface Gate {
  record InputGate(Type type) implements Gate {
    @Override
    public List<Port> inputPorts() {
      return List.of();
    }

    @Override
    public List<Type> outputTypes() {
      return List.of(type);
    }

    @Override
    public Function<List<List<Object>>, List<List<Object>>> processingFunction() {
      return inputs -> List.of(List.of(inputs.getFirst().getFirst()));
    }
  }

  record OutputGate(Type type) implements Gate {
    @Override
    public List<Port> inputPorts() {
      return List.of(new Port(type, Port.Condition.EXACTLY, 1));
    }

    @Override
    public List<Type> outputTypes() {
      return List.of();
    }

    @Override
    public Function<List<List<Object>>, List<List<Object>>> processingFunction() {
      return inputs -> List.of(List.of(inputs.getFirst().getFirst()));
    }
  }

  record Port(Type type, Condition condition, int n) {
    enum Condition {EXACTLY, AT_LEAST}

    public static Port atLeast(Type type, int n) {
      return new Port(type, Condition.AT_LEAST, n);
    }

    public static Port exactly(Type type, int n) {
      return new Port(type, Condition.EXACTLY, n);
    }

    public static Port single(Type type) {
      return new Port(type, Condition.EXACTLY, 1);
    }
  }

  List<Port> inputPorts();

  List<Type> outputTypes();

  Function<List<List<Object>>, List<List<Object>>> processingFunction();

  static Gate of(
      List<Port> inputPorts,
      List<Type> outputTypes,
      Function<List<List<Object>>, List<List<Object>>> processingFunction
  ) {
    record HardGate(
        List<Port> inputPorts,
        List<Type> outputTypes,
        Function<List<List<Object>>, List<List<Object>>> processingFunction
    ) implements Gate {}
    return new HardGate(inputPorts, outputTypes, processingFunction);
  }

}
