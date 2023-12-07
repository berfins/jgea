/*-
 * ========================LICENSE_START=================================
 * jgea-experimenter
 * %%
 * Copyright (C) 2018 - 2023 Eric Medvet
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
package io.github.ericmedvet.jgea.experimenter.listener.plot.image;

import io.github.ericmedvet.jgea.core.util.Table;
import io.github.ericmedvet.jgea.experimenter.listener.plot.*;
import io.github.ericmedvet.jsdynsym.core.DoubleRange;
import io.github.ericmedvet.jsdynsym.grid.Grid;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.swing.*;

/**
 * @author "Eric Medvet" on 2023/12/01 for jgea
 */
public class ImagePlotter implements Plotter<BufferedImage> {
  private final double w;
  private final double h;
  private final Configuration c;

  private final Map<Configuration.Text.Use, Font> fonts;

  public ImagePlotter(int w, int h) {
    this(w, h, Configuration.DEFAULT);
  }

  public ImagePlotter(int w, int h, Configuration c) {
    this.w = w;
    this.h = h;
    this.c = c;
    fonts = Arrays.stream(Configuration.Text.Use.values())
        .collect(Collectors.toMap(
            u -> u,
            u -> new Font(c.text().fontName(), Font.PLAIN, (int) Math.round((double) Math.max(w, h)
                * c.text().sizeRates().getOrDefault(u, c.text().fontSizeRate())))));
  }

  private enum AnchorH {
    L,
    C,
    R
  }

  private enum AnchorV {
    T,
    C,
    B
  }

  private record Axes(
      DoubleRange xRange,
      DoubleRange yRange,
      List<Double> xTicks,
      List<Double> yTicks,
      String xLabelFormat,
      String yLabelFormat) {
    double xIn(double x, Rectangle2D r) {
      return r.getX() + r.getWidth() * xRange.normalize(x);
    }

    double yIn(double y, Rectangle2D r) {
      return r.getY() + r.getHeight() * (1 - yRange.normalize(y));
    }
  }

  private record RangeMap(DoubleRange xRange, DoubleRange yRange, DoubleRange gxRange, DoubleRange gyRange) {}

  private static String computeTicksFormat(DoubleRange range, int nOfTicks) {
    int nOfDigits = 0;
    double[] ticks = DoubleStream.iterate(
            range.min(), v -> v <= range.max(), v -> v + range.extent() / (double) nOfTicks)
        .toArray();
    while (true) {
      final int d = nOfDigits;
      long nOfDistinct = Arrays.stream(ticks)
          .mapToObj(("%." + d + "f")::formatted)
          .map(String::toString)
          .distinct()
          .count();
      if (nOfDistinct == ticks.length) {
        break;
      }
      nOfDigits = nOfDigits + 1;
    }
    return "%." + nOfDigits + "f";
  }

  public static void main(String[] args) {
    RandomGenerator rg = new Random(1);
    XYDataSeries ds1 = XYDataSeries.of(
        "sin(x)",
        DoubleStream.iterate(-2.5, v -> v < 1.5, v -> v + .01)
            .mapToObj(x -> new XYDataSeries.Point(
                Value.of(x),
                RangedValue.of(
                    Math.sin(x),
                    Math.sin(x) - 0.1 - Math.abs(0.1 * rg.nextGaussian()),
                    Math.sin(x) + 0.1 + Math.abs(0.05 * rg.nextGaussian()))))
            .toList());
    XYDataSeries ds2 = XYDataSeries.of(
        "sin(x)/(1+|x|)",
        DoubleStream.iterate(-2, v -> v < 15, v -> v + .1)
            .mapToObj(x -> new XYDataSeries.Point(Value.of(x), Value.of(Math.sin(x) / (1d + Math.abs(x)))))
            .toList());
    XYDataSeries ds3 = XYDataSeries.of(
        "1+sin(1+x^2)",
        DoubleStream.iterate(0, v -> v < 10, v -> v + .1)
            .mapToObj(x -> new XYDataSeries.Point(Value.of(x), Value.of(1 + Math.sin(1 + x * x))))
            .toList());
    XYDataSeries ds4 = XYDataSeries.of(
        "1+sin(2+x^0.5)",
        DoubleStream.iterate(0, v -> v < 10, v -> v + .1)
            .mapToObj(
                x -> new XYDataSeries.Point(Value.of(x), Value.of(1 + Math.sin(2 + Math.pow(x, 0.5)))))
            .toList());
    XYDataSeries ds5 = XYDataSeries.of(
        "1+sin(1+x^0.2)",
        DoubleStream.iterate(0, v -> v < 10, v -> v + .1)
            .mapToObj(
                x -> new XYDataSeries.Point(Value.of(x), Value.of(1 + Math.sin(1 + Math.pow(x, 0.2)))))
            .toList());
    XYSinglePlot p = XYSinglePlot.of(
        "functions with a very long title",
        "xj",
        "y",
        DoubleRange.UNBOUNDED,
        DoubleRange.UNBOUNDED,
        List.of(ds1, ds2));
    ImagePlotter ip = new ImagePlotter(800, 600);
    showImage(ip.plot(p));
    XYMatrixPlot m = XYMatrixPlot.of(
        "functions matrix",
        "xj",
        "y",
        DoubleRange.UNBOUNDED,
        DoubleRange.UNBOUNDED,
        Table.of(Map.ofEntries(
            Map.entry(
                "row veryyyy loooooong",
                Map.ofEntries(Map.entry("a", List.of(ds1)), Map.entry("b", List.of(ds1, ds2)))),
            Map.entry(
                "ea.p.s.sphere(p = 2)",
                Map.ofEntries(
                    Map.entry("a", List.of(ds2, ds3, ds4, ds5)), Map.entry("b", List.of(ds1)))))));
    showImage(ip.plot(m));
  }

  private static DoubleRange range(XYDataSeries dataSeries, ToDoubleFunction<XYDataSeries.Point> vExtractor) {
    return new DoubleRange(
        dataSeries.points().stream().mapToDouble(vExtractor).min().orElse(0d),
        dataSeries.points().stream().mapToDouble(vExtractor).max().orElse(1d));
  }

  private static DoubleRange range(
      Collection<XYDataSeries> dataSeries, ToDoubleFunction<XYDataSeries.Point> vExtractor) {
    return new DoubleRange(
        dataSeries.stream()
            .mapToDouble(ds -> ds.points().stream()
                .mapToDouble(vExtractor)
                .min()
                .orElse(0d))
            .min()
            .orElse(0d),
        dataSeries.stream()
            .mapToDouble(ds -> ds.points().stream()
                .mapToDouble(vExtractor)
                .max()
                .orElse(1d))
            .max()
            .orElse(1d));
  }

  private static <T> List<T> reverse(List<T> ts) {
    return IntStream.range(0, ts.size())
        .mapToObj(i -> ts.get(ts.size() - 1 - i))
        .toList();
  }

  public static void showImage(BufferedImage image) {
    EventQueue.invokeLater(() -> {
      JFrame frame = new JFrame("Image");
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      frame.add(new JPanel() {
        protected void paintComponent(Graphics g) {
          super.paintComponent(g);
          Dimension d = getSize();
          Graphics2D g2d = (Graphics2D) g.create();
          g2d.drawImage(image, (d.width - image.getWidth()) / 2, (d.height - image.getHeight()) / 2, this);
          g2d.dispose();
        }

        public Dimension getPreferredSize() {
          return new Dimension(image.getWidth(), image.getHeight());
        }
      });
      frame.pack();
      frame.setLocationRelativeTo(null);
      frame.setVisible(true);
    });
  }

  private static double x(double x, RangeMap rm) {
    return rm.gxRange.denormalize(rm.xRange.normalize(x));
  }

  private static double y(double y, RangeMap rm) {
    return rm.gyRange.max() - rm.gyRange.extent() * rm.yRange.normalize(y);
  }

  private Point2D center(Rectangle2D r) {
    return new Point2D.Double(r.getCenterX(), r.getCenterY());
  }

  private Grid<Axes> computeAxes(
      Graphics2D g, Layout l, Grid<List<XYDataSeries>> dataGrid, DoubleRange xRange, DoubleRange yRange) {
    // compute ranges
    Grid<Axes> grid = dataGrid.map(d -> new Axes(
        xRange.equals(DoubleRange.UNBOUNDED) ? range(d, p -> p.x().v()) : xRange,
        yRange.equals(DoubleRange.UNBOUNDED) ? range(d, p -> p.y().v()) : yRange,
        List.of(),
        List.of(),
        "",
        ""));
    // compute ranges
    List<DoubleRange> colXLargestRanges =
        grid.columns().stream().map(c -> largestRange(c, Axes::xRange)).toList();
    List<DoubleRange> colYLargestRanges =
        grid.columns().stream().map(c -> largestRange(c, Axes::yRange)).toList();
    List<DoubleRange> rowXLargestRanges =
        grid.rows().stream().map(c -> largestRange(c, Axes::xRange)).toList();
    List<DoubleRange> rowYLargestRanges =
        grid.rows().stream().map(c -> largestRange(c, Axes::yRange)).toList();
    DoubleRange largestXRange = Stream.of(colXLargestRanges, rowXLargestRanges)
        .flatMap(List::stream)
        .reduce((r1, r2) -> new DoubleRange(Math.min(r1.min(), r2.min()), Math.max(r1.max(), r2.max())))
        .orElseThrow();
    DoubleRange largestYRange = Stream.of(colYLargestRanges, rowYLargestRanges)
        .flatMap(List::stream)
        .reduce((r1, r2) -> new DoubleRange(Math.min(r1.min(), r2.min()), Math.max(r1.max(), r2.max())))
        .orElseThrow();
    // adjust considering independency and add labels
    return grid.keys().stream()
        .map(k -> new Grid.Entry<>(
            k,
            new Axes(
                plotRange(
                    true,
                    grid.get(k).xRange,
                    colXLargestRanges.get(k.x()),
                    rowXLargestRanges.get(k.y()),
                    largestXRange),
                plotRange(
                    false,
                    grid.get(k).yRange,
                    colYLargestRanges.get(k.x()),
                    rowYLargestRanges.get(k.y()),
                    largestYRange),
                List.of(),
                List.of(),
                "",
                "")))
        .map(e -> new Grid.Entry<>(
            e.key(),
            new Axes(
                e.value().xRange,
                e.value().yRange,
                computeTicks(
                    g,
                    e.value().xRange(),
                    l.plotInnerW() * c.general().plotDataRatio()),
                computeTicks(
                    g,
                    e.value().yRange(),
                    l.plotInnerH() * c.general().plotDataRatio()),
                "",
                "")))
        .map(e -> new Grid.Entry<>(
            e.key(),
            new Axes(
                e.value().xRange,
                e.value().yRange,
                e.value().xTicks,
                e.value().yTicks,
                computeTicksFormat(
                    e.value().xRange, e.value().xTicks.size()),
                computeTicksFormat(
                    e.value().yRange, e.value().yTicks.size()))))
        .collect(Grid.collector());
  }

  private SortedMap<String, Color> computeDataColors(List<XYDataSeries> dataSeries) {
    List<String> names =
        dataSeries.stream().map(XYDataSeries::name).distinct().toList();
    return new TreeMap<>(
        IntStream.range(0, names.size()).boxed().collect(Collectors.toMap(names::get, i -> c.colors()
            .dataColors()
            .get(i % c.colors().dataColors().size()))));
  }

  private Layout computeLayout(Graphics2D g, XYMatrixPlot plot) {
    double initialXAxisL = computeStringW(g, "0", Configuration.Text.Use.TICK_LABEL)
        + 2d * c.layout().xAxisMarginHRate() * h
        + c.layout().xAxisInnerMarginHRate() * h;
    double initialYAxisL = computeStringW(g, "0", Configuration.Text.Use.TICK_LABEL)
        + 2d * c.layout().yAxisMarginWRate() * w
        + c.layout().yAxisInnerMarginWRate() * w;
    // build an empty layout
    Layout l = new Layout(
        w,
        h,
        plot.dataSeries().nColumns(),
        plot.dataSeries().nRows(),
        plot.title().isEmpty()
            ? 0
            : (computeStringH(g, "a", Configuration.Text.Use.TITLE)
                + 2d * c.layout().mainTitleMarginHRate() * w),
        computeLegendH(
                g,
                w,
                h,
                computeDataColors(plot.dataSeries().values().stream()
                    .flatMap(List::stream)
                    .toList()))
            + 2d * c.layout().legendMarginHRate() * h,
        c.plotMatrix().titlesShow().equals(Configuration.PlotMatrix.Show.BORDER)
            ? plot.dataSeries().colIndexes().stream().allMatch(String::isEmpty)
                ? 0
                : (computeStringH(g, "a", Configuration.Text.Use.AXIS_LABEL)
                    + 2d * c.layout().colTitleMarginHRate() * h)
            : 0,
        c.plotMatrix().titlesShow().equals(Configuration.PlotMatrix.Show.BORDER)
            ? plot.dataSeries().rowIndexes().stream().allMatch(String::isEmpty)
                ? 0
                : (computeStringH(g, "a", Configuration.Text.Use.AXIS_LABEL)
                    + 2d * c.layout().rowTitleMarginWRate() * w)
            : 0,
        c.plotMatrix().axesShow().equals(Configuration.PlotMatrix.Show.BORDER) ? initialXAxisL : 0,
        c.plotMatrix().axesShow().equals(Configuration.PlotMatrix.Show.BORDER) ? initialYAxisL : 0,
        c.plotMatrix().axesShow().equals(Configuration.PlotMatrix.Show.BORDER) ? 0 : initialXAxisL,
        c.plotMatrix().axesShow().equals(Configuration.PlotMatrix.Show.BORDER) ? 0 : initialYAxisL,
        c.plotMatrix().titlesShow().equals(Configuration.PlotMatrix.Show.BORDER)
            ? 0
            : plot.dataSeries().colIndexes().stream().allMatch(String::isEmpty)
                ? 0
                : (computeStringH(g, "a", Configuration.Text.Use.AXIS_LABEL)
                    + 2d * c.layout().colTitleMarginHRate() * h),
        c.plotMatrix().titlesShow().equals(Configuration.PlotMatrix.Show.BORDER)
            ? 0
            : plot.dataSeries().rowIndexes().stream().allMatch(String::isEmpty)
                ? 0
                : (computeStringH(g, "a", Configuration.Text.Use.AXIS_LABEL)
                    + 2d * c.layout().rowTitleMarginWRate() * w),
        c.layout());
    // iterate
    Grid<List<XYDataSeries>> dataGrid =
        Grid.create(plot.dataSeries().nColumns(), plot.dataSeries().nRows(), (x, y) -> plot.dataSeries()
            .get(x, y));
    int nOfIterations = 3;
    for (int i = 0; i < nOfIterations; i = i + 1) {
      Grid<Axes> axesGrid = computeAxes(g, l, dataGrid, plot.xRange(), plot.yRange());
      List<String> xTickLabels = axesGrid.values().stream()
          .map(a -> a.xTicks.stream().map(a.xLabelFormat::formatted).toList())
          .flatMap(List::stream)
          .toList();
      List<String> yTickLabels = axesGrid.values().stream()
          .map(a -> a.yTicks.stream().map(a.yLabelFormat::formatted).toList())
          .flatMap(List::stream)
          .toList();
      double maxXTickL = xTickLabels.stream()
          .mapToDouble(s -> computeStringW(g, s, Configuration.Text.Use.TICK_LABEL))
          .max()
          .orElse(0);
      double maxYTickL = yTickLabels.stream()
          .mapToDouble(s -> computeStringW(g, s, Configuration.Text.Use.TICK_LABEL))
          .max()
          .orElse(0);
      l = l.refit(
          maxXTickL
              + computeStringH(g, "0", Configuration.Text.Use.AXIS_LABEL)
              + 2d * c.layout().xAxisMarginHRate() * h
              + c.layout().xAxisInnerMarginHRate() * h,
          maxYTickL
              + computeStringH(g, "0", Configuration.Text.Use.AXIS_LABEL)
              + 2d * c.layout().yAxisMarginWRate() * w
              + c.layout().yAxisInnerMarginWRate() * w);
    }
    return l;
  }

  private double computeLegendH(Graphics2D g, double w, double h, SortedMap<String, Color> dataColors) {
    double maxLineL = w - 2d * c.layout().legendMarginWRate() * w;
    double lineH = Math.max(
        c.general().legendImageHRate() * h, computeStringH(g, "0", Configuration.Text.Use.LEGEND_LABEL));
    double lH = lineH;
    double lineL = 0;
    for (String s : dataColors.keySet()) {
      double localL = c.general().legendImageWRate() * w
          + 2d * c.layout().legendInnerMarginWRate() * w
          + computeStringW(g, s, Configuration.Text.Use.LEGEND_LABEL);
      if (lineL + localL > maxLineL) {
        lH = lH + c.layout().legendInnerMarginHRate() * h + lineH;
        lineL = 0;
      }
      lineL = lineL + localL;
    }
    return lH;
  }

  private void drawLegend(Graphics2D g, Rectangle2D r, SortedMap<String, Color> dataColors) {
    if (c.debug()) {
      g.setStroke(new BasicStroke(1));
      g.setColor(Color.MAGENTA);
      g.draw(r);
    }
    double lineH = Math.max(
        c.general().legendImageHRate() * h, computeStringH(g, "0", Configuration.Text.Use.LEGEND_LABEL));
    double x = 0;
    double y = 0;
    for (Map.Entry<String, Color> e : dataColors.entrySet()) {
      double localL = c.general().legendImageWRate() * w
          + 2d * c.layout().legendInnerMarginWRate() * w
          + computeStringW(g, e.getKey(), Configuration.Text.Use.LEGEND_LABEL);
      if (x + localL > r.getWidth()) {
        y = y + c.layout().legendInnerMarginHRate() * h + lineH;
        x = 0;
      }
      g.setColor(c.colors().plotBgColor());
      g.fill(new Rectangle2D.Double(
          r.getX() + x,
          r.getY() + y,
          c.general().legendImageWRate() * w,
          c.general().legendImageHRate() * h));
      g.setColor(e.getValue());
      g.setStroke(new BasicStroke((float) (c.linePlot().dataStrokeSize() * Math.max(w, h))));
      g.draw(new Line2D.Double(
          r.getX() + x, r.getY() + y + c.general().legendImageHRate() * h / 2d,
          r.getX() + x + c.general().legendImageWRate() * w,
              r.getY() + y + c.general().legendImageHRate() * h / 2d));
      drawString(
          g,
          new Point2D.Double(
              r.getX()
                  + x
                  + c.general().legendImageWRate() * w
                  + c.layout().legendInnerMarginWRate() * w,
              r.getY() + y),
          e.getKey(),
          AnchorH.L,
          AnchorV.B,
          Configuration.Text.Use.LEGEND_LABEL,
          Configuration.Text.Direction.H,
          c.colors().titleColor());
      x = x + localL;
    }
  }

  private double computeStringH(Graphics2D g, String s, Configuration.Text.Use fontUse) {
    g.setFont(fonts.get(fontUse));
    return g.getFontMetrics().getHeight();
  }

  /*
  private void drawLegend(
  Graphics2D g,
  Rectangle2D legendSize,
  SortedMap<String, Color> colorMap,
  int w,
  int h,
  double fontH,
  double fontW
  ) {
  double x = (w - legendSize.getWidth() - 2d * c.margin) / 2d;
  double y = h - c.margin - legendSize.getHeight();
  g.setFont(c.hFont());
  g.setStroke(c.dataStroke());
  for (Map.Entry<String, Color> e : colorMap.entrySet()) {
  double localW = c.legendW + c.margin + (float) e.getKey().length() * fontW;
  if (x + c.margin + localW > w - c.margin) {
  x = (w - legendSize.getWidth() - 2d * c.margin) / 2d;
  y = y + c.margin + Math.max(fontH, c.legendH);
  }
  x = x + c.margin;
  g.setColor(c.plotBgColor);
  g.fill(new Rectangle2D.Double(x, y + fontH - c.legendH, c.legendW, c.legendH));
  g.setColor(e.getValue());
  g.draw(new Line2D.Double(
  x, y + fontH - c.legendH + c.legendH / 2d, x + c.legendW, y + fontH - c.legendH + c.legendH / 2d));
  x = x + c.legendW + c.margin;
  g.setColor(c.labelColor);
  g.drawString(e.getKey(), (float) x, (float) (y + fontH));
  x = x + (float) e.getKey().length() * fontW;
  }
  }

  private void drawLine(XYDataSeries<?, ?> ds, Color color, Graphics2D g, RangeMap rm) {
  if (ds.points().get(0).y() instanceof RangedValue) {
  Path2D sPath = new Path2D.Double();
  sPath.moveTo(
  x(ds.points().get(0).x().v(), rm), y(ds.points().get(0).y().v(), rm));
  ds.points().stream()
  .skip(1)
  .forEach(p -> sPath.lineTo(
  x(p.x().v(), rm), y(((RangedValue) p.y()).range().max(), rm)));
  reverse(ds.points())
  .forEach(p -> sPath.lineTo(
  x(p.x().v(), rm), y(((RangedValue) p.y()).range().min(), rm)));
  sPath.closePath();
  g.setColor(
  new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (color.getAlpha() * c.alpha())));
  g.fill(sPath);
  }
  g.setColor(color);
  g.setStroke(c.dataStroke());
  Path2D path = new Path2D.Double();
  path.moveTo(x(ds.points().get(0).x().v(), rm), y(ds.points().get(0).y().v(), rm));
  ds.points().stream().skip(1).forEach(p -> path.lineTo(x(p.x().v(), rm), y(p.y().v(), rm)));
  g.draw(path);
  }

  private  void drawLinePlot(
  Graphics2D g,
  RangeMap rm,
  List<XYDataSeries> dataSeries,
  SortedMap<String, Color> colorMap,
  List<Double> xTicks,
  List<Double> yTicks
  ) {
  // draw background
  g.setColor(c.plotBgColor);
  g.fill(new Rectangle2D.Double(rm.gxRange.min(), rm.gyRange.min(), rm.gxRange.extent(), rm.gyRange.extent()));
  // draw grid
  g.setStroke(c.gridStroke());
  g.setColor(c.gridColor);
  xTicks.forEach(x -> g.draw(new Line2D.Double(
  x(x, rm), y(rm.yRange.min(), rm),
  x(x, rm), y(rm.yRange.max(), rm)
  )));
  yTicks.forEach(y -> g.draw(new Line2D.Double(
  x(rm.xRange.min(), rm), y(y, rm),
  x(rm.xRange.max(), rm), y(y, rm)
  )));
  // draw data
  dataSeries.forEach(ds -> drawLine(ds, colorMap.get(ds.name()), g, rm));
  }

  private void drawTitle(Graphics2D g, int w, String title, double fontH, double fontW) {
  g.setColor(c.labelColor);
  g.setFont(c.hFont());
  g.drawString(title, (float) (w / 2d - (double) title.length() * fontW / 2d), (float) (c.margin + fontH));
  }

  private void drawXAxisText(
  Graphics2D g,
  RangeMap rm,
  List<Double> xTicks,
  String xTickFormat,
  String xName,
  double fontH,
  double fontW
  ) {
  g.setColor(c.tickLabelColor);
  g.setFont(c.vFont());
  xTicks.forEach(t -> {
  String s = xTickFormat.formatted(t);
  g.drawString(
  s, (float) (x(t, rm) + fontH / 2f), (float) (rm.gyRange.max() + c.margin + fontW * s.length()));
  });
  double labelsW = xTicks.stream()
  .mapToDouble(t -> xTickFormat.formatted(t).length() * fontW)
  .max()
  .orElse(0);
  g.setFont(c.hFont());
  g.setColor(c.labelColor);
  g.drawString(xName, (float) (rm.gxRange.min() + rm.gxRange.extent() / 2d - xName.length() * fontW / 2f), (float)
  (rm.gyRange.max() + c.margin + labelsW + c.margin + fontH));
  }

  private void drawYAxisText(
  Graphics2D g,
  RangeMap rm,
  List<Double> yTicks,
  String yTickFormat,
  String yName,
  double fontH,
  double fontW
  ) {
  g.setColor(c.tickLabelColor);
  g.setFont(c.hFont());
  yTicks.forEach(t -> {
  String s = yTickFormat.formatted(t);
  g.drawString(
  s, (float) (rm.gxRange.min() - c.margin - fontW * s.length()), (float) (y(t, rm) + fontH / 2f));
  });
  double labelsW = yTicks.stream()
  .mapToDouble(t -> yTickFormat.formatted(t).length() * fontW)
  .max()
  .orElse(0);
  g.setFont(c.vFont());
  g.setColor(c.labelColor);
  g.drawString(yName, (float) (rm.gxRange.min() - c.margin - labelsW - c.margin), (float)
  (y(rm.yRange.min() + rm.yRange.extent() / 2d, rm) + yName.length() * fontW / 2f));
  }*/

  private double computeStringW(Graphics2D g, String s, Configuration.Text.Use fontUse) {
    g.setFont(fonts.get(fontUse));
    return g.getFontMetrics().stringWidth(s);
  }

  private List<Double> computeTicks(Graphics2D g, DoubleRange range, double l) {
    DoubleRange innerRange = enlarge(range, c.general().plotDataRatio());
    double labelLineL = computeStringH(g, "1", Configuration.Text.Use.TICK_LABEL)
        * (1d + c.general().tickLabelGapRatio());
    int n = (int) Math.round(l / labelLineL);
    return DoubleStream.iterate(innerRange.min(), v -> v <= range.max(), v -> v + innerRange.extent() / (double) n)
        .boxed()
        .toList();
  }

  private void drawLine(Graphics2D g, Rectangle2D r, XYDataSeries ds, Axes a, Color color) {
    if (ds.points().get(0).y() instanceof RangedValue) {
      // draw shaded area
      Path2D sPath = new Path2D.Double();
      sPath.moveTo(
          a.xIn(ds.points().get(0).x().v(), r),
          a.yIn(ds.points().get(0).y().v(), r));
      ds.points().stream()
          .skip(1)
          .forEach(p -> sPath.lineTo(
              a.xIn(p.x().v(), r), a.yIn(RangedValue.range(p.y()).min(), r)));
      reverse(ds.points())
          .forEach(p -> sPath.lineTo(
              a.xIn(p.x().v(), r), a.yIn(RangedValue.range(p.y()).max(), r)));
      sPath.closePath();
      g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)
          (color.getAlpha() * c.linePlot().alpha())));
      g.fill(sPath);
    }
    // draw line
    g.setColor(color);
    g.setStroke(new BasicStroke((float) (c.linePlot().dataStrokeSize() * Math.max(w, h))));
    Path2D path = new Path2D.Double();
    path.moveTo(
        a.xIn(ds.points().get(0).x().v(), r),
        a.yIn(ds.points().get(0).y().v(), r));
    ds.points().stream().skip(1).forEach(p -> path.lineTo(a.xIn(p.x().v(), r), a.yIn(p.y().v(), r)));
    g.draw(path);
  }

  private void drawLinePlot(
      Graphics2D g, Rectangle2D r, List<XYDataSeries> dataSeries, Axes a, SortedMap<String, Color> dataColors) {
    // draw background
    g.setColor(c.colors().plotBgColor());
    g.fill(r);
    // draw border and grid
    g.setStroke(new BasicStroke((float) (c.general().gridStrokeSizeRate() * Math.max(w, h))));
    g.setColor(c.colors().plotBorderColor());
    g.draw(r);
    g.setClip(r);
    g.setColor(c.colors().gridColor());
    a.xTicks.forEach(x -> g.draw(new Line2D.Double(
        a.xIn(x, r), a.yIn(a.yRange.min(), r),
        a.xIn(x, r), a.yIn(a.yRange.max(), r))));
    a.yTicks.forEach(y -> g.draw(new Line2D.Double(
        a.xIn(a.xRange.min(), r), a.yIn(y, r),
        a.xIn(a.xRange.max(), r), a.yIn(y, r))));
    if (c.debug()) {
      g.setStroke(new BasicStroke(1));
      g.setColor(Color.MAGENTA);
      g.draw(r);
    }
    // draw data
    dataSeries.forEach(ds -> drawLine(g, r, ds, a, dataColors.get(ds.name())));
    // reset clip
    g.setClip(new Rectangle2D.Double(0, 0, w, h));
  }

  private void drawString(
      Graphics2D g,
      Point2D p,
      String s,
      AnchorH anchorH,
      AnchorV anchorV,
      Configuration.Text.Use use,
      Configuration.Text.Direction direction,
      Color color) {
    if (s.isEmpty()) {
      return;
    }
    g.setFont(fonts.get(use));
    double sW = computeStringW(g, s, use);
    double sH = computeStringH(g, s, use);
    double w =
        switch (direction) {
          case H -> sW;
          case V -> sH;
        };
    double h =
        switch (direction) {
          case H -> sH;
          case V -> sW;
        };
    double d = g.getFontMetrics().getDescent();
    double x =
        switch (anchorH) {
          case L -> p.getX();
          case C -> p.getX() - w / 2;
          case R -> p.getX() - w;
        };
    double y =
        switch (anchorV) {
          case T -> p.getY();
          case C -> p.getY() + h / 2;
          case B -> p.getY() + h;
        };
    if (c.debug()) {
      g.setStroke(new BasicStroke(1));
      g.setColor(Color.MAGENTA);
      g.draw(new Rectangle2D.Double(x, y - h, w, h));
    }
    g.setColor(color);
    if (direction.equals(Configuration.Text.Direction.V)) {
      g.setFont(g.getFont().deriveFont(AffineTransform.getRotateInstance(Math.toRadians(-90))));
      g.drawString(s, (float) (x + w - d), (float) y);
    } else {
      g.drawString(s, (float) x, (float) (y - d));
    }
  }

  private void drawXAxis(Graphics2D g, Rectangle2D r, String name, Axes a) {
    if (c.debug()) {
      g.setStroke(new BasicStroke(1));
      g.setColor(Color.MAGENTA);
      g.draw(r);
    }
    drawString(
        g,
        new Point2D.Double(r.getCenterX(), r.getY() + r.getHeight()),
        name,
        AnchorH.L,
        AnchorV.T,
        Configuration.Text.Use.AXIS_LABEL,
        Configuration.Text.Direction.H,
        c.colors().axisLabelColor());
    a.xTicks.forEach(x -> drawString(
        g,
        new Point2D.Double(a.xIn(x, r), r.getY()),
        a.xLabelFormat.formatted(x),
        AnchorH.C,
        AnchorV.B,
        Configuration.Text.Use.TICK_LABEL,
        Configuration.Text.Direction.V,
        c.colors().tickLabelColor()));
  }

  private void drawYAxis(Graphics2D g, Rectangle2D r, String name, Axes a) {
    if (c.debug()) {
      g.setStroke(new BasicStroke(1));
      g.setColor(Color.MAGENTA);
      g.draw(r);
    }
    drawString(
        g,
        new Point2D.Double(r.getX(), r.getCenterY()),
        name,
        AnchorH.L,
        AnchorV.C,
        Configuration.Text.Use.AXIS_LABEL,
        Configuration.Text.Direction.V,
        c.colors().axisLabelColor());
    a.yTicks.forEach(y -> drawString(
        g,
        new Point2D.Double(r.getX() + r.getWidth(), a.yIn(y, r)),
        a.yLabelFormat.formatted(y),
        AnchorH.R,
        AnchorV.C,
        Configuration.Text.Use.TICK_LABEL,
        Configuration.Text.Direction.H,
        c.colors().tickLabelColor()));
  }

  /*
  private Rectangle2D legendSize(SortedMap<String, Color> names, int w, double fontH, double fontW) {
  double legendH = Math.max(c.legendH, fontH);
  double legendW = 0;
  double lineW = -c.margin;
  for (String name : names.keySet()) {
  double localW = c.legendW + c.margin + name.length() * fontW;
  if (lineW + c.margin + localW > w - 2d * c.margin) {
  lineW = -c.margin;
  legendH = legendH + c.margin + Math.max(c.legendH, fontH);
  }
  lineW = lineW + c.margin + localW;
  legendW = Math.max(legendW, lineW);
  }
  return new Rectangle2D.Double(0, 0, legendW, legendH);
  }
   */

  private DoubleRange enlarge(DoubleRange range, double r) {
    return new DoubleRange(
        range.min() - range.extent() * (r - 1d) / 2d, range.max() + range.extent() * (r - 1d) / 2d);
  }

  private DoubleRange largestRange(List<Axes> axesList, Function<Axes, DoubleRange> rangeExtractor) {
    return axesList.stream()
        .map(rangeExtractor)
        .reduce((r1, r2) -> new DoubleRange(Math.min(r1.min(), r2.min()), Math.max(r1.max(), r2.max())))
        .orElseThrow();
  }

  @Override
  public BufferedImage plot(XYMatrixPlot plot) {
    // prepare image
    BufferedImage img = new BufferedImage((int) w, (int) h, BufferedImage.TYPE_3BYTE_BGR);
    Graphics2D g = img.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    // compute layout and axes
    Grid<List<XYDataSeries>> dataGrid =
        Grid.create(plot.dataSeries().nColumns(), plot.dataSeries().nRows(), (x, y) -> plot.dataSeries()
            .get(x, y));
    Layout l = computeLayout(g, plot);
    Grid<Axes> axesGrid = computeAxes(g, l, dataGrid, plot.xRange(), plot.yRange());
    SortedMap<String, Color> dataColors = computeDataColors(
        dataGrid.values().stream().flatMap(List::stream).toList());
    // clean
    g.setColor(c.colors().bgColor());
    g.fill(new Rectangle2D.Double(0, 0, w, h));
    // draw title
    drawString(
        g,
        center(l.mainTitle()),
        plot.title(),
        AnchorH.C,
        AnchorV.C,
        Configuration.Text.Use.TITLE,
        Configuration.Text.Direction.H,
        c.colors().titleColor());
    // draw legend
    drawLegend(g, l.legend(), dataColors);
    for (int px = 0; px < axesGrid.w(); px = px + 1) {
      for (int py = 0; py < axesGrid.h(); py = py + 1) {
        if (px == 0 && c.plotMatrix().axesShow().equals(Configuration.PlotMatrix.Show.BORDER)) {
          // draw common y-axis
          drawYAxis(g, l.commonYAxis(py), plot.yName(), axesGrid.get(0, py));
        }
        if (px == axesGrid.w() - 1
            && c.plotMatrix().titlesShow().equals(Configuration.PlotMatrix.Show.BORDER)) {
          // draw common row title
          drawString(
              g,
              center(l.commonRowTitle(py)),
              plot.dataSeries().rowIndexes().get(py),
              AnchorH.C,
              AnchorV.C,
              Configuration.Text.Use.AXIS_LABEL,
              Configuration.Text.Direction.V,
              c.colors().titleColor());
        }
        if (py == axesGrid.h() - 1 && c.plotMatrix().axesShow().equals(Configuration.PlotMatrix.Show.BORDER)) {
          // draw common x-axis
          drawXAxis(g, l.commonXAxis(px), plot.xName(), axesGrid.get(px, 0));
        }
        if (py == 0 && c.plotMatrix().titlesShow().equals(Configuration.PlotMatrix.Show.BORDER)) {
          // draw common col title
          drawString(
              g,
              center(l.commonColTitle(px)),
              plot.dataSeries().colIndexes().get(px),
              AnchorH.C,
              AnchorV.C,
              Configuration.Text.Use.AXIS_LABEL,
              Configuration.Text.Direction.H,
              c.colors().titleColor());
        }
        // draw plot titles
        if (c.plotMatrix().titlesShow().equals(Configuration.PlotMatrix.Show.ALL)) {
          drawString(
              g,
              center(l.colTitle(px, py)),
              plot.dataSeries().colIndexes().get(px),
              AnchorH.C,
              AnchorV.C,
              Configuration.Text.Use.AXIS_LABEL,
              Configuration.Text.Direction.H,
              c.colors().titleColor());
          drawString(
              g,
              center(l.rowTitle(px, py)),
              plot.dataSeries().rowIndexes().get(py),
              AnchorH.C,
              AnchorV.C,
              Configuration.Text.Use.AXIS_LABEL,
              Configuration.Text.Direction.V,
              c.colors().titleColor());
        }
        // draw axes
        if (c.plotMatrix().axesShow().equals(Configuration.PlotMatrix.Show.ALL)) {
          drawXAxis(g, l.xAxis(px, py), plot.xName(), axesGrid.get(px, py));
          drawYAxis(g, l.yAxis(px, py), plot.yName(), axesGrid.get(px, py));
        }
        // draw lines
        drawLinePlot(g, l.innerPlot(px, py), dataGrid.get(px, py), axesGrid.get(px, py), dataColors);
      }
    }
    // return
    g.dispose();
    return img;
  }

  private DoubleRange plotRange(
      boolean isXAxis,
      DoubleRange originalRange,
      DoubleRange colLargestRange,
      DoubleRange rowLargestRange,
      DoubleRange allLargestRange) {
    if (c.plotMatrix().independences().contains(Configuration.PlotMatrix.Independence.ALL)) {
      return originalRange;
    }
    if (isXAxis && c.plotMatrix().independences().contains(Configuration.PlotMatrix.Independence.COLS)) {
      return colLargestRange;
    }
    if (!isXAxis && c.plotMatrix().independences().contains(Configuration.PlotMatrix.Independence.ROWS)) {
      return rowLargestRange;
    }
    return allLargestRange;
  }
}
