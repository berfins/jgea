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

package io.github.ericmedvet.jgea.experimenter.drawer;

import io.github.ericmedvet.jgea.core.representation.programsynthesis.ttpn.Gate;
import io.github.ericmedvet.jgea.core.representation.programsynthesis.ttpn.Network;
import io.github.ericmedvet.jgea.core.representation.programsynthesis.ttpn.Wire;
import io.github.ericmedvet.jgea.core.representation.programsynthesis.type.Base;
import io.github.ericmedvet.jgea.core.representation.programsynthesis.type.Type;
import io.github.ericmedvet.jgea.problem.image.ImageUtils;
import io.github.ericmedvet.jnb.datastructure.DoubleRange;
import io.github.ericmedvet.jviz.core.drawer.Drawer;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TTPNDrawer implements Drawer<Network> {

  private final Configuration configuration;

  public TTPNDrawer(Configuration configuration) {
    this.configuration = configuration;
  }

  public record Configuration(
      Color bgColor, Color fgColor, Map<Base, Color> baseTypeColors, Color otherTypeColor, double gateW, double wireW,
      double gateWHRatio, double portRadiusHRate, double gapWRate, double gapHRate, double marginWRate,
      double marginHRate, double gateOutputWRate, double gateOutputHRate, double ioGateOutputWRate,
      double ioGateOutputHRate, double textWMarginRate, double textHMarginRate,
      boolean showGateIndex
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
        100,
        5,
        1.5d,
        0.1d,
        0.5d,
        0.5d,
        0.75d,
        0.75d,
        0.9d,
        0.75d,
        0.5d,
        0.5d,
        0.05,
        0.05,
        true
    );
  }

  private record Metrics(
      double w, double h, int iW, int iH, List<Point> gatePoints, Map<Integer, SequencedSet<Wire>> xGapWires,
      Map<Integer, SequencedSet<Wire>> yGapWires
  ) {
    public static Metrics of(double w, double h, Network network) {
      List<Point> gatePoints = computeGatePoints(network);
      int iW = gatePoints.stream().mapToInt(Point::x).max().orElseThrow() + 1;
      int iH = gatePoints.stream().mapToInt(Point::y).max().orElseThrow() + 1;
      Map<Integer, SequencedSet<Wire>> xGapWires = new LinkedHashMap<>();
      Map<Integer, SequencedSet<Wire>> yGapWires = new LinkedHashMap<>();
      network.wires()
          .forEach(wire -> {
            Point srcPoint = gatePoints.get(wire.src().gateIndex());
            Point dstPoint = gatePoints.get(wire.dst().gateIndex());
            if (srcPoint.x + 1 == dstPoint.x) {
              xGapWires.computeIfAbsent(srcPoint.x, i -> new LinkedHashSet<>()).add(wire);
            } else {
              xGapWires.computeIfAbsent(srcPoint.x, i -> new LinkedHashSet<>()).add(wire);
              xGapWires.computeIfAbsent(dstPoint.x - 1, i -> new LinkedHashSet<>()).add(wire);
              yGapWires.computeIfAbsent(dstPoint.y, i -> new LinkedHashSet<>()).add(wire);
            }
          });
      return new Metrics(w, h, iW, iH, gatePoints, xGapWires, yGapWires);
    }
  }

  private record Point(int x, int y) {}

  private static List<Point> computeGatePoints(Network network) {
    Map<Integer, Point> map = new TreeMap<>();
    IntStream.range(0, network.gates().size())
        .boxed()
        .sorted(Comparator.comparingInt(gi -> network.wiresTo(gi).size()))
        //.filter(gi -> network.wiresTo(gi).isEmpty())
        .forEach(
            gi -> fillGatesPoints(
                network,
                gi,
                new Point(0, map.values().stream().mapToInt(p -> p.y + 1).max().orElse(0)),
                map
            )
        );
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
    return map.values().stream().toList();
  }

  private static void fillGatesPoints(Network network, int gi, Point current, Map<Integer, Point> map) {
    Gate gate = network.gates().get(gi);
    if (map.containsKey(gi)) {
      return;
    }
    if (network.wiresTo(gi).isEmpty()) {
      current = new Point(0, map.values().stream().mapToInt(Point::y).max().orElse(-1) + 1);
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
                            map.values()
                                .stream()
                                .filter(p -> p.x == currentX + 1)
                                .filter(p -> p.y >= currentY)
                                .mapToInt(Point::y)
                                .min()
                                .orElse(currentY - 1) + 1
                        ),
                        map
                    )
                )
        );
  }

  private static int indexOf(Class<? extends Gate> gateClass, int gi, Network network) {
    return IntStream.range(0, network.gates().size())
        .filter(lgi -> gateClass.isInstance(network.gates().get(lgi)))
        .boxed()
        .toList()
        .indexOf(gi);
  }

  private static <T> int indexOf(T t, SequencedSet<T> s) {
    int c = 0;
    for (T currentT : s) {
      if (t.equals(currentT)) {
        return c;
      }
      c = c + 1;
    }
    return -1;
  }

  private Path2D computeWirePath(Metrics m, Wire w, Network network) {
    Point srcPoint = m.gatePoints.get(w.src().gateIndex());
    Point dstPoint = m.gatePoints.get(w.dst().gateIndex());
    double srcX = gateXRange(srcPoint.x, m).max();
    double srcY = nThPos(
        w.src().portIndex(),
        network.gates().get(w.src().gateIndex()).outputTypes().size(),
        gateYRange(srcPoint.y, m)
    );
    double dstX = gateXRange(dstPoint.x, m).min();
    double dstY = nThPos(
        w.dst().portIndex(),
        network.gates().get(w.dst().gateIndex()).inputPorts().size(),
        gateYRange(dstPoint.y, m)
    );
    Path2D p = new Path2D.Double();
    p.moveTo(srcX, srcY);
    double wp1x = nThPos(
        indexOf(w, m.xGapWires().get(srcPoint.x)),
        m.xGapWires().get(srcPoint.x).size(),
        gapXRange(srcPoint.x, m)
    );
    p.lineTo(wp1x, srcY);
    if (srcPoint.x + 1 == dstPoint.x) {
      p.lineTo(wp1x, dstY);
    } else {
      double wp2y = nThPos(
          indexOf(w, m.yGapWires().get(dstPoint.y)),
          m.yGapWires().get(dstPoint.y).size(),
          gapYRange(dstPoint.y, m)
      );
      double wp3x = nThPos(
          indexOf(w, m.xGapWires().get(dstPoint.x - 1)),
          m.xGapWires().get(dstPoint.x - 1).size(),
          gapXRange(dstPoint.x - 1, m)
      );
      p.lineTo(wp1x, wp2y);
      p.lineTo(wp3x, wp2y);
      p.lineTo(wp3x, dstY);
    }
    p.lineTo(dstX, dstY);
    return p;
  }

  @Override
  public void draw(Graphics2D g, Network network) {
    Metrics m = Metrics.of(g.getClipBounds().getWidth(), g.getClipBounds().getHeight(), network);
    //draw gates
    IntStream.range(0, network.gates().size()).forEach(gi -> drawGate(g, m, gi, network));
    //draw wires
    network.wires().forEach(w -> drawWire(g, m, w, network));
  }

  private void drawWire(Graphics2D g, Metrics m, Wire w, Network network) {
    Path2D path = computeWirePath(m, w, network);
    Stroke stroke = g.getStroke();
    g.setColor(configuration.fgColor);
    g.setStroke(
        new BasicStroke(
            (float) configuration.wireW,
            BasicStroke.CAP_BUTT,
            BasicStroke.JOIN_BEVEL
        )
    );
    g.draw(path);
    g.setColor(configuration.bgColor);
    g.setStroke(
        new BasicStroke(
            (float) configuration.wireW - 2f,
            BasicStroke.CAP_BUTT,
            BasicStroke.JOIN_BEVEL
        )
    );
    g.draw(path);
    g.setStroke(stroke);
  }

  private void drawGate(Graphics2D g, Metrics m, int gi, Network network) {
    DoubleRange xR = gateXRange(m.gatePoints.get(gi).x, m);
    DoubleRange yR = gateYRange(m.gatePoints.get(gi).y, m);
    // draw gate
    Gate gate = network.gates().get(gi);
    Shape s = switch (gate) {
      case Gate.InputGate inputGate -> {
        Path2D p = new Path2D.Double();
        p.moveTo(xR.min(), yR.min());
        p.lineTo(xR.denormalize(configuration.ioGateOutputWRate), yR.min());
        p.lineTo(xR.max(), yR.denormalize((1d - configuration.ioGateOutputHRate) / 2d));
        p.lineTo(xR.max(), yR.denormalize(1d - (1d - configuration.ioGateOutputHRate) / 2d));
        p.lineTo(xR.denormalize(configuration.ioGateOutputWRate), yR.max());
        p.lineTo(xR.min(), yR.max());
        p.closePath();
        yield p;
      }
      case Gate.OutputGate outputGate -> {
        Path2D p = new Path2D.Double();
        p.moveTo(xR.min(), yR.denormalize((1d - configuration.ioGateOutputHRate) / 2d));
        p.lineTo(xR.denormalize(1d - configuration.ioGateOutputWRate), yR.min());
        p.lineTo(xR.max(), yR.min());
        p.lineTo(xR.max(), yR.max());
        p.lineTo(xR.denormalize(1d - configuration.ioGateOutputWRate), yR.max());
        p.lineTo(xR.min(), yR.denormalize(1d - (1d - configuration.ioGateOutputHRate) / 2d));
        p.closePath();
        yield p;
      }
      default -> {
        Path2D p = new Path2D.Double();
        p.moveTo(xR.min(), yR.min());
        p.lineTo(xR.denormalize(configuration.gateOutputWRate), yR.min());
        p.lineTo(xR.max(), yR.denormalize((1d - configuration.gateOutputHRate) / 2d));
        p.lineTo(xR.max(), yR.denormalize(1d - (1d - configuration.gateOutputHRate) / 2d));
        p.lineTo(xR.denormalize(configuration.gateOutputWRate), yR.max());
        p.lineTo(xR.min(), yR.max());
        p.closePath();
        yield p;
      }
    };
    g.setColor(configuration.bgColor);
    g.fill(s);
    g.setColor(configuration.fgColor);
    g.draw(s);
    // draw ports
    IntStream.range(0, gate.inputPorts().size())
        .forEach(
            pi -> drawPort(
                g,
                gate.inputPorts().get(pi).type(),
                gate.inputPorts().get(pi).toString(),
                pi,
                gate.inputPorts().size(),
                xR,
                yR,
                true
            )
        );
    IntStream.range(0, gate.outputTypes().size())
        .forEach(
            pi -> drawPort(
                g,
                gate.outputTypes().get(pi),
                gate.outputTypes().get(pi).toString(),
                pi,
                gate.outputTypes().size(),
                xR,
                yR,
                false
            )
        );
    // write name
    g.setColor(configuration.fgColor);
    Shape originalClip = g.getClip();
    g.setClip(s);
    String str = switch (gate) {
      case Gate.InputGate inputGate -> "I-%d".formatted(indexOf(Gate.InputGate.class, gi, network));
      case Gate.OutputGate outputGate -> "O-%d".formatted(indexOf(Gate.OutputGate.class, gi, network));
      default -> gate.operator().toString();
    };
    if (configuration.showGateIndex) {
      str = str + "[%d]".formatted(gi);
    }
    Rectangle2D strR = ImageUtils.bounds(str, g.getFont(), g);
    float labelY = (float) (yR.min() + strR.getHeight() + yR.extent() * configuration.textHMarginRate);
    float labelX = switch (gate) {
      case Gate.InputGate inputGate -> (float) (xR.min() + xR.extent() * configuration.textHMarginRate);
      case Gate.OutputGate outputGate ->
        (float) (xR.max() - strR.getWidth() - xR.extent() * configuration.textHMarginRate);
      default -> (float) (xR.center() - strR.getWidth() / 2d);
    };
    g.drawString(str, labelX, labelY);
    // write generics
    if (gate.hasGenerics()) {
      str = network.concreteMapping(gi)
          .entrySet()
          .stream()
          .sorted(Comparator.comparing(e -> e.getKey().toString()))
          .map(Object::toString)
          .collect(Collectors.joining(", "));
      strR = ImageUtils.bounds(str, g.getFont(), g);
      g.drawString(
          str,
          (float) (xR.center() - strR.getWidth() / 2d),
          (float) (yR.max() - yR.extent() * configuration.textHMarginRate)
      );
    }
    g.setClip(originalClip);
  }

  private void drawPort(
      Graphics2D g,
      Type type,
      String label,
      int pi,
      int nPorts,
      DoubleRange xR,
      DoubleRange yR,
      boolean isInput
  ) {
    double pR = yR.extent() * configuration.portRadiusHRate;
    //noinspection SuspiciousMethodCalls
    g.setColor(configuration.baseTypeColors.getOrDefault(type, configuration.otherTypeColor));
    Rectangle2D s = isInput ? new Rectangle2D.Double(
        xR.min(),
        nThPos(pi, nPorts, yR) - pR,
        2d * pR,
        2d * pR
    ) : new Rectangle2D.Double(xR.max() - 2d * pR, nThPos(pi, nPorts, yR) - pR, 2d * pR, 2d * pR);
    g.fill(s);
    g.setColor(configuration.fgColor);
    g.draw(s);
    g.setColor(configuration.fgColor);
    Rectangle2D strR = ImageUtils.bounds(label, g.getFont(), g);
    if (isInput) {
      g.drawString(label, (float) (xR.min() + 2.5d * pR), (float) (nThPos(pi, nPorts, yR) + 2d * pR + strR.getMinY()));
    } else {
      g.drawString(
          label,
          (float) (xR.max() - 2.5d * pR - strR.getWidth()),
          (float) (nThPos(pi, nPorts, yR) + 2d * pR + strR.getMinY())
      );
    }
  }

  private DoubleRange gapXRange(int x, Metrics m) {
    return new DoubleRange(gateXRange(x, m).max(), gateXRange(x + 1, m).min());
  }

  private DoubleRange gapYRange(int y, Metrics m) {
    return new DoubleRange(gateYRange(y, m).max(), gateYRange(y + 1, m).min());
  }

  private DoubleRange gateXRange(int x, Metrics m) {
    double cells = (double) m.iW + ((double) (m.iW - 1)) * configuration.gapWRate + 2 * configuration.marginWRate;
    double xs = configuration.marginWRate + (double) x + (x * configuration.gapWRate);
    return new DoubleRange(xs / cells * m.w, (xs + 1) / cells * m.w);
  }

  private DoubleRange gateYRange(int y, Metrics m) {
    double cells = (double) m.iH + ((double) (m.iH - 1)) * configuration.gapHRate + 2 * configuration.marginHRate;
    double ys = configuration.marginHRate + (double) y + (y * configuration.gapHRate);
    return new DoubleRange(ys / cells * m.h, (ys + 1) / cells * m.h);
  }

  @Override
  public ImageInfo imageInfo(Network network) {
    Metrics m = Metrics.of(1d, 1d, network);
    return new ImageInfo(
        (int) (configuration.gateW * (m.iW() + configuration.gapWRate() * (m
            .iW() - 1d) + 2d * configuration.marginWRate)),
        (int) (configuration.gateW / configuration.gateWHRatio() * (m.iH() + configuration.gapHRate() * (m
            .iH() - 1d) + 2d * configuration.marginHRate))
    );
  }

  private double nThPos(int i, int n, DoubleRange range) {
    return range.denormalize((double) (i + 1) / (double) (n + 1));
  }

}
