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

import io.github.ericmedvet.jgea.core.representation.tree.numeric.Element;

import java.util.Collections;
import java.util.List;

public class Gates {

  private Gates() {
  }

  public static Gate iPMathOperator(Element.Operator operator) {
    return Gate.of(
        Collections.nCopies(operator.arity(), Gate.Port.single(Type.Base.INT)),
        List.of(Type.Base.INT),
        inputs -> List.of(List.of((int) operator.applyAsDouble(inputs.stream()
            .mapToDouble(tokens -> ((Integer) tokens.getFirst()).doubleValue())
            .toArray())))
    );
  }

  public static Gate iSMult() {
    return Gate.of(
        List.of(Gate.Port.atLeast(Type.Base.INT, 2)),
        List.of(Type.Base.INT),
        inputs -> List.of(List.of(inputs.getFirst()
            .stream()
            .mapToInt(token -> (Integer) token)
            .reduce((n1, n2) -> n1 * n2)))
    );
  }

  public static Gate iSSum() {
    return Gate.of(
        List.of(Gate.Port.atLeast(Type.Base.INT, 2)),
        List.of(Type.Base.INT),
        inputs -> List.of(List.of(inputs.getFirst().stream().mapToInt(token -> (Integer) token).sum()))
    );
  }

  public static Gate iSplit() {
    return Gate.of(
        List.of(Gate.Port.single(new Type.Composed.Sequence(Type.Base.INT))),
        List.of(Type.Base.INT),
        inputs -> inputs.getFirst().stream().map(List::of).toList()
    );
  }

  public static Gate rPMathOperator(Element.Operator operator) {
    return Gate.of(
        Collections.nCopies(operator.arity(), Gate.Port.single(Type.Base.REAL)),
        List.of(Type.Base.REAL),
        inputs -> List.of(List.of(operator.applyAsDouble(inputs.stream()
            .mapToDouble(tokens -> (Double) tokens.getFirst())
            .toArray())))
    );
  }

  public static Gate rSMult() {
    return Gate.of(
        List.of(Gate.Port.atLeast(Type.Base.REAL, 2)),
        List.of(Type.Base.REAL),
        inputs -> List.of(List.of(inputs.getFirst()
            .stream()
            .mapToDouble(token -> (Double) token)
            .reduce((n1, n2) -> n1 * n2)))
    );
  }

  public static Gate rSSum() {
    return Gate.of(
        List.of(Gate.Port.atLeast(Type.Base.REAL, 2)),
        List.of(Type.Base.REAL),
        inputs -> List.of(List.of(inputs.getFirst().stream().mapToDouble(token -> (Double) token).sum()))
    );
  }

}
