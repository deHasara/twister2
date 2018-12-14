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

package edu.iu.dsc.tws.master.worker;

import java.nio.channels.SocketChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.protobuf.Message;

import edu.iu.dsc.tws.common.config.Config;
import edu.iu.dsc.tws.common.net.tcp.Progress;
import edu.iu.dsc.tws.common.net.tcp.StatusCode;
import edu.iu.dsc.tws.common.net.tcp.request.BlockingSendException;
import edu.iu.dsc.tws.common.net.tcp.request.ConnectHandler;
import edu.iu.dsc.tws.common.net.tcp.request.MessageHandler;
import edu.iu.dsc.tws.common.net.tcp.request.RRClient;
import edu.iu.dsc.tws.common.net.tcp.request.RequestID;
import edu.iu.dsc.tws.common.resource.WorkerInfoUtils;
import edu.iu.dsc.tws.master.JobMasterContext;
import edu.iu.dsc.tws.proto.jobmaster.JobMasterAPI;
import edu.iu.dsc.tws.proto.jobmaster.JobMasterAPI.WorkerInfo;

/**
 * JobMasterClient class
 * It is started for each Twister2 worker
 * It handles the communication with the Job Master
 * <p>
 * It provides:
 * worker discovery
 * barrier method
 * Ping service
 * <p>
 * It can be started in two different modes:
 * Threaded and Blocking
 * <p>
 * If the user calls:
 * startThreaded()
 * It starts as a Thread and the call to this method returns
 * <p>
 * If the user calls:
 * startBlocking()
 * It uses the calling thread and this call does not return unless the close method is called
 */

public class JobMasterClient {
  private static final Logger LOG = Logger.getLogger(JobMasterClient.class.getName());

  private static Progress looper;
  private boolean stopLooper = false;

  private Config config;
  private WorkerInfo thisWorker;

  private String masterAddress;
  private int masterPort;

  private RRClient rrClient;
  private Pinger pinger;
  private JMWorkerController jmWorkerController;

  private boolean registrationSucceeded;

  private int numberOfWorkers;

  /**
   * the maximum duration this client will try to connect to the Job Master
   * in milli seconds
   */
  private static final long CONNECTION_TRY_TIME_LIMIT = 100000;

  /**
   * to control the connection error when we repeatedly try connecting
   */
  private boolean connectionRefused = false;

  public JobMasterClient(Config config,
                         WorkerInfo thisWorker,
                         String masterHost,
                         int masterPort,
                         int numberOfWorkers) {
    this.config = config;
    this.thisWorker = thisWorker;
    this.masterAddress = masterHost;
    this.masterPort = masterPort;
    this.numberOfWorkers = numberOfWorkers;
  }

  /**
   * return WorkerInfo for this worker
   */
  public WorkerInfo getWorkerInfo() {
    return thisWorker;
  }

  /**
   * initialize JobMasterClient
   * wait until it connects to JobMaster
   * return false, if it can not connect to JobMaster
   */
  private void init() {

    looper = new Progress();

    ClientConnectHandler connectHandler = new ClientConnectHandler();
    rrClient = new RRClient(masterAddress, masterPort, null, looper,
        thisWorker.getWorkerID(), connectHandler);

    long interval = JobMasterContext.pingInterval(config);
    pinger = new Pinger(thisWorker.getWorkerID(), rrClient, interval);

    // protocol buffer message registrations
    JobMasterAPI.Ping.Builder pingBuilder = JobMasterAPI.Ping.newBuilder();
    rrClient.registerResponseHandler(pingBuilder, pinger);

    JobMasterAPI.RegisterWorker.Builder registerWorkerBuilder =
        JobMasterAPI.RegisterWorker.newBuilder();
    JobMasterAPI.RegisterWorkerResponse.Builder registerWorkerResponseBuilder
        = JobMasterAPI.RegisterWorkerResponse.newBuilder();

    JobMasterAPI.WorkerStateChange.Builder stateChangeBuilder =
        JobMasterAPI.WorkerStateChange.newBuilder();
    JobMasterAPI.WorkerStateChangeResponse.Builder stateChangeResponseBuilder
        = JobMasterAPI.WorkerStateChangeResponse.newBuilder();

    JobMasterAPI.ScaledComputeResource.Builder scaleMessageBuilder =
        JobMasterAPI.ScaledComputeResource.newBuilder();

    ResponseMessageHandler responseMessageHandler = new ResponseMessageHandler();
    rrClient.registerResponseHandler(registerWorkerBuilder, responseMessageHandler);
    rrClient.registerResponseHandler(registerWorkerResponseBuilder, responseMessageHandler);
    rrClient.registerResponseHandler(stateChangeBuilder, responseMessageHandler);
    rrClient.registerResponseHandler(stateChangeResponseBuilder, responseMessageHandler);
    rrClient.registerResponseHandler(scaleMessageBuilder, responseMessageHandler);

    // try to connect to JobMaster
    tryUntilConnected(CONNECTION_TRY_TIME_LIMIT);

    if (!rrClient.isConnected()) {
      throw new RuntimeException("JobMasterClient can not connect to Job Master. Exiting .....");
    }
  }

  /**
   * this will be initialized after worker registration
   * since WorkerInfo may change when job master assigns workerIDs
   */
  private void initJMWorkerController() {

    jmWorkerController = new JMWorkerController(config, thisWorker, rrClient, numberOfWorkers);

    JobMasterAPI.ListWorkersRequest.Builder listRequestBuilder =
        JobMasterAPI.ListWorkersRequest.newBuilder();
    JobMasterAPI.ListWorkersResponse.Builder listResponseBuilder =
        JobMasterAPI.ListWorkersResponse.newBuilder();
    rrClient.registerResponseHandler(listRequestBuilder, jmWorkerController);
    rrClient.registerResponseHandler(listResponseBuilder, jmWorkerController);

    JobMasterAPI.BarrierRequest.Builder barrierRequestBuilder =
        JobMasterAPI.BarrierRequest.newBuilder();
    JobMasterAPI.BarrierResponse.Builder barrierResponseBuilder =
        JobMasterAPI.BarrierResponse.newBuilder();
    rrClient.registerResponseHandler(barrierRequestBuilder, jmWorkerController);
    rrClient.registerResponseHandler(barrierResponseBuilder, jmWorkerController);
  }

  public JMWorkerController getJMWorkerController() {
    return jmWorkerController;
  }

  /**
   * stop the JobMasterClient
   */
  public void close() {
    stopLooper = true;
    looper.wakeup();
  }

  private void startLooping() {

    while (!stopLooper) {
      long timeToNextPing = pinger.timeToNextPing();
      if (timeToNextPing < 30 && registrationSucceeded) {
        pinger.sendPingMessage();
      } else {
        looper.loopBlocking(timeToNextPing);
      }
    }

    rrClient.disconnect();
  }

  /**
   * start the Job Master Client in a Thread
   */
  public Thread startThreaded() {
    // first initialize the client, connect to Job Master
    init();

    Thread jmThread = new Thread() {
      public void run() {
        startLooping();
      }
    };

    jmThread.start();

    boolean registered = registerWorker();
    if (!registered) {
      throw new RuntimeException("Could not register JobMaster with Dashboard. Exiting .....");
    }

    return jmThread;
  }

  /**
   * start the Job Master Client in a blocking call
   */
  public void startBlocking() {
    // first initialize the client, connect to Job Master
    init();

    startLooping();

    boolean registered = registerWorker();
    if (!registered) {
      throw new RuntimeException("Could not register JobMaster with Dashboard. Exiting .....");
    }
  }

  /**
   * try connecting until the time limit is reached
   */
  public boolean tryUntilConnected(long timeLimit) {
    long startTime = System.currentTimeMillis();
    long duration = 0;
    long sleepInterval = 50;

    // log interval in milliseconds
    long logInterval = 1000;
    long nextLogTime = logInterval;

    // allow the first connection attempt
    connectionRefused = true;

    while (duration < timeLimit) {
      // try connecting
      if (connectionRefused) {
        rrClient.tryConnecting();
        connectionRefused = false;
      }

      // loop to connect
      looper.loop();

      if (rrClient.isConnected()) {
        return true;
      }

      try {
        Thread.sleep(sleepInterval);
      } catch (InterruptedException e) {
        LOG.warning("Sleep interrupted.");
      }

      if (rrClient.isConnected()) {
        return true;
      }

      duration = System.currentTimeMillis() - startTime;

      if (duration > nextLogTime) {
        LOG.info("Still trying to connect to the Job Master: " + masterAddress + ":" + masterPort);
        nextLogTime += logInterval;
      }
    }

    return false;
  }

  /**
   * send RegisterWorker message to Job Master
   * put WorkerInfo in this message
   * @return
   */
  private boolean registerWorker() {

    JobMasterAPI.RegisterWorker registerWorker = JobMasterAPI.RegisterWorker.newBuilder()
        .setWorkerID(thisWorker.getWorkerID())
        .setWorkerInfo(thisWorker)
        .build();

    LOG.fine("Sending RegisterWorker message: \n" + registerWorker);

    // wait for the response
    try {
      rrClient.sendRequestWaitResponse(registerWorker,
          JobMasterContext.responseWaitDuration(config));

      if (registrationSucceeded) {
        pinger.sendPingMessage();
        initJMWorkerController();
      }

      return registrationSucceeded;

    } catch (BlockingSendException bse) {
      LOG.log(Level.SEVERE, bse.getMessage(), bse);
      return false;
    }
  }

  public boolean sendWorkerRunningMessage() {

    JobMasterAPI.WorkerStateChange workerStateChange = JobMasterAPI.WorkerStateChange.newBuilder()
        .setWorkerID(thisWorker.getWorkerID())
        .setState(JobMasterAPI.WorkerState.RUNNING)
        .build();

    RequestID requestID = rrClient.sendRequest(workerStateChange);
    if (requestID == null) {
      LOG.severe("Could not send Worker RUNNING message.");
      return false;
    }

    LOG.fine("Sent Worker RUNNING message: \n" + workerStateChange);
    return true;
  }

  public boolean sendWorkerCompletedMessage() {

    JobMasterAPI.WorkerStateChange workerStateChange = JobMasterAPI.WorkerStateChange.newBuilder()
        .setWorkerID(thisWorker.getWorkerID())
        .setState(JobMasterAPI.WorkerState.COMPLETED)
        .build();

    LOG.fine("Sending Worker COMPLETED message: \n" + workerStateChange);
    try {
      rrClient.sendRequestWaitResponse(workerStateChange,
          JobMasterContext.responseWaitDuration(config));
    } catch (BlockingSendException e) {
      LOG.log(Level.SEVERE, e.getMessage(), e);
      return false;
    }

    return true;
  }

  public class ResponseMessageHandler implements MessageHandler {

    @Override
    public void onMessage(RequestID id, int workerId, Message message) {

      if (message instanceof JobMasterAPI.RegisterWorkerResponse) {

        LOG.fine("Received a RegisterWorkerResponse message from the master. \n" + message);

        JobMasterAPI.RegisterWorkerResponse responseMessage =
            (JobMasterAPI.RegisterWorkerResponse) message;

        registrationSucceeded = responseMessage.getResult();

        if (JobMasterContext.jobMasterAssignsWorkerIDs(config)) {
          thisWorker = WorkerInfoUtils.updateWorkerID(thisWorker, responseMessage.getWorkerID());
          pinger.setWorkerID(responseMessage.getWorkerID());
          rrClient.setWorkerID(responseMessage.getWorkerID());
        }

      } else if (message instanceof JobMasterAPI.WorkerStateChangeResponse) {
        LOG.fine("Received a WorkerStateChange response from the master. \n" + message);

        // nothing to do
      } else if (message instanceof JobMasterAPI.ScaledComputeResource) {
        LOG.info("Received ScaleComputeResource message from the master. \n" + message);

        // nothing to do

      } else {
        LOG.warning("Received message unrecognized. \n" + message);
      }

    }
  }

  public class ClientConnectHandler implements ConnectHandler {
    @Override
    public void onError(SocketChannel channel) {

    }

    @Override
    public void onConnect(SocketChannel channel, StatusCode status) {
      if (status == StatusCode.SUCCESS) {
        LOG.info(thisWorker.getWorkerID() + " JobMasterClient connected to JobMaster: " + channel);
      }

      if (status == StatusCode.CONNECTION_REFUSED) {
        connectionRefused = true;
      }
    }

    @Override
    public void onClose(SocketChannel channel) {

    }
  }

}
