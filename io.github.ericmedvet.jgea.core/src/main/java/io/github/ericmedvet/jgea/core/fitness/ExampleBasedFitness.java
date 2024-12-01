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
package io.github.ericmedvet.jgea.core.fitness;

import io.github.ericmedvet.jgea.core.util.IndexedProvider;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public interface ExampleBasedFitness<S, EI, EO, EQ, AQ> extends CaseBasedFitness<S, ExampleBasedFitness.Example<EI, EO>, EQ, AQ> {
  record Example<I, O>(I input, O output) {}

  BiFunction<EO, EO, EQ> errorFunction();

  BiFunction<S, EI, EO> predictFunction();

  static <S, EI, EO, EQ, AQ> ExampleBasedFitness<S, EI, EO, EQ, AQ> from(
      Function<List<EQ>, AQ> aggregateFunction,
      BiFunction<S, EI, EO> predictFunction,
      BiFunction<EO, EO, EQ> errorFunction,
      IndexedProvider<Example<EI, EO>> caseProvider
  ) {
    record HardExampleBasedFitness<S, EI, EO, EQ, AQ>(
        Function<List<EQ>, AQ> aggregateFunction,
        BiFunction<S, EI, EO> predictFunction,
        BiFunction<EO, EO, EQ> errorFunction,
        IndexedProvider<Example<EI, EO>> caseProvider
    ) implements ExampleBasedFitness<S, EI, EO, EQ, AQ> {}
    return new HardExampleBasedFitness<>(aggregateFunction, predictFunction, errorFunction, caseProvider);
  }

  static <S, EI, EO, EQ, AQ> ExampleBasedFitness<S, EI, EO, EQ, AQ> from(
      Function<List<EQ>, AQ> aggregateFunction,
      BiFunction<S, EI, EO> predictFunction,
      BiFunction<EO, EO, EQ> errorFunction,
      IndexedProvider<EI> inputs,
      Function<EI, EO> oracle
  ) {
    return from(
        aggregateFunction,
        predictFunction,
        errorFunction,
        inputs.then(ei -> new Example<>(ei, oracle.apply(ei)))
    );
  }

  static <S, EI, EO, EQ, AQ> ExampleBasedFitness<S, EI, EO, EQ, AQ> from(
      Function<List<EQ>, AQ> aggregateFunction,
      BiFunction<S, EI, EO> predictFunction,
      BiFunction<EO, EO, EQ> errorFunction,
      IndexedProvider<EI> inputs,
      S target
  ) {
    Function<EI, EO> oracle = ei -> predictFunction.apply(target, ei);
    return from(aggregateFunction, predictFunction, errorFunction, inputs, oracle);
  }

  @Override
  default BiFunction<S, Example<EI, EO>, EQ> caseFunction() {
    return (s, e) -> errorFunction().apply(e.output, predictFunction().apply(s, e.input));
  }

}
