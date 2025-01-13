/*-
 * ========================LICENSE_START=================================
 * jgea-core
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
package io.github.ericmedvet.jgea.core.representation.programsynthesis.ttpn;

import io.github.ericmedvet.jgea.core.representation.programsynthesis.ProgramExecutionException;
import io.github.ericmedvet.jgea.core.representation.programsynthesis.type.Base;
import io.github.ericmedvet.jgea.core.representation.programsynthesis.type.Composed;
import io.github.ericmedvet.jgea.core.representation.programsynthesis.type.Type;
import io.github.ericmedvet.jgea.core.representation.programsynthesis.type.TypeException;
import io.github.ericmedvet.jgea.core.representation.tree.numeric.Element;
import io.github.ericmedvet.jgea.core.util.IntRange;
import io.github.ericmedvet.jnb.datastructure.DoubleRange;
import io.github.ericmedvet.jnb.datastructure.FormattedNamedFunction;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class StatsMain {

  public static final int MAX_N_OF_STEPS = 100;
  public static final int MAX_N_OF_TOKENS = 1000;
  public static final List<Gate> ALL_GATES = List.of(
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
      Gates.bNot(),
      Gates.repeater(),
      Gates.iRange(),
      Gates.iConst(0),
      Gates.iConst(1),
      Gates.iConst(5),
      Gates.dConst(0),
      Gates.dConst(0.1),
      Gates.dConst(0.5),
      Gates.bConst(true)
  );

  public static final DataFactory DATA_FACTORY = new DataFactory(
      List.of(1, 2, 3),
      List.of(1d, 2d, 3d, 1.5, 2.5, 3.14),
      List.of("cat", "dog", "Hello World!", "mummy"),
      new IntRange(-10, MAX_N_OF_STEPS),
      new DoubleRange(-10, 10),
      new IntRange(2, 20),
      new IntRange(3, 8)
  );

  private static void factoryStats(
      List<Type> inputTypes,
      List<Type> outputTypes,
      int nOfNetworks,
      int maxNumberOfGates,
      int nOfCases,
      RandomGenerator rnd
  ) throws ProgramExecutionException {
    NetworkFactory factory = new NetworkFactory(
        inputTypes,
        outputTypes,
        new LinkedHashSet<>(ALL_GATES),
        maxNumberOfGates,
        0
    );
    Runner runner = new Runner(MAX_N_OF_STEPS, MAX_N_OF_TOKENS, false);
    List<List<Object>> cases = IntStream.range(0, nOfCases)
        .mapToObj(
            i -> inputTypes.stream()
                .map(t -> DATA_FACTORY.apply(t, rnd))
                .toList()
        )
        .toList();
    Predicate<Network> goodGatesPredicate = network -> IntStream.range(0, network.gates().size())
        .noneMatch(network::isGateAutoBlocked);
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
            n -> goodGatesPredicate.test(n) ? 1d : 0d,
            "*%6.4f",
            "isAllGood"
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
            "*%6.4f",
            "rate.cases.null"
        )
    );
    System.out.println(fs.stream().map(f -> f.name()).collect(Collectors.joining("\t")));
    Function<Collection<Network>, Map<String, Double>> statter = networks -> fs.stream()
        .collect(
            Collectors.toMap(
                f -> f.name(),
                f -> networks.stream().mapToDouble(f::apply).average().orElse(Double.NaN)
            )
        );
    Function<Collection<Network>, String> sStatter = statter.andThen(
        map -> fs.stream()
            .map(f -> f.format().formatted(map.get(f.name())))
            .collect(Collectors.joining(" "))
    );

    List<Network> all = factory.build(nOfNetworks, rnd);
    NetworkMutation mutation = new NetworkMutation(new LinkedHashSet<>(ALL_GATES), maxNumberOfGates);
    SequencedMap<String, List<Network>> map = new LinkedHashMap<>();
    map.put("factory-all", all);
    map.put("factory-good", all.stream().filter(goodGatesPredicate).toList());
    map.put("factory-bad", all.stream().filter(goodGatesPredicate.negate()).toList());
    map.put("mutated-all", all.stream().map(network -> mutation.mutate(network, rnd)).toList());
    map.put(
        "mutated-good",
        map.get("factory-good").stream().map(network -> mutation.mutate(network, rnd)).toList()
    );
    map.put("mutated-bad", map.get("factory-bad").stream().map(network -> mutation.mutate(network, rnd)).toList());
    map.forEach(
        (name, networks) -> System.out.printf("%16.16s (%4d) -> %s%n", name, networks.size(), sStatter.apply(networks))
    );
  }

  public static void main(String[] args) throws ProgramExecutionException {
    factoryStats(
        List.of(Composed.sequence(Base.REAL), Composed.sequence(Base.REAL)),
        List.of(Base.REAL),
        200,
        32,
        10,
        new Random(1)
    );
  }

}
