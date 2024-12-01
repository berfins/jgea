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

public interface CaseBasedFitness<S, C, CO, Q> extends Function<S, Q> {

  Function<List<CO>, Q> aggregateFunction();

  BiFunction<S, C, CO> caseFunction();

  IndexedProvider<C> caseProvider();

  static <S, C, CO, Q> CaseBasedFitness<S, C, CO, Q> from(
      Function<List<CO>, Q> aggregateFunction,
      BiFunction<S, C, CO> caseFunction,
      IndexedProvider<C> caseProvider
  ) {
    record HardCaseBasedFitness<S, C, CO, Q>(
        Function<List<CO>, Q> aggregateFunction,
        BiFunction<S, C, CO> caseFunction,
        IndexedProvider<C> caseProvider
    ) implements CaseBasedFitness<S, C, CO, Q> {}
    return new HardCaseBasedFitness<>(aggregateFunction, caseFunction, caseProvider);
  }

  @Override
  default Q apply(S s) {
    List<CO> outcomes = caseProvider().stream().map(c -> caseFunction().apply(s, c)).toList();
    return aggregateFunction().apply(outcomes);
  }

}
