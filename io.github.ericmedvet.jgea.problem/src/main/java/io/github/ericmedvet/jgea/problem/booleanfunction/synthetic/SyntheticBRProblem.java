package io.github.ericmedvet.jgea.problem.booleanfunction.synthetic;

import io.github.ericmedvet.jgea.core.problem.TargetEBProblem;
import io.github.ericmedvet.jgea.problem.booleanfunction.BooleanFunction;
import io.github.ericmedvet.jgea.problem.booleanfunction.BooleanRegressionProblem;

import java.util.SequencedMap;

public interface SyntheticBRProblem extends BooleanRegressionProblem, TargetEBProblem<BooleanFunction, boolean[], boolean[], BooleanRegressionProblem.Outcome, SequencedMap<String, Double>> {
}
