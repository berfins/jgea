package io.github.ericmedvet.jgea.experimenter.drawer;

import io.github.ericmedvet.jgea.core.representation.programsynthesis.InstrumentedProgram;
import io.github.ericmedvet.jgea.core.representation.programsynthesis.ProgramExecutionException;
import io.github.ericmedvet.jgea.core.representation.programsynthesis.ttpn.*;
import io.github.ericmedvet.jgea.core.representation.programsynthesis.type.Base;
import io.github.ericmedvet.jgea.core.representation.programsynthesis.type.TypeException;
import io.github.ericmedvet.jgea.core.util.IntRange;
import io.github.ericmedvet.jgea.problem.programsynthesis.ProgramSynthesisProblem;
import io.github.ericmedvet.jgea.problem.programsynthesis.synthetic.PrecomputedSyntheticPSProblem;
import io.github.ericmedvet.jnb.datastructure.DoubleRange;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.random.RandomGenerator;

public class BerfinMain {
    public static void main(String[] args) throws NetworkStructureException, TypeException, ProgramExecutionException {
        // create iBiMax ttpn
        Network target = new Network(
                List.of(
                        Gate.input(Base.INT),
                        Gate.input(Base.INT),
                        Gate.output(Base.INT),
                        Gates.iBefore(),
                        Gates.select()
                ),
                Set.of(
                        Wire.of(0, 0, 3, 0),
                        Wire.of(0, 0, 4, 1),
                        Wire.of(1, 0, 3, 1),
                        Wire.of(1, 0, 4, 0),
                        Wire.of(3, 0, 4, 2),
                        Wire.of(4, 0, 2, 0)
                )
        );
        // plot it
        TTPNDrawer drawer = new TTPNDrawer(TTPNDrawer.Configuration.DEFAULT);
        drawer.show(target);
        System.out.println(target);
        // try it
        Runner runner = new Runner(100, 100, 100, 100, false);
        InstrumentedProgram targetProgram = runner.asInstrumentedProgram(target);
        System.out.println(targetProgram.run(List.of(4, 5)));
        System.out.println(targetProgram.run(List.of(43, 5)));
        // prepare the data factory
        DataFactory df = new DataFactory(
                List.of(1, 2, 3),
                List.of(1d, 2d, 3d, 1.5, 2.5, 3.14),
                List.of("cat", "dog", "Hello World!", "mummy"),
                new IntRange(-10, 100),
                new DoubleRange(-10, 10),
                new IntRange(2, 20),
                new IntRange(3, 8)
        );
        PrecomputedSyntheticPSProblem problem = new PrecomputedSyntheticPSProblem(
                targetProgram,
                List.of(ProgramSynthesisProblem.Metric.AVG_RAW_DISSIMILARITY),
                1000,
                df,
                new Random(1),
                10,
                10,
                0
        );
        System.out.println(problem.caseProvider().all());
        // repeat many tımes
        //   mutate the target
        //   measure the avg_raw_dıss


    }
}
