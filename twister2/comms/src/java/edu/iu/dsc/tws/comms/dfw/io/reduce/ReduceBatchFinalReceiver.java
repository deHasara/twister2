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
package edu.iu.dsc.tws.comms.dfw.io.reduce;

import java.util.List;
import java.util.Map;

import edu.iu.dsc.tws.api.comms.DataFlowOperation;
import edu.iu.dsc.tws.api.comms.ReduceFunction;
import edu.iu.dsc.tws.api.comms.SingularReceiver;
import edu.iu.dsc.tws.api.config.Config;

public class ReduceBatchFinalReceiver extends BaseReduceBatchFinalReceiver {
  private SingularReceiver singularReceiver;

  public ReduceBatchFinalReceiver(ReduceFunction reduce, SingularReceiver receiver) {
    super(reduce);
    this.reduceFunction = reduce;
    this.singularReceiver = receiver;
  }

  @Override
  public void init(Config cfg, DataFlowOperation op, Map<Integer, List<Integer>> expectedIds) {
    super.init(cfg, op, expectedIds);
    singularReceiver.init(cfg, expectedIds.keySet());
  }

  protected boolean handleFinished(int task, Object value) {
    return singularReceiver.receive(task, value);
  }

  @Override
  protected boolean sendSyncForward(int target) {
    onSyncEvent(target, barriers.get(target));
    return false;
  }

  protected void onSyncEvent(int target, byte[] value) {
    singularReceiver.sync(target, value);
  }
}
