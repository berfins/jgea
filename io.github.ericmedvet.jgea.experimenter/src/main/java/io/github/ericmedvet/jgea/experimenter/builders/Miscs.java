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
package io.github.ericmedvet.jgea.experimenter.builders;

import io.github.ericmedvet.jgea.core.util.Misc;
import io.github.ericmedvet.jgea.experimenter.drawer.DoubleGridDrawer;
import io.github.ericmedvet.jgea.problem.ca.MultivariateRealGridCellularAutomaton;
import io.github.ericmedvet.jgea.problem.image.ImageUtils;
import io.github.ericmedvet.jnb.core.Cacheable;
import io.github.ericmedvet.jnb.core.Discoverable;
import io.github.ericmedvet.jnb.core.Param;
import io.github.ericmedvet.jnb.datastructure.DoubleRange;
import io.github.ericmedvet.jnb.datastructure.Grid;
import io.github.ericmedvet.jnb.datastructure.Pair;
import io.github.ericmedvet.jsdynsym.control.Simulation;
import io.github.ericmedvet.jsdynsym.control.SimulationOutcomeDrawer;
import io.github.ericmedvet.jviz.core.drawer.Drawer;
import io.github.ericmedvet.jviz.core.drawer.ImageBuilder;
import io.github.ericmedvet.jviz.core.drawer.VideoBuilder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;

@Discoverable(prefixTemplate = "ea.misc")
public class Miscs {

  private Miscs() {
  }

  @SuppressWarnings("unused")
  public static VideoBuilder<MultivariateRealGridCellularAutomaton> caVideo(
      @Param(value = "gray", dB = true) boolean gray,
      @Param(value = "caStateRange", dNPM = "m.range(min=-1;max=1)") DoubleRange caStateRange,
      @Param(value = "nOfSteps", dI = 100) int nOfSteps,
      @Param(value = "sizeRate", dI = 10) int sizeRate,
      @Param(value = "marginRate", dD = 0d) double marginRate,
      @Param(value = "frameRate", dD = 10d) double frameRate,
      @Param(value = "fontSize", dD = 10d) double fontSize
  ) {
    DoubleGridDrawer gDrawer = new DoubleGridDrawer(
        new DoubleGridDrawer.Configuration(
            gray ? DoubleGridDrawer.Configuration.ColorType.GRAY : DoubleGridDrawer.Configuration.ColorType.RGB,
            caStateRange,
            sizeRate,
            marginRate
        )
    );
    Drawer<Pair<Integer, Grid<double[]>>> pDrawer = new Drawer<>() {
      @Override
      public void draw(Graphics2D g, Pair<Integer, Grid<double[]>> p) {
        gDrawer.draw(g, p.second());
        Drawer.stringWriter(Color.PINK, (float) fontSize, Function.identity())
            .draw(g, "k=%3d".formatted(p.first()));
      }

      @Override
      public ImageInfo imageInfo(Pair<Integer, Grid<double[]>> p) {
        return gDrawer.imageInfo(p.second());
      }
    };
    return VideoBuilder.from(pDrawer, Function.identity(), frameRate).on(ca -> {
      List<Grid<double[]>> seq = ca.evolve(nOfSteps);
      return IntStream.range(0, seq.size())
          .mapToObj(i -> new Pair<>(i, seq.get(i)))
          .toList();
    });
  }

  @SuppressWarnings("unused")
  public static Character ch(@Param("s") String s) {
    return s.charAt(0);
  }

  @SuppressWarnings("unused")
  public static Color colorByName(@Param("name") String name) {
    try {
      return (Color) Color.class.getField(name.toUpperCase()).get(null);
    } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("unused")
  public static Color colorByRgb(@Param("r") int r, @Param("g") int g, @Param("b") int b) {
    return new Color(r, g, b);
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static <K, V> Map.Entry<K, V> entry(@Param("key") K key, @Param("value") V value) {
    return Map.entry(key, value);
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static BufferedImage imgByName(
      @Param("name") String name,
      @Param(value = "gateBGColor", dNPM = "ea.misc.colorByName(name = black)") Color bgColor,
      @Param(value = "w", dI = 15) int w,
      @Param(value = "h", dI = 15) int h,
      @Param(value = "marginRate", dD = 0.1) double marginRate
  ) {
    return ImageUtils.imageDrawer(bgColor, marginRate)
        .build(new ImageBuilder.ImageInfo(w, h), ImageUtils.loadFromResource(name));
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static BufferedImage imgFromString(
      @Param("s") String s,
      @Param(value = "borderColor", dNPM = "ea.misc.colorByName(name = white)") Color fgColor,
      @Param(value = "gateBGColor", dNPM = "ea.misc.colorByName(name = black)") Color bgColor,
      @Param(value = "w", dI = 159) int w,
      @Param(value = "h", dI = 15) int h,
      @Param(value = "marginRate", dD = 0.1) double marginRate
  ) {
    return ImageUtils.stringDrawer(fgColor, bgColor, marginRate).build(new ImageBuilder.ImageInfo(w, h), s);
  }

  @SuppressWarnings("unused")
  public static <K, V> Map<K, V> map(@Param("entries") List<Map.Entry<K, V>> entries) {
    Map<K, V> map = new LinkedHashMap<>();
    entries.forEach(e -> map.put(e.getKey(), e.getValue()));
    return Collections.unmodifiableMap(map);
  }

  @SuppressWarnings("unused")
  public static <K, V> Map<K, V> mapFromLists(
      @Param("keys") List<K> keys,
      @Param("values") List<V> values
  ) {
    if (keys.size() != values.size()) {
      throw new IllegalArgumentException(
          "Keys and values size do not match: %d != %d".formatted(
              keys.size(),
              values.size()
          )
      );
    }
    return Collections.unmodifiableSequencedMap(
        IntStream.range(0, keys.size())
            .boxed()
            .collect(
                Misc.toSequencedMap(
                    keys::get,
                    values::get
                )
            )
    );
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static <V> Map.Entry<String, V> sEntry(@Param("key") String key, @Param("value") V value) {
    return Map.entry(key, value);
  }

  @SuppressWarnings("unused")
  public static <V> Map<String, V> sMapFromLists(
      @Param("keys") List<String> keys,
      @Param("values") List<V> values
  ) {
    if (keys.size() != values.size()) {
      throw new IllegalArgumentException(
          "Keys and values size do not match: %d != %d".formatted(
              keys.size(),
              values.size()
          )
      );
    }
    return Collections.unmodifiableSequencedMap(
        IntStream.range(0, keys.size())
            .boxed()
            .collect(
                Misc.toSequencedMap(
                    keys::get,
                    values::get
                )
            )
    );
  }

  @SuppressWarnings("unused")
  public static <S> VideoBuilder<Simulation.Outcome<S>> toVideo(@Param("drawer") SimulationOutcomeDrawer<S> drawer) {
    return drawer.videoBuilder();
  }
}
