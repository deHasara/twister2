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
package edu.iu.dsc.tws.checkpointing.worker;

import edu.iu.dsc.tws.api.config.Config;
import edu.iu.dsc.tws.api.resource.IPersistentVolume;
import edu.iu.dsc.tws.api.resource.IVolatileVolume;
import edu.iu.dsc.tws.api.resource.IWorker;
import edu.iu.dsc.tws.api.resource.IWorkerController;
import edu.iu.dsc.tws.api.resource.IWorkerFailureListener;
import edu.iu.dsc.tws.api.resource.WorkerEnvironment;
import edu.iu.dsc.tws.proto.jobmaster.JobMasterAPI;

public class ManagedWorker implements IWorker, IWorkerFailureListener {
  private WorkerEnvironment wEnv;

  /**
   * The configuration
   */
  private Config cfg;

  /**
   * Worker id
   */
  private int wId;

  /**
   * Worker controller
   */
  private IWorkerController wController;

  /**
   * Persistent volume
   */
  private IPersistentVolume pVolume;

  /**
   * The volatile volue
   */
  private IVolatileVolume vVolume;

  @Override
  public void execute(Config config, int workerID,
                      IWorkerController workerController,
                      IPersistentVolume persistentVolume,
                      IVolatileVolume volatileVolume) {
    this.cfg = config;
    this.wId = workerID;
    this.wController = workerController;
    this.pVolume = persistentVolume;
    this.vVolume = volatileVolume;

    this.wEnv = WorkerEnvironment.init(config, workerID, workerController,
        persistentVolume, volatileVolume);
  }

  @Override
  public void failed(int workerID) {
    // lets close the worker environment
    this.wEnv.close();
  }

  @Override
  public void restarted(JobMasterAPI.WorkerInfo workerInfo) {
    // now lets try to restart
    this.wEnv = WorkerEnvironment.init(cfg, wId, wController,
        pVolume, vVolume);
  }
}
