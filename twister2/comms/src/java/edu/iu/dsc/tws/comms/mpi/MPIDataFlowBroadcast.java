//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//  http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.
package edu.iu.dsc.tws.comms.mpi;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import edu.iu.dsc.tws.comms.api.MessageHeader;
import edu.iu.dsc.tws.comms.routing.BinaryTreeRouter;
import edu.iu.dsc.tws.comms.routing.IRouter;

public class MPIDataFlowBroadcast extends MPIDataFlowOperation {
  private static final Logger LOG = Logger.getLogger(MPIDataFlowBroadcast.class.getName());

  private int source;

  private Set<Integer> destinations;

  protected IRouter router;

  public MPIDataFlowBroadcast(TWSMPIChannel channel, int src, Set<Integer> dests) {
    super(channel);
    this.source = src;
    this.destinations = dests;
  }

  @Override
  public void close() {
  }

  @Override
  public void injectPartialResult(int src, Object message) {
    throw new RuntimeException("Not supported method");
  }

  protected void passMessageDownstream(MPIMessage currentMessage) {
    List<Integer> routes = new ArrayList<>();
    // we will get the routing based on the originating id
    routeReceivedMessage(currentMessage.getHeader(), routes);
    // try to send further
    sendMessage(currentMessage, routes);
  }

  protected void setupRouting() {
    // we will only have one distinct route
    Set<Integer> sources = new HashSet<>();
    sources.add(source);

    router = new BinaryTreeRouter(config, instancePlan, sources, destinations, edge, 1);
  }

  @Override
  protected void routeReceivedMessage(MessageHeader message, List<Integer> routes) {
    // check the origin
    int src = message.getSourceId();
    // get the expected routes
    Set<Integer> routing = router.getDownstreamTasks(src);

    if (routing == null) {
      throw new RuntimeException("Un-expected message from source: " + src);
    }

    routes.addAll(routing);
  }

  @Override
  protected void routeSendMessage(int src, List<Integer> routes) {
    // get the expected routes
    Set<Integer> routing = router.getDownstreamTasks(src);

    if (routing == null) {
      throw new RuntimeException("Un-expected message from source: " + src);
    }

    routes.addAll(routing);
  }

  @Override
  protected Set<Integer> receivingExecutors() {
    return null;
  }

  @Override
  protected Map<Integer, List<Integer>> receiveExpectedTaskIds() {
    return null;
  }

  @Override
  protected boolean isLast(int taskIdentifier) {
    return false;
  }
}

