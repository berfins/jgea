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
import io.github.ericmedvet.jgea.core.representation.ttpn.type.Type;
import io.github.ericmedvet.jgea.core.util.Misc;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public record Network(List<Gate> gates, Set<Wire> wires) {
  public Network(List<Gate> gates, Set<Wire> wires) {
    this.gates = Collections.unmodifiableList(gates);
    this.wires = Collections.unmodifiableSet(wires);
  }

  @Override
  public String toString() {
    return IntStream.range(0, gates.size())
        .mapToObj(i -> "%3d : %s inputs:%s outputs:%s"
            .formatted(
                i,
                gates.get(i),
                IntStream.range(0, gates.get(i).inputPorts().size())
                    .mapToObj(j -> wireTo(new Wire.EndPoint(i, j))
                        .map(w -> w.src().toString())
                        .orElse("_"))
                    .collect(Collectors.joining(",")),
                IntStream.range(0, gates.get(i).outputTypes().size())
                    .mapToObj(j -> wiresFrom(new Wire.EndPoint(i, j)).stream()
                        .map(w -> w.src().toString())
                        .collect(Collectors.joining("+")))
                    .collect(Collectors.joining(","))
            ))
        .collect(Collectors.joining("\n"));
  }

  public void validate() throws NetworkStructureException {
    for (Wire wire : wires) {
      try {
        validateGateIndexes(wire);
        validatePortIndexes(wire);
      } catch (NetworkStructureException e) {
        throw new NetworkStructureException("Wrong wire %s".formatted(wire), e);
      }
    }
    for (int gateIndex = 0; gateIndex < gates.size(); gateIndex++) {
      try {
        validatePortArity(gateIndex);
        validateOutGenerics(gateIndex);
      } catch (NetworkStructureException e) {
        throw new NetworkStructureException("Wrong gate %d".formatted(gateIndex), e);
      }
    }
    for (Wire wire : wires) {
      try {
        validateType(wire);
      } catch (NetworkStructureException e) {
        throw new NetworkStructureException("Wrong wire %s".formatted(wire), e);
      }
    }
  }

  private void validatePortArity(int gateIndex) throws NetworkStructureException {
    for (int portIndex = 0; portIndex < gates.get(gateIndex).inputPorts().size(); portIndex++) {
      int finalPortIndex = portIndex;
      long count = wires.stream()
          .filter(w -> w.dst().gateIndex() == gateIndex && w.dst().portIndex() == finalPortIndex)
          .count();
      if (count > 1) {
        throw new NetworkStructureException("Multiple input wires on port %d".formatted(portIndex));
      }
    }
  }

  private void validateOutGenerics(int gateIndex) throws NetworkStructureException {
    Set<Generic> outGenerics = gates.get(gateIndex)
        .outputTypes()
        .stream()
        .map(Type::generics)
        .reduce(Misc::union)
        .orElse(Set.of());
    Set<Generic> inGenerics = gates.get(gateIndex)
        .inputPorts()
        .stream()
        .map(Gate.Port::type)
        .map(Type::generics)
        .reduce(Misc::union)
        .orElse(Set.of());
    if (!inGenerics.containsAll(outGenerics)) {
      throw new NetworkStructureException("Undefined out generics: in=%s out=%s".formatted(
          inGenerics,
          outGenerics
      ));
    }
  }

  private void validateGateIndexes(Wire wire) throws NetworkStructureException {
    if (gates.size() <= wire.src().gateIndex()) {
      throw new NetworkStructureException("Not existing src gate");
    }
    if (gates.size() <= wire.dst().gateIndex()) {
      throw new NetworkStructureException("Not existing dst gate");
    }
  }

  private void validatePortIndexes(Wire wire) throws NetworkStructureException {
    if (gates().get(wire.src().gateIndex()).outputTypes().size()
        <= wire.src().portIndex()) {
      throw new NetworkStructureException("Not existing out port in src gate (%s)"
          .formatted(gates().get(wire.src().gateIndex())));
    }
    if (gates().get(wire.dst().gateIndex()).inputPorts().size()
        <= wire.dst().portIndex()) {
      throw new NetworkStructureException("Not existing in port in dst gate (%s)"
          .formatted(gates().get(wire.dst().gateIndex())));
    }
  }

  public void validateType(Wire wire) throws NetworkStructureException {
    Type dstType = gates.get(wire.dst().gateIndex())
        .inputPorts()
        .get(wire.dst().portIndex())
        .type();
    Type srcType = actualType(wire);
    if (!dstType.canTakeValuesOf(srcType)) {
      throw new NetworkStructureException("Not consistent types: src=%s, dst=%s"
          .formatted(srcType, dstType));
    }
  }

  private List<Wire> wiresFrom(Wire.EndPoint src) {
    return wires.stream().filter(w -> w.src().equals(src)).toList();
  }

  private Optional<Wire> wireTo(Wire.EndPoint dst) {
    return wires.stream().filter(w -> w.dst().equals(dst)).findFirst();
  }

  private Type actualType(Wire wire) {
    Type srcType = gates.get(wire.src().gateIndex()).outputTypes().get(wire.src().portIndex());
    return srcType;
  }

  public static void main(String[] args) throws NetworkStructureException {
    Network n = new Network(
        List.of(
            Gate.input(Composed.sequence(Base.REAL)),
            Gate.input(Composed.sequence(Base.REAL)),
            Gates.split(),
            Gates.split(),
            Gates.rPMathOperator(Element.Operator.MULTIPLICATION),
            Gates.rPMathOperator(Element.Operator.ADDITION),
            Gate.output(Base.REAL)
        ),
        Set.of(
            Wire.of(0, 0, 2, 0),
            Wire.of(1, 0, 3, 0),
            Wire.of(2, 0, 4, 0),
            Wire.of(3, 0, 4, 1),
            Wire.of(4, 0, 5, 0),
            Wire.of(5, 0, 6, 0)
        )
    );
    System.out.println(n);
    n.validate();
  }
}
