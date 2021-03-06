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
package edu.iu.dsc.tws.examples.internal.task;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.iu.dsc.tws.api.comms.Communicator;
import edu.iu.dsc.tws.api.comms.channel.TWSChannel;
import edu.iu.dsc.tws.api.compute.executor.ExecutionPlan;
import edu.iu.dsc.tws.api.compute.graph.ComputeGraph;
import edu.iu.dsc.tws.api.compute.schedule.elements.TaskSchedulePlan;
import edu.iu.dsc.tws.api.compute.schedule.elements.Worker;
import edu.iu.dsc.tws.api.compute.schedule.elements.WorkerPlan;
import edu.iu.dsc.tws.api.config.Config;
import edu.iu.dsc.tws.api.exceptions.TimeoutException;
import edu.iu.dsc.tws.api.resource.IWorkerController;
import edu.iu.dsc.tws.api.resource.Network;
import edu.iu.dsc.tws.executor.core.ExecutionPlanBuilder;
import edu.iu.dsc.tws.executor.threading.ExecutorFactory;
import edu.iu.dsc.tws.proto.jobmaster.JobMasterAPI;
import edu.iu.dsc.tws.tsched.streaming.roundrobin.RoundRobinTaskScheduler;

public final class TaskUtils {
  private static final Logger LOG = Logger.getLogger(TaskUtils.class.getName());

  private TaskUtils() {
  }

  public static void executeBatch(Config config, int workerID,
                                  ComputeGraph graph, IWorkerController workerController) {
    RoundRobinTaskScheduler roundRobinTaskScheduler = new RoundRobinTaskScheduler();
    roundRobinTaskScheduler.initialize(config);

    WorkerPlan workerPlan = null;
    List<JobMasterAPI.WorkerInfo> workerList = null;
    try {
      workerList = workerController.getAllWorkers();
    } catch (TimeoutException timeoutException) {
      LOG.log(Level.SEVERE, timeoutException.getMessage(), timeoutException);
      return;
    }
    workerPlan = createWorkerPlan(workerList);
    TaskSchedulePlan taskSchedulePlan = roundRobinTaskScheduler.schedule(graph, workerPlan);

    TWSChannel network = Network.initializeChannel(config, workerController);
    ExecutionPlanBuilder executionPlanBuilder = new ExecutionPlanBuilder(workerID,
        workerList, new Communicator(config, network), workerController.getCheckpointingClient());
    ExecutionPlan plan = executionPlanBuilder.build(config, graph, taskSchedulePlan);
    ExecutorFactory executor = new ExecutorFactory(config, workerID, network);
    executor.getExecutor(config, plan, graph.getOperationMode()).execute();
  }

  public static void execute(Config config, int workerID, ComputeGraph graph,
                             IWorkerController workerController) {
    RoundRobinTaskScheduler roundRobinTaskScheduler = new RoundRobinTaskScheduler();
    roundRobinTaskScheduler.initialize(config);

    List<JobMasterAPI.WorkerInfo> workerList = null;
    try {
      workerList = workerController.getAllWorkers();
    } catch (TimeoutException timeoutException) {
      LOG.log(Level.SEVERE, timeoutException.getMessage(), timeoutException);
      return;
    }

    WorkerPlan workerPlan = createWorkerPlan(workerList);
    TaskSchedulePlan taskSchedulePlan = roundRobinTaskScheduler.schedule(graph, workerPlan);

    TWSChannel network = Network.initializeChannel(config, workerController);
    ExecutionPlanBuilder executionPlanBuilder = new ExecutionPlanBuilder(workerID,
        workerList, new Communicator(config, network), workerController.getCheckpointingClient());
    ExecutionPlan plan = executionPlanBuilder.build(config, graph, taskSchedulePlan);
    ExecutorFactory executor = new ExecutorFactory(config, workerID, network);
    executor.getExecutor(config, plan, graph.getOperationMode()).execute();
  }

  public static WorkerPlan createWorkerPlan(List<JobMasterAPI.WorkerInfo> workerInfoList) {
    List<Worker> workers = new ArrayList<>();
    for (JobMasterAPI.WorkerInfo workerInfo : workerInfoList) {
      Worker w = new Worker(workerInfo.getWorkerID());
      workers.add(w);
    }

    return new WorkerPlan(workers);
  }

}
