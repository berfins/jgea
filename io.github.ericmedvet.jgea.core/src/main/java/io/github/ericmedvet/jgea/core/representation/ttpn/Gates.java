/*-
 * ========================LICENSE_START=================================
 * jgea-core
 * %%
 * Copyright (C) 2018 - 2024 Eric Medvet
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
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
import io.github.ericmedvet.jnb.datastructure.NamedFunction;
import java.util.Collections;
import java.util.List;

public class Gates {

  private Gates() {}

  public static Gate iPMathOperator(Element.Operator operator) {
    return Gate.of(
        Collections.nCopies(operator.arity(), Gate.Port.single(Type.Base.INT)),
        List.of(Type.Base.INT),
        NamedFunction.from(
            inputs -> List.of(List.of((int) operator.applyAsDouble(inputs.stream()
                .mapToDouble(tokens -> ((Integer) tokens.getFirst()).doubleValue())
                .toArray()))),
            "%s[i]".formatted(operator.toString())));
  }

  public static Gate iSMult() {
    return Gate.of(
        List.of(Gate.Port.atLeast(Type.Base.INT, 2)),
        List.of(Type.Base.INT),
        NamedFunction.from(
            inputs -> List.of(List.of(inputs.getFirst().stream()
                .mapToInt(token -> (Integer) token)
                .reduce((n1, n2) -> n1 * n2))),
            "s*[i]"));
  }

  public static Gate iSSum() {
    return Gate.of(
        List.of(Gate.Port.atLeast(Type.Base.INT, 2)),
        List.of(Type.Base.INT),
        NamedFunction.from(
            inputs -> List.of(List.of(inputs.getFirst().stream()
                .mapToInt(token -> (Integer) token)
                .sum())),
            "s+[i]"));
  }

  public static Gate rPMathOperator(Element.Operator operator) {
    return Gate.of(
        Collections.nCopies(operator.arity(), Gate.Port.single(Type.Base.REAL)),
        List.of(Type.Base.REAL),
        NamedFunction.from(
            inputs -> List.of(List.of(operator.applyAsDouble(inputs.stream()
                .mapToDouble(tokens -> (Double) tokens.getFirst())
                .toArray()))),
            "%s[r]".formatted(operator.toString())));
  }

  public static Gate rSMult() {
    return Gate.of(
        List.of(Gate.Port.atLeast(Type.Base.REAL, 2)),
        List.of(Type.Base.REAL),
        NamedFunction.from(
            inputs -> List.of(List.of(inputs.getFirst().stream()
                .mapToDouble(token -> (Double) token)
                .reduce((n1, n2) -> n1 * n2))),
            "s*[r]"));
  }

  public static Gate rSSum() {
    return Gate.of(
        List.of(Gate.Port.atLeast(Type.Base.REAL, 2)),
        List.of(Type.Base.REAL),
        NamedFunction.from(
            inputs -> List.of(List.of(inputs.getFirst().stream()
                .mapToDouble(token -> (Double) token)
                .sum())),
            "s+[r]"));
  }

  public static Gate split() {
    return Gate.of(
        List.of(Gate.Port.single(Type.Composed.sequence(Type.Generic.of("t")))),
        List.of(Type.Generic.of("t")),
        NamedFunction.from(
            inputs -> inputs.getFirst().stream().map(List::of).toList(), "split"));
  }
}
