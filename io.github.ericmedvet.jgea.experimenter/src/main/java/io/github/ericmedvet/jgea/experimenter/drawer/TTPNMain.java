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

import io.github.ericmedvet.jgea.core.fitness.CaseBasedFitness;
import io.github.ericmedvet.jgea.core.representation.programsynthesis.InstrumentedProgram;
import io.github.ericmedvet.jgea.core.representation.programsynthesis.Program;
import io.github.ericmedvet.jgea.core.representation.programsynthesis.ProgramExecutionException;
import io.github.ericmedvet.jgea.core.representation.programsynthesis.ttpn.*;
import io.github.ericmedvet.jgea.core.representation.programsynthesis.type.Base;
import io.github.ericmedvet.jgea.core.representation.programsynthesis.type.Composed;
import io.github.ericmedvet.jgea.core.representation.programsynthesis.type.StringParser;
import io.github.ericmedvet.jgea.core.representation.programsynthesis.type.Typed;
import io.github.ericmedvet.jgea.core.representation.tree.numeric.Element;
import io.github.ericmedvet.jgea.core.util.IntRange;
import io.github.ericmedvet.jgea.problem.programsynthesis.DataFactory;
import io.github.ericmedvet.jgea.problem.programsynthesis.ProgramSynthesisFitness;
import io.github.ericmedvet.jgea.problem.programsynthesis.ProgramSynthesisProblem;
import io.github.ericmedvet.jnb.datastructure.DoubleRange;
import io.github.ericmedvet.jviz.core.drawer.ImageBuilder;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.random.RandomGenerator;
import java.util.stream.IntStream;

public class TTPNMain {
  public static void main(
      String[] args
  ) throws NetworkStructureException, ProgramExecutionException, NoSuchMethodException {
    Network n = new Network(
        List.of(
            Gate.input(Composed.sequence(Base.REAL)),
            Gate.input(Composed.sequence(Base.REAL)),
            Gates.splitter(),
            Gates.splitter(),
            Gates.rPMathOperator(Element.Operator.MULTIPLICATION),
            Gates.rSPSum(),
            Gate.output(Base.REAL),
            //new ones
            Gates.rSPSum(),
            Gates.rSPSum(),
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
            Wire.of(5, 0, 6, 0),
            //new ones
            Wire.of(2, 0, 7, 0),
            Wire.of(9, 0, 7, 1),
            Wire.of(7, 0, 8, 0),
            Wire.of(9, 0, 8, 1),
            Wire.of(8, 0, 9, 0),
            Wire.of(8, 0, 9, 1),
            Wire.of(9, 0, 10, 0)
        )
    );
    System.out.println(n);
    n.validate();

    DataFactory df = new DataFactory(
        List.of(1, 2, 3),
        List.of(1d, 2d, 3d, 1.5, 2.5, 3.14),
        List.of("cat", "dog", "hello world!", "mummy"),
        new IntRange(-10, 100),
        new DoubleRange(-10, 10),
        new IntRange(2, 20),
        new IntRange(3, 8)
    );
    RandomGenerator rnd = new Random(1);
    System.out.println(df.apply(StringParser.parse("[<S,[I],R>]"), rnd));

    Program tProgram = Program.from(TTPNMain.class.getMethod("vProduct", List.class, List.class));
    System.out.println(tProgram);
    InstrumentedProgram ttpnProgram = new Runner(1000, 1000).asInstrumentedProgram(n);
    List<Object> inputs = List.of(List.of(1d, 2d), List.of(3d, 4d));
    System.out.println(tProgram.run(inputs));
    InstrumentedProgram.Outcome o = ttpnProgram.runInstrumented(inputs);
    System.out.println(o);

    ProgramSynthesisProblem psp = new ProgramSynthesisProblem(
        10,
        10,
        0.1,
        df,
        rnd,
        tProgram,
        ProgramSynthesisFitness.Metric.SUCCESS_RATE
    );
    IntFunction<List<Object>> caseProvider = ((CaseBasedFitness<Program, List<Object>, Double, Double>) psp
        .qualityFunction()).caseProvider();
    IntStream.range(0, 10).forEach(i -> System.out.println(caseProvider.apply(i)));

    System.exit(0);

    TTPNDrawer drawer = new TTPNDrawer(TTPNDrawer.Configuration.DEFAULT);
    drawer.show(n);
    drawer.show(new ImageBuilder.ImageInfo(600, 300), n);

  }

  @Typed("R")
  public static Double vProduct(@Typed("[R]") List<Double> v1, @Typed("[R]") List<Double> v2) {
    if (v1.size() != v2.size()) {
      throw new IllegalArgumentException("Input sizes are different: %d and %d".formatted(v1.size(), v2.size()));
    }
    return IntStream.range(0, v1.size()).mapToDouble(i -> v1.get(i) * v2.get(i)).sum();
  }
}
