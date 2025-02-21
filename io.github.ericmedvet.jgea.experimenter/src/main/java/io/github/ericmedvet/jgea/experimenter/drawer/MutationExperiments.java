/*-
 * ========================LICENSE_START=================================
 * jgea-experimenter
 * %%
 * Copyright (C) 2018 - 2025 Eric Medvet
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

import io.github.ericmedvet.jgea.core.operator.Mutation;
import io.github.ericmedvet.jgea.core.representation.programsynthesis.ProgramExecutionException;
import io.github.ericmedvet.jgea.core.representation.programsynthesis.ttpn.*;
import io.github.ericmedvet.jgea.core.representation.programsynthesis.type.Base;
import io.github.ericmedvet.jgea.core.representation.programsynthesis.type.Composed;
import io.github.ericmedvet.jgea.core.representation.programsynthesis.type.Generic;
import io.github.ericmedvet.jgea.core.representation.programsynthesis.type.TypeException;
import io.github.ericmedvet.jgea.core.representation.tree.numeric.Element;
import io.github.ericmedvet.jgea.problem.programsynthesis.ProgramSynthesisProblem;
import io.github.ericmedvet.jnb.core.NamedBuilder;
import java.util.*;
import java.util.random.RandomGenerator;

public class MutationExperiments {

  public static void main(
      String[] args
  ) throws NetworkStructureException, ProgramExecutionException, NoSuchMethodException, TypeException {

    Network rIntSumgoodNetwork = new Network(
        List.of(
            Gate.input(Base.REAL),
            Gate.input(Base.REAL),
            Gates.rPMathOperator(Element.Operator.ADDITION),
            Gates.rToI(),
            Gate.output(Base.INT)
        ),
        Set.of(
            Wire.of(0, 0, 2, 0),
            Wire.of(1, 0, 2, 1),
            Wire.of(2, 0, 3, 0),
            Wire.of(3, 0, 4, 0)

        )
    );

    Network biLongestStringgoodNetwork = new Network(
        List.of(
            Gate.input(Base.STRING),
            Gate.input(Base.STRING),
            Gates.sSplitter(),
            Gates.sSplitter(),
            Gates.length(),
            Gates.length(),
            Gates.iBefore(),
            Gates.select(),
            Gate.output(Generic.of("t"))
        ),
        Set.of(
            Wire.of(0, 0, 2, 0),
            Wire.of(1, 0, 3, 0),
            Wire.of(2, 0, 4, 0),
            Wire.of(3, 0, 5, 0),
            Wire.of(4, 0, 6, 0),
            Wire.of(5, 0, 6, 1),
            Wire.of(0, 0, 7, 1),
            Wire.of(1, 0, 7, 0),
            Wire.of(6, 0, 7, 2),
            Wire.of(7, 0, 8, 0)
        )
    );

    Network iArraySumgoodNetwork = new Network(
        List.of(
            Gate.input(Composed.sequence(Base.INT)),
            Gates.splitter(),
            Gates.iSPSum(),
            Gate.output(Base.INT)
        ),
        Set.of(
            Wire.of(0, 0, 1, 0),
            Wire.of(1, 0, 2, 0),
            Wire.of(2, 0, 3, 0),
            Wire.of(2, 0, 2, 1)
        )
    );

    Network iBiMaxgoodNetwork = new Network(
        List.of(
            Gate.input(Base.INT),
            Gate.input(Base.INT),
            Gate.output(Base.INT),
            Gates.iBefore(),
            Gates.select()
        ),
        Set.of(
            Wire.of(0, 0, 3, 0),
            Wire.of(1, 0, 3, 1),
            Wire.of(0, 0, 4, 1),
            Wire.of(1, 0, 4, 0),
            Wire.of(3, 0, 4, 2),
            Wire.of(4, 0, 2, 0)
        )
    );

    Network iTriMaxgoodNetwork = new Network(
        List.of(
            Gate.input(Base.INT),
            Gate.input(Base.INT),
            Gate.input(Base.INT),
            Gates.iBefore(),
            Gates.select(),
            Gates.iBefore(),
            Gates.select(),
            Gate.output(Base.INT)
        ),
        Set.of(
            Wire.of(0, 0, 3, 0),
            Wire.of(1, 0, 3, 1),
            Wire.of(0, 0, 4, 1),
            Wire.of(1, 0, 4, 0),
            Wire.of(3, 0, 4, 2),

            Wire.of(4, 0, 5, 0),
            Wire.of(2, 0, 5, 1),
            Wire.of(4, 0, 6, 1),
            Wire.of(2, 0, 6, 0),
            Wire.of(5, 0, 6, 2),
            Wire.of(6, 0, 7, 0)
        )
    );

    Network vScProductgoodNetwork = new Network(
        List.of(
            Gate.input(Composed.sequence(Base.REAL)),
            Gate.input(Base.REAL),
            Gates.length(),
            Gates.repeater(),
            Gates.splitter(),
            Gates.rPMathOperator(Element.Operator.MULTIPLICATION),
            Gates.sPSequencer(),
            Gate.output(Composed.sequence(Base.REAL))
        ),
        Set.of(
            Wire.of(1, 0, 3, 0),
            Wire.of(0, 0, 2, 0),
            Wire.of(0, 0, 4, 0),
            Wire.of(2, 0, 3, 1),
            Wire.of(3, 0, 5, 0),
            Wire.of(4, 0, 5, 1),
            Wire.of(5, 0, 6, 0),
            Wire.of(6, 0, 6, 1),
            Wire.of(6, 0, 7, 0)
        )
    );

    Network sLengthergoodNetwork = new Network(
        List.of(
            Gate.input(Composed.sequence(Base.STRING)),
            Gates.splitter(),
            Gates.sSplitter(),
            Gates.length(),
            Gates.pairer(),
            Gates.sPSequencer(),
            Gate.output(Composed.sequence(Composed.tuple(List.of(Base.STRING, Base.INT))))
        ),
        Set.of(
            Wire.of(0, 0, 1, 0),
            Wire.of(1, 0, 2, 0),
            Wire.of(1, 0, 4, 0),
            Wire.of(2, 0, 3, 0),
            Wire.of(3, 0, 4, 1),
            Wire.of(4, 0, 5, 0),
            Wire.of(5, 0, 5, 1),
            Wire.of(5, 0, 6, 0)

        )
    );

    Network triLongestStringgoodNetwork = new Network(
        List.of(
            Gate.input(Base.STRING),
            Gate.input(Base.STRING),
            Gate.input(Base.STRING),
            Gates.sSplitter(),
            Gates.sSplitter(),
            Gates.sSplitter(),
            Gates.length(),
            Gates.length(),
            Gates.length(),
            Gates.iBefore(),
            Gates.select(),
            Gates.sSplitter(),
            Gates.length(),
            Gates.iBefore(),
            Gates.select(),
            Gate.output(Generic.of("t"))
        ),
        Set.of(
            Wire.of(0, 0, 3, 0),
            Wire.of(0, 0, 10, 1),
            Wire.of(1, 0, 4, 0),
            Wire.of(1, 0, 10, 0),
            Wire.of(3, 0, 6, 0),
            Wire.of(4, 0, 7, 0),
            Wire.of(6, 0, 9, 0),
            Wire.of(7, 0, 9, 1),
            Wire.of(9, 0, 10, 2),
            Wire.of(10, 0, 11, 0),
            Wire.of(10, 0, 14, 1),
            Wire.of(2, 0, 5, 0),
            Wire.of(5, 0, 8, 0),
            Wire.of(8, 0, 13, 1),
            Wire.of(2, 0, 14, 0),
            Wire.of(11, 0, 12, 0),
            Wire.of(12, 0, 13, 0),
            Wire.of(13, 0, 14, 2),
            Wire.of(14, 0, 15, 0)
        )
    );

    Network vProductgoodNetwork = new Network(
        List.of(
            Gate.input(Composed.sequence(Base.REAL)),
            Gate.input(Composed.sequence(Base.REAL)),
            Gates.splitter(),
            Gates.splitter(),
            Gates.queuer(),
            Gates.rSMult(),
            Gates.rSPSum(),
            Gate.output(Base.REAL)
        ),
        Set.of(
            Wire.of(0, 0, 2, 0),
            Wire.of(1, 0, 3, 0),
            Wire.of(2, 0, 4, 0),
            Wire.of(3, 0, 4, 1),
            Wire.of(4, 0, 5, 0),
            Wire.of(5, 0, 6, 0),
            Wire.of(6, 0, 6, 1),
            Wire.of(6, 0, 7, 0)


        )
    );


    NamedBuilder<?> nb = NamedBuilder.fromDiscovery();
    ProgramSynthesisProblem rIntSumpsb = (ProgramSynthesisProblem) nb.build(
        "ea.p.ps.synthetic(name = \"rIntSum\"; metrics = [fail_rate; avg_raw_dissimilarity; exception_error_rate; profile_avg_steps; profile_avg_tot_size])"
    );
    ProgramSynthesisProblem biLongestStringpsb = (ProgramSynthesisProblem) nb.build(
        "ea.p.ps.synthetic(name = \"biLongestString\"; metrics = [fail_rate; avg_raw_dissimilarity; exception_error_rate; profile_avg_steps; profile_avg_tot_size])"
    );
    ProgramSynthesisProblem iArraySumpsb = (ProgramSynthesisProblem) nb.build(
        "ea.p.ps.synthetic(name = \"iArraySum\"; metrics = [fail_rate; avg_raw_dissimilarity; exception_error_rate; profile_avg_steps; profile_avg_tot_size])"
    );
    ProgramSynthesisProblem iBiMaxpsb = (ProgramSynthesisProblem) nb.build(
        "ea.p.ps.synthetic(name = \"iBiMax\"; metrics = [fail_rate; avg_raw_dissimilarity; exception_error_rate; profile_avg_steps; profile_avg_tot_size])"
    );
    ProgramSynthesisProblem iTriMaxpsb = (ProgramSynthesisProblem) nb.build(
        "ea.p.ps.synthetic(name = \"iTriMax\"; metrics = [fail_rate; avg_raw_dissimilarity; exception_error_rate; profile_avg_steps; profile_avg_tot_size])"
    );
    ProgramSynthesisProblem vScProductpsb = (ProgramSynthesisProblem) nb.build(
        "ea.p.ps.synthetic(name = \"vScProduct\"; metrics = [fail_rate; avg_raw_dissimilarity; exception_error_rate; profile_avg_steps; profile_avg_tot_size])"
    );
    ProgramSynthesisProblem sLengtherpsb = (ProgramSynthesisProblem) nb.build(
        "ea.p.ps.synthetic(name = \"sLengther\"; metrics = [fail_rate; avg_raw_dissimilarity; exception_error_rate; profile_avg_steps; profile_avg_tot_size])"
    );
    ProgramSynthesisProblem triLongestStringpsb = (ProgramSynthesisProblem) nb.build(
        "ea.p.ps.synthetic(name = \"triLongestString\"; metrics = [fail_rate; avg_raw_dissimilarity; exception_error_rate; profile_avg_steps; profile_avg_tot_size])"
    );
    ProgramSynthesisProblem vProductpsb = (ProgramSynthesisProblem) nb.build(
        "ea.p.ps.synthetic(name = \"vProduct\"; metrics = [fail_rate; avg_raw_dissimilarity; exception_error_rate; profile_avg_steps; profile_avg_tot_size])"
    );


    TTPNDrawer drawer = new TTPNDrawer(TTPNDrawer.Configuration.DEFAULT);


    Runner runner = new Runner(100, 100, 100, 100, false);


    //        rIntSumpsb.caseProvider()
    //                .stream()
    //                .forEach(
    //                        e -> System.out.printf(
    //                                "in=%s\tactualOut=%s\tpredOut=%s\terror=%s\tsteps=%d%n",
    //                                e.input(),
    //                                e.output().outputs(),
    //                                runner.run(rIntSumgoodNetwork, e.input()).outputs(),
    //                                rIntSumpsb.errorFunction().apply(e.input(), e.output(), runner.run(rIntSumgoodNetwork, e.input())),
    //                                runner.run(rIntSumgoodNetwork, e.input()).profile().states().size()
    //                        ));

    RandomGenerator rnd = new Random(3);
    Mutation<Network> giMutation = new GateInserterMutation(new LinkedHashSet<>(StatsMain.ALL_GATES), 10, 10, true);
    Mutation<Network> grMutation = new GateRemoverMutation(10, true);
    Mutation<Network> wsMutation = new WireSwapperMutation(10, true);


    drawer.show(rIntSumgoodNetwork);
    drawer.show(biLongestStringgoodNetwork);
    drawer.show(iArraySumgoodNetwork);
    drawer.show(iBiMaxgoodNetwork);
    drawer.show(iTriMaxgoodNetwork);
    drawer.show(vScProductgoodNetwork);
    drawer.show(sLengthergoodNetwork);
    drawer.show(triLongestStringgoodNetwork);
    drawer.show(vProductgoodNetwork);


    //        System.out.println(rIntSumpsb.qualityFunction().apply(runner.asInstrumentedProgram(rIntSumgoodNetwork)));
    //        System.out.println(biLongestStringpsb.qualityFunction().apply(runner.asInstrumentedProgram(biLongestStringgoodNetwork)));
    //        System.out.println(iArraySumpsb.qualityFunction().apply(runner.asInstrumentedProgram(iArraySumgoodNetwork)));
    //        System.out.println(iBiMaxpsb.qualityFunction().apply(runner.asInstrumentedProgram(iBiMaxgoodNetwork)));
    //        System.out.println(iTriMaxpsb.qualityFunction().apply(runner.asInstrumentedProgram(iTriMaxgoodNetwork)));
    //        System.out.println(vScProductpsb.qualityFunction().apply(runner.asInstrumentedProgram(vScProductgoodNetwork)));
    //        System.out.println(sLengtherpsb.qualityFunction().apply(runner.asInstrumentedProgram(sLengthergoodNetwork)));
    //        System.out.println(triLongestStringpsb.qualityFunction().apply(runner.asInstrumentedProgram(triLongestStringgoodNetwork)));
    //        System.out.println(vProductpsb.qualityFunction().apply(runner.asInstrumentedProgram(vProductgoodNetwork)));


    Map<String, Double> rIntSumqualityMetrics = rIntSumpsb.qualityFunction()
        .apply(runner.asInstrumentedProgram(rIntSumgoodNetwork));
    Map<String, Double> biLongestStringqualityMetrics = biLongestStringpsb.qualityFunction()
        .apply(runner.asInstrumentedProgram(biLongestStringgoodNetwork));
    Map<String, Double> iArraySumqualityMetrics = iArraySumpsb.qualityFunction()
        .apply(runner.asInstrumentedProgram(iArraySumgoodNetwork));
    Map<String, Double> iBiMaxqualityMetrics = iBiMaxpsb.qualityFunction()
        .apply(runner.asInstrumentedProgram(iBiMaxgoodNetwork));
    Map<String, Double> vScProductqualityMetrics = vScProductpsb.qualityFunction()
        .apply(runner.asInstrumentedProgram(vScProductgoodNetwork));
    Map<String, Double> sLengtherqualityMetrics = sLengtherpsb.qualityFunction()
        .apply(runner.asInstrumentedProgram(sLengthergoodNetwork));
    Map<String, Double> triLongestqualityMetrics = triLongestStringpsb.qualityFunction()
        .apply(runner.asInstrumentedProgram(triLongestStringgoodNetwork));
    Map<String, Double> vProductqualityMetrics = vProductpsb.qualityFunction()
        .apply(runner.asInstrumentedProgram(vProductgoodNetwork));


    System.out.println("\t\t\t | Error Rate | \t | Average Distance | \t | Average Steps | ");
    System.out.printf(
        "rIntSum \t\t %s \t\t\t\t %s \t\t\t\t\t %s %n",
        rIntSumqualityMetrics.get("fail_rate"),
        rIntSumqualityMetrics.get("avg_raw_dissimilarity"),
        rIntSumqualityMetrics.get("profile_avg_steps")
    );
    System.out.printf(
        "biLongestString  %s \t\t\t\t %s \t\t\t\t\t %s %n",
        biLongestStringqualityMetrics.get("fail_rate"),
        biLongestStringqualityMetrics.get("avg_raw_dissimilarity"),
        biLongestStringqualityMetrics.get("profile_avg_steps")
    );
    System.out.printf(
        "iArraySum \t\t %s \t\t\t\t %s \t\t\t\t\t %s %n",
        iArraySumqualityMetrics.get("fail_rate"),
        iArraySumqualityMetrics.get("avg_raw_dissimilarity"),
        iArraySumqualityMetrics.get("profile_avg_steps")
    );
    System.out.printf(
        "iBiMax \t\t\t %s \t\t\t\t %s \t\t\t\t\t %s %n",
        iBiMaxqualityMetrics.get("fail_rate"),
        iBiMaxqualityMetrics.get("avg_raw_dissimilarity"),
        iBiMaxqualityMetrics.get("profile_avg_steps")
    );
    System.out.printf(
        "vScProduct \t\t %s \t\t\t\t %s \t\t\t\t\t %s %n",
        vScProductqualityMetrics.get("fail_rate"),
        vScProductqualityMetrics.get("avg_raw_dissimilarity"),
        vScProductqualityMetrics.get("profile_avg_steps")
    );
    System.out.printf(
        "sLengther \t\t %s \t\t\t\t %s \t\t\t\t\t %s %n",
        sLengtherqualityMetrics.get("fail_rate"),
        sLengtherqualityMetrics.get("avg_raw_dissimilarity"),
        sLengtherqualityMetrics.get("profile_avg_steps")
    );
    System.out.printf(
        "triLongest \t\t %s \t\t\t\t %s \t\t\t\t\t %s %n",
        triLongestqualityMetrics.get("fail_rate"),
        triLongestqualityMetrics.get("avg_raw_dissimilarity"),
        triLongestqualityMetrics.get("profile_avg_steps")
    );
    System.out.printf(
        "vProduct \t\t %s \t\t\t\t %s  %s %n",
        vProductqualityMetrics.get("fail_rate"),
        vProductqualityMetrics.get("avg_raw_dissimilarity"),
        vProductqualityMetrics.get("profile_avg_steps")
    );


  }
}
