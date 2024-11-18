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

import io.github.ericmedvet.jgea.core.representation.ttpn.Gate;
import io.github.ericmedvet.jgea.core.representation.ttpn.Network;
import io.github.ericmedvet.jviz.core.drawer.Drawer;

import java.awt.*;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.IntStream;

public class TTPNDrawer implements Drawer<Network> {

  private final Configuration configuration;

  public TTPNDrawer(Configuration configuration) {
    this.configuration = configuration;
  }

  public record Configuration() {}

  private record Point(int x, int y) {}

  private static Map<Integer, Point> computeGatesPoints(Network network) {
    Map<Integer, Point> map = new TreeMap<>();
    IntStream.range(0, network.gates().size())
        .filter(gi -> network.gates().get(gi) instanceof Gate.InputGate)
        .forEach(gi -> fillGatesPoints(network, gi, new Point(0, 0), map));
    // pull output node to max right
    // TODO
    return map;
  }

  private static void fillGatesPoints(Network network, int gi, Point current, Map<Integer, Point> map) {
    Gate gate = network.gates().get(gi);
    if (current.x == 0 && current.y == 0 && !(gate instanceof Gate.InputGate)) {
      throw new RuntimeException("Starting node is not an input gate: %s".formatted(gate));
    }
    if (map.containsKey(gi)) {
      return;
    }
    if (gate instanceof Gate.InputGate) {
      current = new Point(
          0,
          map.values().stream().mapToInt(Point::y).max().orElse(0)+1
      );
    }
    map.put(gi, current);
    int currentX = current.x;
    int currentY = current.y;
    IntStream.range(0, gate.outputTypes().size())
        .forEach(pi -> network.wiresFrom(gi, pi).forEach(w -> fillGatesPoints(
            network, w.dst().gateIndex(), new Point(
                currentX + 1, currentY + pi), map
        )));
  }

  @Override
  public void draw(Graphics2D g, Network network) {
    Map<Integer, Point> gatePoints = computeGatesPoints(network);
    System.out.println(gatePoints);
  }
}
