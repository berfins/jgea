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

import java.util.List;
import java.util.Set;

public record Network(
    List<Gate> gates,
    Set<Wire> wires
) {

  public record Wire(int srcGateIndex, int srcGateOutPortIndex, int dstGateIndex, int dstGateInPortIndex) {
    public boolean validate(List<Gate> gates) {
      return validateGateIndexes(gates) && validatePortIndexes(
          gates.get(srcGateIndex),
          gates.get(dstGateIndex)
      ) && validateType(gates.get(srcGateIndex), gates.get(dstGateIndex));
    }

    public boolean validateGateIndexes(List<Gate> gates) {
      return gates.size() > srcGateIndex && gates.size() > dstGateIndex;
    }

    public boolean validatePortIndexes(Gate srcGate, Gate dstGate) {
      return srcGate.outputTypes().size() > srcGateOutPortIndex && dstGate.inputPorts().size() < dstGateInPortIndex;
    }

    public boolean validateType(Gate srcGate, Gate dstGate) {
      return srcGate.outputTypes().get(srcGateOutPortIndex).equals(dstGate.inputPorts().get(dstGateInPortIndex).type());
    }
  }

  public static void main(String[] args) {
    Network n = new Network(
        List.of(
            new Gate.InputGate(Type.Composed.sequence(Type.Base.REAL)),
            new Gate.InputGate(Type.Composed.sequence(Type.Base.REAL)),
            Gates.split(),
            Gates.split(),
            Gates.rPMathOperator(Element.Operator.MULTIPLICATION),
            Gates.rPMathOperator(Element.Operator.ADDITION)
        ),
        Set.of()
    );
    System.out.println(n);
  }

  public boolean validateWires() {
    return wires.stream().allMatch(w -> w.validate(gates));
  }

}
