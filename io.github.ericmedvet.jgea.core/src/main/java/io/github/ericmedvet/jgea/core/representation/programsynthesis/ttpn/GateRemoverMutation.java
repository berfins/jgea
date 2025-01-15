package io.github.ericmedvet.jgea.core.representation.programsynthesis.ttpn;

import io.github.ericmedvet.jgea.core.operator.Mutation;

import java.util.random.RandomGenerator;

public class GateRemoverMutation implements Mutation<Network> {

  private final int maxNOfAttempts;
  private final boolean avoidDeadGates;

  public GateRemoverMutation(int maxNOfAttempts, boolean avoidDeadGates) {
    this.maxNOfAttempts = maxNOfAttempts;
    this.avoidDeadGates = avoidDeadGates;
  }

  @Override
  public Network mutate(Network network, RandomGenerator random) {
    return null;
  }
}
