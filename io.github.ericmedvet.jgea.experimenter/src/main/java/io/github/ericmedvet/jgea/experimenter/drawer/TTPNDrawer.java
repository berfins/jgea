/*-
 * ========================LICENSE_START=================================
 * jgea-experimenter
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
 * http://www.apache.org/licenses/LICENSE-2.0
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
import io.github.ericmedvet.jgea.core.representation.ttpn.type.Base;
import io.github.ericmedvet.jgea.core.representation.ttpn.type.Type;
import io.github.ericmedvet.jviz.core.drawer.Drawer;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TTPNDrawer implements Drawer<Network> {

  private final Configuration configuration;

  public TTPNDrawer(Configuration configuration) {
    this.configuration = configuration;
  }

  public record Configuration(
      Color bgColor,
      Color fgColor,
      Map<Base, Color> baseTypeColors,
      Color otherTypeColor,
      double gateWHRatio,
      double portRadiusHRate,
      double cellWMarginRate,
      double cellHMarginRate,
      double gateW
  ) {
    public static Configuration DEFAULT = new Configuration(
        Color.LIGHT_GRAY,
        Color.BLACK,
        Map.ofEntries(
            Map.entry(Base.INT, Color.BLUE),
            Map.entry(Base.REAL, Color.CYAN),
            Map.entry(Base.BOOLEAN, Color.RED),
            Map.entry(Base.STRING, Color.GREEN)
        ),
        Color.GRAY,
        2d,
        0.1d,
        2d,
        2d,
        100
    );
  }

  private record Point(int x, int y) {}

  private static Map<Integer, Point> computeGatesPoints(Network network) {
    Map<Integer, Point> map = new TreeMap<>();
    IntStream.range(0, network.gates().size())
        .filter(gi -> network.gates().get(gi) instanceof Gate.InputGate)
        .forEach(gi -> fillGatesPoints(network, gi, new Point(0, 0), map));
    // pull output node to max right
    int maxX = map.values().stream().mapToInt(Point::x).max().orElseThrow();
    Map<Integer, Point> outputsMap = map.entrySet()
        .stream()
        .filter(
            e -> network.gates()
                .get(e.getKey()) instanceof Gate.OutputGate
        )
        .collect(
            Collectors.toMap(
                Map.Entry::getKey,
                e -> new Point(maxX, e.getValue().y)
            )
        );
    map.putAll(outputsMap);
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
          map.values().stream().mapToInt(Point::y).max().orElse(-1) + 1
      );
    }
    map.put(gi, current);
    int currentX = current.x;
    int currentY = current.y;
    IntStream.range(0, gate.outputTypes().size())
        .forEach(
            pi -> network.wiresFrom(gi, pi)
                .forEach(
                    w -> fillGatesPoints(
                        network,
                        w.dst().gateIndex(),
                        new Point(
                            currentX + 1,
                            currentY + pi
                        ),
                        map
                    )
                )
        );
  }


  @Override
  public ImageInfo imageInfo(Network network) {
    Map<Integer, Point> gatePoints = computeGatesPoints(network);
    double ggW = gatePoints.values().stream().mapToInt(Point::x).max().orElseThrow() + 1d;
    double ggH = gatePoints.values().stream().mapToInt(Point::y).max().orElseThrow() + 1d;
    double gW = configuration.gateW;
    double gH = gW / configuration.gateWHRatio;
    double w = ggW * gW + (ggW + 1d) * gW * configuration.cellWMarginRate;
    double h = ggH * gH + (ggH + 1d) * gH * configuration.cellHMarginRate;
    return new ImageInfo((int) w, (int) h);
  }

  @Override
  public void draw(Graphics2D g, Network network) {
    Map<Integer, Point> gatePoints = computeGatesPoints(network);
    double gW = configuration.gateW;
    double gH = gW / configuration.gateWHRatio;
    double mW = gW * configuration.cellWMarginRate;
    double mH = gH * configuration.cellHMarginRate;
    //draw gates
    IntStream.range(0, network.gates().size())
        .forEach(
            gi -> drawGate(
                g,
                network.gates().get(gi),
                gatePoints.get(gi),
                indexOf(network.gates().get(gi), gi, network),
                gW,
                gH,
                mW,
                mH
            )
        );
  }

  private void drawGate(
      Graphics2D g,
      Gate gate,
      Point point,
      int indexOfType,
      double gW,
      double gH,
      double mW,
      double mH
  ) {
    double x = mW + (mW + gW) * point.x;
    double y = mH + (mH + gH) * point.y;
    // draw gate
    Shape s = switch (gate) {
      default -> new Rectangle2D.Double(x, y, gW, gH);
    };
    g.setColor(configuration.bgColor);
    g.fill(s);
    g.setColor(configuration.fgColor);
    g.draw(s);
    // draw ports
    IntStream.range(0, gate.inputPorts().size())
        .forEach(pi -> drawPort(g, gate.inputPorts().get(pi).type(), pi, gate.inputPorts().size(), gH, x, y, true));
    IntStream.range(0, gate.outputTypes().size())
        .forEach(pi -> drawPort(g, gate.outputTypes().get(pi), pi, gate.outputTypes().size(), gH, x + gW, y, false));
    // write name
    g.setColor(configuration.fgColor);
    Shape originalClip = g.getClip();
    g.setClip(s);
    String str = switch (gate) {
      case Gate.InputGate inputGate -> "I%d".formatted(indexOfType);
      case Gate.OutputGate outputGate -> "O%d".formatted(indexOfType);
      default -> gate.operator().toString();
    };
    Rectangle2D strR = g.getFontMetrics().getStringBounds(str, g);
    g.drawString(str, (float) (x + gW / 2d - strR.getWidth() / 2d), (float) (y + gH / 2d + strR.getHeight() / 2d));
    g.setClip(originalClip);
  }

  private void drawPort(Graphics2D g, Type type, int pi, int nPorts, double gH, double x, double y, boolean isInput) {
    double pR = gH * configuration.portRadiusHRate;
    g.setColor(configuration.baseTypeColors.getOrDefault(type, configuration.otherTypeColor));
    Rectangle2D circle = new Rectangle2D.Double(
        x - (isInput ? 0 : pR * 2),
        y + portY(gH, nPorts, pi) - pR,
        2d * pR,
        2d * pR
    );
    g.fill(circle);
    g.setColor(configuration.fgColor);
    g.draw(circle);
  }

  private double portY(double h, int nPorts, int pi) {
    double dY = h / (double) nPorts;
    return dY / 2 + pi * dY;
  }

  private int indexOf(Gate gate, int gi, Network network) {
    return IntStream.range(0, network.gates().size())
        .filter(
            lgi -> gate.getClass()
                .isInstance(network.gates().get(lgi))
        )
        .boxed()
        .toList()
        .indexOf(gi);
  }
}
