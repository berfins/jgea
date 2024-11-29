/*-
 * ========================LICENSE_START=================================
 * jgea-problem
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

package io.github.ericmedvet.jgea.problem.booleanfunction;

import io.github.ericmedvet.jgea.core.representation.tree.Tree;
import io.github.ericmedvet.jgea.core.representation.tree.booleanfunction.Element;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

public class BooleanFunctionFitnessCL extends CLListCaseBasedFitness<List<Tree<Element>>, boolean[], Boolean, Double> {

  public BooleanFunctionFitnessCL(TargetFunction targetFunction, List<boolean[]> observations) {
    super(observations, new Error(targetFunction), new ErrorRate());
  }

  public interface TargetFunction extends Function<boolean[], boolean[]> {
    String[] varNames();

    static TargetFunction from(final Function<boolean[], boolean[]> function, final String... varNames) {
      return new TargetFunction() {
        @Override
        public boolean[] apply(boolean[] values) {
          return function.apply(values);
        }

        @Override
        public String[] varNames() {
          return varNames;
        }
      };
    }
  }

  private record Error(TargetFunction targetFunction) implements BiFunction<List<Tree<Element>>, boolean[], Boolean> {

    @Override
    public Boolean apply(List<Tree<Element>> solution, boolean[] observation) {
      Map<String, Boolean> varValues = new LinkedHashMap<>();
      for (int i = 0; i < targetFunction.varNames().length; i++) {
        varValues.put(targetFunction.varNames()[i], observation[i]);
      }
      boolean[] computed = BooleanUtils.compute(solution, varValues);
      return Arrays.equals(computed, targetFunction.apply(observation));
    }
  }

  private static class ErrorRate implements Function<List<Boolean>, Double> {

    @Override
    public Double apply(List<Boolean> vs) {
      double errors = 0;
      for (Boolean v : vs) {
        errors = errors + (v ? 0d : 1d);
      }
      return errors / (double) vs.size();
    }
  }
}
