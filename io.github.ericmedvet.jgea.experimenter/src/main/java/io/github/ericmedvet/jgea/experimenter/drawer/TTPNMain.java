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
package io.github.ericmedvet.jgea.experimenter.drawer;

import io.github.ericmedvet.jgea.core.representation.tree.numeric.Element;
import io.github.ericmedvet.jgea.core.representation.ttpn.*;
import io.github.ericmedvet.jgea.core.representation.ttpn.type.Base;
import io.github.ericmedvet.jgea.core.representation.ttpn.type.Composed;

import java.util.List;
import java.util.Set;

public class TTPNMain {
  public static void main(String[] args) throws NetworkStructureException, RunnerException {
    Network n = new Network(
        List.of(
            Gate.input(Composed.sequence(Base.REAL)),
            Gate.input(Composed.sequence(Base.REAL)),
            Gates.splitter(),
            Gates.splitter(),
            Gates.rPMathOperator(Element.Operator.MULTIPLICATION),
            //Gates.rPMathOperator(Element.Operator.ADDITION),
            Gates.rSPSum(),
            Gate.output(Base.REAL)
        ),
        Set.of(
            Wire.of(0, 0, 2, 0),
            Wire.of(1, 0, 3, 0),
            Wire.of(2, 0, 4, 0),
            Wire.of(3, 0, 4, 1),
            Wire.of(4, 0, 5, 0),
            Wire.of(5, 0, 5, 1),
            Wire.of(5, 0, 6, 0)
        )
    );
    System.out.println(n);
    n.validate();
    Runner runner = new Runner(1000, 1000);
    Runner.Outcome outcome = runner.run(n, List.of(List.of(1d, 2d), List.of(3d, 4d)));
    System.out.println(outcome.outputs());
    TTPNDrawer drawer = new TTPNDrawer(new TTPNDrawer.Configuration());
    drawer.draw(null, n);
  }
}
