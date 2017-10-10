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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import edu.iu.dsc.tws.common.config.Config;
import edu.iu.dsc.tws.comms.api.Message;
import edu.iu.dsc.tws.comms.api.MessageDeSerializer;
import edu.iu.dsc.tws.comms.api.MessageHeader;
import edu.iu.dsc.tws.comms.api.MessageReceiver;
import edu.iu.dsc.tws.comms.api.MessageSerializer;
import edu.iu.dsc.tws.comms.core.TaskPlan;
import edu.iu.dsc.tws.comms.routing.BinaryTreeRouter;
import edu.iu.dsc.tws.comms.routing.IRouter;
import edu.iu.dsc.tws.comms.routing.Routing;

public class MPIDataFlowReduce extends MPIDataFlowOperation {
  private static final Logger LOG = Logger.getLogger(MPIDataFlowBroadcast.class.getName());

  /**
   * Keep track of the current message been received
   */
  private Map<Integer, Map<Integer, MPIMessage>> currentMessages = new HashMap<>();

  public MPIDataFlowReduce(TWSMPIChannel channel) {
    super(channel);
  }

  @Override
  public void init(Config cfg, int task, TaskPlan plan,
                   Set<Integer> srcs, Set<Integer> dests,
                   int messageStream, MessageReceiver rcvr,
                   MessageDeSerializer fmtr, MessageSerializer bldr,
                   MessageReceiver partialRcvr) {
    super.init(cfg, task, plan, srcs, dests, messageStream, rcvr, fmtr, bldr, partialRcvr);

    if (dests.size() > 1) {
      throw new IllegalArgumentException("Reduce can only have one destination");
    }

    for (Integer source : expectedRoutes.keySet()) {
      currentMessages.put(source, new HashMap<Integer, MPIMessage>());
    }
  }

  public IRouter setupRouting() {
    // lets create the routing needed
    BinaryTreeRouter tree = new BinaryTreeRouter();
    // we only have one destination and sources becomes destinations for creating tree
    // because this is an inverted tree
    tree.init(config, thisTask, instancePlan, destinations, sources, stream, 1);
    return tree;
  }

  @Override
  protected void routeReceivedMessage(MessageHeader message, List<Integer> routes) {
    throw new RuntimeException("We don't rout send received messages directly");
  }

  @Override
  protected void routeSendMessage(MessageHeader message, List<Integer> routes) {
    // check the origin
    int source = message.getSourceId();
    // get the expected routes
    Routing routing = expectedRoutes.get(source);

    if (routing == null) {
      throw new RuntimeException("Un-expected message from source: " + source);
    }
    routes.addAll(routing.getDownstreamIds());
  }

  @Override
  public void onReceiveComplete(int id, int stream, MPIBuffer buffer) {
    int originatingNode = buffer.getByteBuffer().getInt();
    int sourceNode = buffer.getByteBuffer().getInt();

    Map<Integer, MPIMessage> messageMap = currentMessages.get(sourceNode);

    // we need to try to build the message here, we may need many more messages to complete
    MPIMessage currentMessage = messageMap.get(originatingNode);

    if (currentMessage == null) {
      MessageHeader header = buildHeader(buffer);
      currentMessage = new MPIMessage(thisTask, header, MPIMessageType.RECEIVE, this);
      messageMap.put(originatingNode, currentMessage);
    } else if (!currentMessage.isComplete()) {
      currentMessage.addBuffer(buffer);
      currentMessage.build();
    }

    if (currentMessage.isComplete()) {
      // we received a message, we need to determine weather we need to
      // forward to another node and process
      if (messageDeSerializer != null) {
        Object object = messageDeSerializer.buid(currentMessage);
        receiver.onMessage(object);
      }

      currentMessages.remove(originatingNode);
    }
  }


  @Override
  public void injectPartialResult(Message message) {
    super.injectPartialResult(message);
  }

  @Override
  public void sendCompleteMessage(Message message) {
    // this need to use the available buffers
    // we need to advertise the available buffers to the upper layers
    Object msgObj = messageSerializer.build(message);

    if (!(msgObj instanceof MPIMessage)) {
      throw new IllegalArgumentException("Expecting a message of MPIMessage type");
    }

    MPIMessage mpiMessage = (MPIMessage) msgObj;
    List<Integer> routes = new ArrayList<>();
    routeSendMessage(mpiMessage.getHeader(), routes);

    if (routes.size() > 1) {
      throw new RuntimeException("We only expect to send to one more task");
    }
    // now send the message
    sendMessage(mpiMessage, routes);
  }
}
