/*
 * Copyright 2023 eric
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.ericmedvet.jgea.experimenter.net;

import io.github.ericmedvet.jgea.core.listener.*;
import io.github.ericmedvet.jgea.core.solver.state.POSetPopulationState;
import io.github.ericmedvet.jgea.core.util.Progress;
import io.github.ericmedvet.jgea.experimenter.Experiment;
import io.github.ericmedvet.jgea.experimenter.Run;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * @author "Eric Medvet" on 2023/03/25 for jgea
 */
public class NetListenerClient<G, S, Q> implements ListenerFactory<POSetPopulationState<G, S, Q>, Run<?, G, S, Q>> {

  private final static Logger L = Logger.getLogger(NetListenerClient.class.getName());
  private final String serverAddress;
  private final int serverPort;
  private final String serverKey;
  private final double pollInterval;
  private final List<NamedFunction<? super POSetPopulationState<G, S, Q>, ?>> stateFunctions;
  private final List<PlotTableBuilder<? super POSetPopulationState<G, S, Q>>> plotTableBuilders;
  private final Experiment experiment;
  private final Map<Integer, Update> updates;
  private final ScheduledExecutorService service;

  private ObjectOutputStream oos = null;

  public NetListenerClient(
      String serverAddress,
      int serverPort,
      String serverKey,
      double pollInterval,
      List<NamedFunction<? super POSetPopulationState<G, S, Q>, ?>> stateFunctions,
      List<PlotTableBuilder<? super POSetPopulationState<G, S, Q>>> plotTableBuilders,
      Experiment experiment
  ) {
    this.serverAddress = serverAddress;
    this.serverPort = serverPort;
    this.serverKey = serverKey;
    this.pollInterval = pollInterval;
    this.stateFunctions = stateFunctions;
    this.plotTableBuilders = plotTableBuilders;
    this.experiment = experiment;
    //check plot builders
    List<PlotTableBuilder<? super POSetPopulationState<G, S, Q>>> wrongPlotTableBuilders = plotTableBuilders.stream()
        .filter(ptb -> ptb.yFunctions().size() != 1)
        .toList();
    if (!wrongPlotTableBuilders.isEmpty()) {
      throw new IllegalArgumentException(
          "There are %d plot builders with num. of y data series not being 1, the first has %s".formatted(
              wrongPlotTableBuilders.size(),
              wrongPlotTableBuilders.get(0).yNames()
          ));
    }
    updates = new HashMap<>();
    service = Executors.newSingleThreadScheduledExecutor();
    service.scheduleAtFixedRate(this::sendUpdates, 0, (int) (1000 * pollInterval), TimeUnit.MILLISECONDS);
  }

  @Override
  public Listener<POSetPopulationState<G, S, Q>> build(Run<?, G, S, Q> run) {
    return new Listener<>() {
      @Override
      public void listen(POSetPopulationState<G, S, Q> state) {
        synchronized (updates) {
          Update update = updates.getOrDefault(run.index(), new Update(
              0, "", -1, Progress.NA, true, new LinkedHashMap<>(), new LinkedHashMap<>()
          ));
          Map<Update.DataItemKey, List<Object>> dataItems = update.dataItems();
          stateFunctions.forEach(f -> {
            Update.DataItemKey dik = new Update.DataItemKey(f.getName(), f.getFormat());
            dataItems.putIfAbsent(dik, new ArrayList<>());
            dataItems.get(dik).add(f.apply(state));
          });
          Map<Update.PlotItemKey, List<Update.PlotPoint>> plotItems = update.plotItems();
          plotTableBuilders.forEach(p -> {
            double minX = Double.NaN;
            double maxX = Double.NaN;
            if (p instanceof XYPlotTableBuilder<? super POSetPopulationState<G, S, Q>> xyPlotTableBuilder) {
              minX = xyPlotTableBuilder.getMinX();
              maxX = xyPlotTableBuilder.getMaxX();
            }
            Update.PlotItemKey pik = new Update.PlotItemKey(
                p.xName(),
                p.yNames().get(0),
                minX,
                maxX
            );
            plotItems.putIfAbsent(pik, new ArrayList<>());
            double x = p.xFunction().apply(state).doubleValue();
            double y = p.yFunctions().get(0).apply(state).doubleValue();
            if (Double.isFinite(x) && Double.isFinite(y)) {
              plotItems.get(pik).add(new Update.PlotPoint(x, y));
            }
          });
          updates.put(run.index(), new Update(
              System.currentTimeMillis(),
              run.map().toString(),
              run.index(),
              state.getProgress(),
              true,
              update.dataItems(),
              update.plotItems()
          ));
        }
      }

      @Override
      public void done() {
        synchronized (updates) {
          updates.merge(
              run.index(),
              new Update(
                  System.currentTimeMillis(),
                  run.map().toString(),
                  run.index(),
                  Progress.NA,
                  false,
                  new LinkedHashMap<>(),
                  new LinkedHashMap<>()
              ),
              (ou, nu) -> new Update(
                  System.currentTimeMillis(),
                  run.map().toString(),
                  run.index(),
                  ou.runProgress(),
                  false,
                  ou.dataItems(),
                  ou.plotItems()
              )
          );
        }
      }
    };
  }

  @Override
  public void shutdown() {
    sendUpdates();
    service.shutdownNow();
  }

  private void doHandshake(
      ObjectInputStream ois,
      ObjectOutputStream oos
  ) throws IOException {
    try {
      int n = Integer.parseInt(NetUtils.decrypt((String) ois.readObject(), serverKey));
      oos.writeObject(NetUtils.encrypt(Integer.toString(n + 1), serverKey));
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  private void openConnection() {
    if (oos != null) {
      return;
    }
    Socket socket = null;
    try {
      socket = new Socket(serverAddress, serverPort);
      oos = new ObjectOutputStream(socket.getOutputStream());
      doHandshake(
          new ObjectInputStream(socket.getInputStream()),
          oos
      );
    } catch (IOException e) {
      L.warning("Cannot open connection due to: %s".formatted(e));
      if (socket != null) {
        try {
          socket.close();
        } catch (IOException ex) {
          //ignore
        }
      }
    }
  }

  private void sendUpdates() {
    List<Update> toSendUpdates;
    synchronized (updates) {
      toSendUpdates = new ArrayList<>(updates.values());
      updates.clear();
    }
    //prepare message
    Message message = new Message(
        System.currentTimeMillis(),
        NetUtils.getMachineInfo(),
        NetUtils.getProcessInfo(),
        pollInterval,
        experiment.runs().size(),
        toSendUpdates
    );
    //attempt send
    openConnection();
    if (oos != null) {
      try {
        oos.writeObject(message);
        L.fine("Message sent with %d updates".formatted(message.updates().size()));
      } catch (IOException e) {
        L.warning("Cannot send message with %d updates due to: %s".formatted(message.updates().size(), e));
        synchronized (updates) {
          message.updates().forEach(u -> updates.put(u.runIndex(), u));
        }
        try {
          oos.close();
        } catch (IOException e2) {
          L.warning("Cannot open connection due to: %s".formatted(e2));
        }
        oos = null;
      }
    }
  }
}
