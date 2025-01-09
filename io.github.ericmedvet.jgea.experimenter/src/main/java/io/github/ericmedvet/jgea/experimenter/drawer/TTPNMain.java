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

import io.github.ericmedvet.jgea.core.representation.programsynthesis.InstrumentedProgram;
import io.github.ericmedvet.jgea.core.representation.programsynthesis.Program;
import io.github.ericmedvet.jgea.core.representation.programsynthesis.ProgramExecutionException;
import io.github.ericmedvet.jgea.core.representation.programsynthesis.ttpn.*;
import io.github.ericmedvet.jgea.core.representation.programsynthesis.type.Base;
import io.github.ericmedvet.jgea.core.representation.programsynthesis.type.Composed;
import io.github.ericmedvet.jgea.core.representation.programsynthesis.type.StringParser;
import io.github.ericmedvet.jgea.core.representation.programsynthesis.type.TypeException;
import io.github.ericmedvet.jgea.core.representation.tree.numeric.Element;
import io.github.ericmedvet.jgea.core.util.IntRange;
import io.github.ericmedvet.jgea.problem.programsynthesis.DataFactory;
import io.github.ericmedvet.jgea.problem.programsynthesis.Problems;
import io.github.ericmedvet.jgea.problem.programsynthesis.ProgramSynthesisProblem;
import io.github.ericmedvet.jgea.problem.programsynthesis.synthetic.PrecomputedSyntheticPSProblem;
import io.github.ericmedvet.jnb.datastructure.DoubleRange;
import io.github.ericmedvet.jnb.datastructure.FormattedNamedFunction;
import java.util.*;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TTPNMain {
  private static List<Gate> allGates() {
    return List.of(
        Gates.rPMathOperator(Element.Operator.MULTIPLICATION),
        Gates.rPMathOperator(Element.Operator.ADDITION),
        Gates.rPMathOperator(Element.Operator.SUBTRACTION),
        Gates.rPMathOperator(Element.Operator.DIVISION),
        Gates.iPMathOperator(Element.Operator.MULTIPLICATION),
        Gates.iPMathOperator(Element.Operator.ADDITION),
        Gates.iPMathOperator(Element.Operator.SUBTRACTION),
        Gates.iPMathOperator(Element.Operator.DIVISION),
        Gates.rSPSum(),
        Gates.rSPMult(),
        Gates.rSSum(),
        Gates.rSMult(),
        Gates.iSPSum(),
        Gates.iSPMult(),
        Gates.iSSum(),
        Gates.iSMult(),
        Gates.splitter(),
        Gates.sSplitter(),
        Gates.pairer(),
        Gates.unpairer(),
        Gates.noop(),
        Gates.length(),
        Gates.iTh(),
        Gates.sequencer(),
        Gates.rToI(),
        Gates.iToR(),
        Gates.sink(),
        Gates.queuer(),
        Gates.equal(),
        Gates.iBefore(),
        Gates.rBefore(),
        Gates.sBefore(),
        Gates.select(),
        Gates.bOr(),
        Gates.bAnd(),
        Gates.bXor(),
        Gates.repeater(),
        Gates.iRange()
    );
  }

  private static void doComputationStuff() throws NoSuchMethodException, ProgramExecutionException, NetworkStructureException, TypeException {
    Network n = new Network(
        List.of(
            Gate.input(Composed.sequence(Base.REAL)),
            Gate.input(Composed.sequence(Base.REAL)),
            Gates.splitter(),
            Gates.splitter(),
            Gates.rPMathOperator(Element.Operator.MULTIPLICATION),
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
    DataFactory df = new DataFactory(
        List.of(1, 2, 3),
        List.of(1d, 2d, 3d, 1.5, 2.5, 3.14),
        List.of("cat", "dog", "Hello World!", "mummy"),
        new IntRange(-10, 100),
        new DoubleRange(-10, 10),
        new IntRange(2, 20),
        new IntRange(3, 8)
    );
    RandomGenerator rnd = new Random(1);
    System.out.println(df.apply(StringParser.parse("[<S,[I],R>]"), rnd));

    Program tProgram = Program.from(Problems.class.getMethod("vProduct", List.class, List.class));
    System.out.println(tProgram);
    InstrumentedProgram ttpnProgram = new Runner(1000, 1000).asInstrumentedProgram(n);
    List<Object> inputs = List.of(List.of(1d, 2d), List.of(3d, 4d));
    System.out.println(tProgram.run(inputs));
    InstrumentedProgram.Outcome o = ttpnProgram.runInstrumented(inputs);
    System.out.println(o);

    ProgramSynthesisProblem psp = new PrecomputedSyntheticPSProblem(
        tProgram,
        List.of(ProgramSynthesisProblem.Metric.FAIL_RATE, ProgramSynthesisProblem.Metric.AVG_RAW_DISSIMILARITY),
        100d,
        df,
        rnd,
        10,
        5,
        0.5
    );

    //System.out.println(psp.qualityFunction().apply(tProgram));
    System.out.println(psp.qualityFunction().apply(ttpnProgram));
    System.out.println(psp.validationQualityFunction().apply(ttpnProgram));

    psp.caseProvider()
        .stream()
        .forEach(
            example -> System.out.printf(
                "\tactual=%s vs. predicted=%s%n",
                example.output().outputs(),
                psp.predictFunction().apply(ttpnProgram, example.input()).outputs()
            )
        );
  }

  private static void doFactoryStuff() throws NetworkStructureException, TypeException {
    Network sn = new Network(
        List.of(
            Gate.input(Composed.sequence(Base.STRING)),
            Gates.splitter(),
            Gate.output(Base.STRING)
        ),
        Set.of(
            Wire.of(0, 0, 1, 0),
            Wire.of(1, 0, 2, 0)
        )
    );
    System.out.println(sn);

    Network n = new Network(
        List.of(
            Gate.input(Composed.sequence(Base.REAL)),
            Gate.input(Composed.sequence(Base.REAL)),
            Gates.splitter(),
            Gates.splitter(),
            Gates.rPMathOperator(Element.Operator.MULTIPLICATION),
            Gates.rSPSum(),
            Gate.output(Base.REAL)
            //new ones
            /*Gates.rSPSum(),
            Gates.rSPSum(),
            Gates.rSPSum(),
            Gate.output(Base.REAL)*/
        ),
        Set.of(
            Wire.of(0, 0, 2, 0),
            Wire.of(1, 0, 3, 0),
            Wire.of(2, 0, 4, 0),
            Wire.of(3, 0, 4, 1),
            Wire.of(4, 0, 5, 0),
            Wire.of(5, 0, 5, 1),
            Wire.of(5, 0, 6, 0)
            //new ones
            /*Wire.of(2, 0, 7, 0),
            Wire.of(9, 0, 7, 1),
            Wire.of(7, 0, 8, 0),
            Wire.of(9, 0, 8, 1),
            Wire.of(8, 0, 9, 0),
            Wire.of(8, 0, 9, 1),
            Wire.of(9, 0, 10, 0)*/
        )
    );
    System.out.println("===\n" + n);

    Network pn = new Network(
        List.of(
            Gates.rPMathOperator(Element.Operator.MULTIPLICATION),
            Gates.rPMathOperator(Element.Operator.ADDITION)
        ),
        Set.of(
            Wire.of(0, 0, 1, 1),
            Wire.of(1, 0, 0, 1)
        )
    );
    System.out.println("===\n" + pn);
    Network mn = n.mergedWith(pn).wireFreeInputEndPoints(ts -> 0).wireFreeOutputEndPoints(ts -> 0);
    System.out.println("===\n" + mn);

    RandomGenerator rnd = new Random();
    TTPNDrawer drawer = new TTPNDrawer(TTPNDrawer.Configuration.DEFAULT);
    //drawer.show(n);
    //drawer.show(pn);
    //drawer.show(mn);
    //drawer.show(new ImageBuilder.ImageInfo(600, 300), n);

    NetworkFactory nf = new NetworkFactory(
        List.of(Composed.sequence(Base.REAL), Composed.sequence(Base.REAL)),
        List.of(Base.REAL),
        new LinkedHashSet<>(allGates()),
        20,
        0
    );
    Network newN = nf.build(rnd);
    //drawer.show(newN);
    //drawer.show(NetworkUtils.randomSubnetwork(newN, rnd, newN.gates().size() / 4));

    for (int i = 0; i < 1000; i++) {
      Network nn = nf.build(rnd);
      Network snn = NetworkUtils.randomSubnetwork(newN, rnd, nn.gates().size() / 2);
      Network hnn = NetworkUtils.randomHoledNetwork(newN, rnd, nn.gates().size() / 4);
      System.out.printf(
          "n.g:%3d n.w:%3d\tsn.g:%3d sn.w:%3d\thn.g:%3d hn.w:%3d\tsubnets:%2d%n",
          nn.gates().size(),
          nn.wires().size(),
          snn.gates().size(),
          snn.wires().size(),
          hnn.gates().size(),
          hnn.wires().size(),
          hnn.disjointSubnetworks().size()
      );
    }

  }

  private static void factory() {
    RandomGenerator rnd = new Random();
    NetworkFactory factory = new NetworkFactory(
        List.of(Composed.sequence(Base.REAL), Composed.sequence(Base.REAL)),
        List.of(Base.REAL),
        new LinkedHashSet<>(allGates()),
        32,
        0
    );
    TTPNDrawer drawer = new TTPNDrawer(TTPNDrawer.Configuration.DEFAULT);
    factory.build(rnd, drawer::show);
    IntStream.range(0, 1000).forEach(i -> factory.build(rnd, n -> System.out.printf("======%n%s%n===%n", n)));
  }


  private static void factoryStats() throws ProgramExecutionException {
    RandomGenerator rnd = new Random(1);
    NetworkFactory factory = new NetworkFactory(
        List.of(Composed.sequence(Base.REAL), Composed.sequence(Base.REAL)),
        List.of(Base.REAL),
        new LinkedHashSet<>(allGates()),
        64,
        0
    );
    Runner runner = new Runner(100, 1000);
    List<List<Object>> cases = List.of(
        List.of(List.of(1d, 2d), List.of(3d, 4d)),
        List.of(List.of(1d), List.of(3d, 4d)),
        List.of(List.of(1d, 2d), List.of(3d)),
        List.of(List.of(1d), List.of(3d))
    );
    List<FormattedNamedFunction<Network, Double>> fs = List.of(
        FormattedNamedFunction.from(n -> (double) n.size(), "%5.1f", "size"),
        FormattedNamedFunction.from(n -> (double) n.gates().size(), "%4.1f", "n.gates"),
        FormattedNamedFunction.from(
            n -> {
              try {
                return (double) (n.disjointSubnetworks().size());
              } catch (NetworkStructureException | TypeException e) {
                return Double.NaN;
              }
            },
            "%4.1f",
            "n.subnetworks"
        ),
        FormattedNamedFunction.from(
            n -> (double) n.inputGates().keySet().stream().filter(n::isWiredToOutput).count(),
            "%3.1f",
            "n.outputWiredInputs"
        ),
        FormattedNamedFunction.from(
            n -> (double) n.outputGates().keySet().stream().filter(n::isWiredToInput).count(),
            "%3.1f",
            "n.inputWiredOutputs"
        ),
        FormattedNamedFunction.from(
            n -> (double) n.outputGates()
                .keySet()
                .stream()
                .filter(n::isGateAutoBlocked)
                .count(),
            "%3.1f",
            "n.blockedOutputs"
        ),
        FormattedNamedFunction.from(
            n -> (double) n.outputGates()
                .keySet()
                .stream()
                .filter(gi -> !n.isGateAutoBlocked(gi) && n.isWiredToInput(gi))
                .count(),
            "%3.1f",
            "n.unblockedAndIWiredOutputs"
        ),
        FormattedNamedFunction.from(
            n -> (double) n.outputGates()
                .keySet()
                .stream()
                .filter(gi -> !n.isGateAutoBlocked(gi) && n.isWiredToInput(gi))
                .count() / (double) n.outputTypes().size(),
            "%6.4f",
            "rate.unblockedOutputs"
        ),
        FormattedNamedFunction.from(
            n -> cases.stream()
                .mapToDouble(c -> runner.asInstrumentedProgram(n).safelyRun(c) == null ? 1d : 0d)
                .average()
                .orElseThrow(),
            "%6.4f",
            "rate.cases.null"
        )
    );
    List<Network> ns = factory.build(10, rnd);
    new TTPNDrawer(TTPNDrawer.Configuration.DEFAULT).show(ns.get(2));
    new TTPNDrawer(TTPNDrawer.Configuration.DEFAULT).show(ns.get(8));
    new TTPNDrawer(TTPNDrawer.Configuration.DEFAULT).show(ns.get(9));

    List<Map<String, Double>> maps = ns.stream()
        .map(
            n -> fs.stream()
                .collect(Collectors.toMap(f -> f.name(), f -> f.apply(n)))
        )
        .toList();
    System.out.println(fs.stream().map(f -> f.name()).collect(Collectors.joining("\t")));
    IntStream.range(0, maps.size())
        .forEach(
            i -> System.out.printf(
                "%3d : %s%n",
                i,
                fs.stream()
                    .map(f -> f.format().formatted(maps.get(i).get(f.name())))
                    .collect(Collectors.joining(" "))
            )
        );
    System.out.printf(
        "SUMMARY:%n    : %s%n",
        fs.stream()
            .map(f -> f.format().formatted(maps.stream().mapToDouble(m -> m.get(f.name())).average().orElseThrow()))
            .collect(
                Collectors.joining(" ")
            )
    );
  }

  private static void loopedNet() throws NetworkStructureException, TypeException {
    Network n = new Network(
        List.of(
            Gate.input(Base.INT),
            Gates.pairer()
        ),
        Set.of(
          //Wire.of(0,0,1,0),
          //Wire.of(1,0,1,1)
        )
    );
    TTPNDrawer drawer = new TTPNDrawer(TTPNDrawer.Configuration.DEFAULT);
    drawer.show(n);
    drawer.show(n.wireFreeInputEndPoints(ts -> 0));
  }

  public static void main(
      String[] args
  ) throws NetworkStructureException, ProgramExecutionException, NoSuchMethodException, TypeException {
    //weirdOne();
    //factory();
    //doComputationStuff();
    factoryStats();
  }

  private static void weirdOne() throws NetworkStructureException, TypeException {
    Network n = new Network(
        List.of(
            Gate.input(Composed.sequence(Base.REAL)),
            Gates.select(),
            Gates.length()
        ),
        Set.of(
            Wire.of(0, 0, 1, 1),
            Wire.of(1, 0, 2, 0)
        )
    );
    new TTPNDrawer(TTPNDrawer.Configuration.DEFAULT).show(n);
  }

}
