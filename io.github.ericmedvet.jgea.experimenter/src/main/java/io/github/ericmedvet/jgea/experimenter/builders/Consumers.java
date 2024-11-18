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
import io.github.ericmedvet.jgea.core.util.Naming;
import io.github.ericmedvet.jgea.experimenter.Experiment;
import io.github.ericmedvet.jgea.experimenter.Run;
import io.github.ericmedvet.jgea.experimenter.Utils;
import io.github.ericmedvet.jgea.experimenter.listener.telegram.TelegramClient;
import io.github.ericmedvet.jnb.core.*;
import io.github.ericmedvet.jnb.datastructure.NamedFunction;
import io.github.ericmedvet.jnb.datastructure.TriConsumer;
import io.github.ericmedvet.jviz.core.drawer.Video;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.function.Function;
import javax.imageio.ImageIO;

@Discoverable(prefixTemplate = "ea.consumer|c")
public class Consumers {

  private Consumers() {
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static <X, Y, O> TriConsumer<X, Run<?, ?, ?, ?>, Experiment> composed(
      @Param(value = "of", dNPM = "f.identity()") Function<X, Y> f,
      @Param(value = "f", dNPM = "f.identity()") Function<Y, O> innerF,
      @Param(value = "c") TriConsumer<O, Run<?, ?, ?, ?>, Experiment> consumer
  ) {
    return Naming.named(
        "%s[f=%s]".formatted(consumer, NamedFunction.name(f)),
        (x, run, experiment) -> consumer.accept(innerF.apply(f.apply(x)), run, experiment)
    );
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static TriConsumer<?, ?, ?> deaf() {
    return Naming.named("deaf", (i1, i2, i3) -> {});
  }

  private static void save(Object o, String filePath, boolean overwrite) {
    File file = null;
    try {
      switch (o) {
        case BufferedImage image -> {
          file = Misc.robustGetFile(filePath + ".png", overwrite);
          ImageIO.write(image, "png", file);
        }
        case String s -> {
          file = Misc.robustGetFile(filePath + ".txt", overwrite);
          Files.writeString(file.toPath(), s, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
        }
        case Video video -> {
          file = Misc.robustGetFile(filePath + ".mp4", overwrite);
          Files.write(file.toPath(), video.data(), StandardOpenOption.WRITE, StandardOpenOption.CREATE);
        }
        case byte[] data -> {
          file = Misc.robustGetFile(filePath + ".bin", overwrite);
          try (OutputStream os = new FileOutputStream(file)) {
            os.write(data);
          }
        }
        case NamedParamMap npm -> {
          file = Misc.robustGetFile(filePath + ".txt", overwrite);
          Files.writeString(
              file.toPath(),
              MapNamedParamMap.prettyToString(npm),
              StandardOpenOption.WRITE,
              StandardOpenOption.CREATE
          );
        }
        case null -> throw new IllegalArgumentException("Cannot save null data of type %s");
        default -> throw new IllegalArgumentException(
            "Cannot save data of type %s".formatted(o.getClass().getSimpleName())
        );
      }
    } catch (IOException e) {
      throw new RuntimeException(
          "Cannot save '%s'".formatted(Objects.isNull(file) ? filePath : file.getPath()),
          e
      );
    }
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static <X, O> TriConsumer<X, Run<?, ?, ?, ?>, Experiment> saver(
      @Param(value = "of", dNPM = "f.identity()") Function<X, O> f,
      @Param(value = "overwrite") boolean overwrite,
      @Param(value = "path", dS = "run-{run.index:%04d}") String filePathTemplate
  ) {
    return Naming.named(
        "saver[%s]".formatted(NamedFunction.name(f)),
        (x, run, experiment) -> save(f.apply(x), Utils.interpolate(filePathTemplate, experiment, run), overwrite)
    );
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static <X, O> TriConsumer<X, Run<?, ?, ?, ?>, Experiment> telegram(
      @Param(value = "of", dNPM = "f.identity()") Function<X, O> f,
      @Param(
          value = "title", dS = // spotless:off
              """
                  Experiment:
                  \t{name}
                  Run {run.index}:
                  \tSolver: {run.solver\
                  .name}
                  \tProblem: {run.problem.name}
                  \tSeed: {run.randomGenerator.seed}""" // spotless:on
      ) String titleTemplate,
      @Param("chatId") String chatId,
      @Param("botIdFilePath") String botIdFilePath
  ) {
    TelegramClient client = new TelegramClient(new File(botIdFilePath), Long.parseLong(chatId));
    return Naming.named(
        "telegram[%s→to:%s]".formatted(NamedFunction.name(f), chatId),
        (x, run, experiment) -> client.send(Utils.interpolate(titleTemplate, experiment, run), f.apply(x))
    );
  }
}
