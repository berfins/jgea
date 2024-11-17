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
import io.github.ericmedvet.jgea.core.representation.ttpn.type.Base;
import io.github.ericmedvet.jgea.core.representation.ttpn.type.Composed;
import io.github.ericmedvet.jgea.core.representation.ttpn.type.Generic;
import io.github.ericmedvet.jnb.datastructure.NamedFunction;

import java.util.Collections;
import java.util.List;

public class Gates {

  private Gates() {
  }

  public static Gate iPMathOperator(Element.Operator operator) {
    return Gate.of(
        Collections.nCopies(operator.arity(), Gate.Port.single(Base.INT)),
        List.of(Base.INT),
        NamedFunction.from(
            inputs -> List.of(List.of((int) operator.applyAsDouble(inputs.stream()
                .mapToDouble(tokens -> ((Integer) tokens.getFirst()).doubleValue())
                .toArray()))),
            "%s".formatted(operator.toString())
        )
    );
  }

  public static Gate iSMult() {
    return Gate.of(
        List.of(Gate.Port.atLeast(Base.INT, 2)),
        List.of(Base.INT),
        NamedFunction.from(
            inputs -> List.of(List.of(inputs.getFirst().stream()
                .mapToInt(token -> (Integer) token)
                .reduce((n1, n2) -> n1 * n2))),
            "s*"
        )
    );
  }

  public static Gate iSPMult() {
    return Gate.of(
        List.of(
            Gate.Port.atLeast(Base.INT, 1),
            Gate.Port.atLeast(Base.INT, 0)
        ),
        List.of(Base.INT),
        NamedFunction.from(
            inputs -> List.of(List.of(
                inputs.getFirst().stream()
                    .mapToInt(token -> (Integer) token)
                    .reduce((i1, i2) -> i1 * i2).orElse(1) +
                    inputs.getLast().stream()
                        .mapToInt(token -> (Integer) token)
                        .reduce((n1, n2) -> n1 * n2).orElse(1)
            )),
            "sp*"
        )
    );
  }

  public static Gate iSPSum() {
    return Gate.of(
        List.of(
            Gate.Port.atLeast(Base.INT, 1),
            Gate.Port.atLeast(Base.INT, 0)
        ),
        List.of(Base.INT),
        NamedFunction.from(
            inputs -> List.of(List.of(
                inputs.getFirst().stream()
                    .mapToInt(token -> (Integer) token)
                    .sum() +
                    inputs.getLast().stream()
                        .mapToInt(token -> (Integer) token)
                        .sum()
            )),
            "sp+"
        )
    );
  }

  public static Gate iSSum() {
    return Gate.of(
        List.of(Gate.Port.atLeast(Base.INT, 2)),
        List.of(Base.INT),
        NamedFunction.from(
            inputs -> List.of(List.of(inputs.getFirst().stream()
                .mapToInt(token -> (Integer) token)
                .sum())),
            "s+"
        )
    );
  }

  public static Gate noop() {
    return Gate.of(
        List.of(Gate.Port.single(Generic.of("t"))),
        List.of(Generic.of("t")),
        NamedFunction.from(inputs -> inputs, "noop")
    );
  }

  public static Gate pairer() {
    return Gate.of(
        List.of(
            Gate.Port.single(Generic.of("f")),
            Gate.Port.single(Generic.of("s"))
        ),
        List.of(Composed.tuple(List.of(
            Generic.of("f"),
            Generic.of("s")
        ))),
        NamedFunction.from(
            inputs -> List.of(List.of(List.of(
                inputs.getFirst().getFirst(),
                inputs.getLast().getFirst()
            ))),
            "pairer"
        )
    );
  }

  public static Gate rPMathOperator(Element.Operator operator) {
    return Gate.of(
        Collections.nCopies(operator.arity(), Gate.Port.single(Base.REAL)),
        List.of(Base.REAL),
        NamedFunction.from(
            inputs -> List.of(List.of(operator.applyAsDouble(inputs.stream()
                .mapToDouble(tokens -> (Double) tokens.getFirst())
                .toArray()))),
            "%s".formatted(operator.toString())
        )
    );
  }

  public static Gate rSMult() {
    return Gate.of(
        List.of(Gate.Port.atLeast(Base.REAL, 2)),
        List.of(Base.REAL),
        NamedFunction.from(
            inputs -> List.of(List.of(inputs.getFirst().stream()
                .mapToDouble(token -> (Double) token)
                .reduce((n1, n2) -> n1 * n2))),
            "s*"
        )
    );
  }

  public static Gate rSPMult() {
    return Gate.of(
        List.of(
            Gate.Port.atLeast(Base.REAL, 1),
            Gate.Port.atLeast(Base.REAL, 0)
        ),
        List.of(Base.REAL),
        NamedFunction.from(
            inputs -> List.of(List.of(
                inputs.getFirst().stream()
                    .mapToDouble(token -> (Double) token)
                    .reduce((n1, n2) -> n1 * n2).orElse(1) +
                    inputs.getLast().stream()
                        .mapToDouble(token -> (Double) token)
                        .reduce((n1, n2) -> n1 * n2).orElse(1)
            )),
            "sp*"
        )
    );
  }

  public static Gate rSPSum() {
    return Gate.of(
        List.of(
            Gate.Port.atLeast(Base.REAL, 1),
            Gate.Port.atLeast(Base.REAL, 0)
        ),
        List.of(Base.REAL),
        NamedFunction.from(
            inputs -> List.of(List.of(
                inputs.getFirst().stream()
                    .mapToDouble(token -> (Double) token)
                    .sum() +
                    inputs.getLast().stream()
                        .mapToDouble(token -> (Double) token)
                        .sum()
            )),
            "sp+"
        )
    );
  }

  public static Gate rSSum() {
    return Gate.of(
        List.of(Gate.Port.atLeast(Base.REAL, 2)),
        List.of(Base.REAL),
        NamedFunction.from(
            inputs -> List.of(List.of(inputs.getFirst().stream()
                .mapToDouble(token -> (Double) token)
                .sum())),
            "s+"
        )
    );
  }

  public static Gate splitter() {
    return Gate.of(
        List.of(Gate.Port.single(Composed.sequence(Generic.of("t")))),
        List.of(Generic.of("t")),
        NamedFunction.from(
            inputs -> inputs.getFirst().stream().map(List::of).toList(), "splitter")
    );
  }

  public static Gate unpairer() {
    //noinspection unchecked
    return Gate.of(
        List.of(Gate.Port.single(Composed.tuple(List.of(
            Generic.of("f"),
            Generic.of("s")
        )))),
        List.of(
            Generic.of("f"),
            Generic.of("s")
        ),
        NamedFunction.from(
            inputs -> List.of(
                List.of(((List<Object>) inputs.getFirst().getFirst()).getFirst()),
                List.of(((List<Object>) inputs.getFirst().getFirst()).get(1))
            ),
            "unpairer"
        )
    );
  }
}
