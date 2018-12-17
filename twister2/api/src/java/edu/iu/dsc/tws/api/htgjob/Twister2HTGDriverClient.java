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
package edu.iu.dsc.tws.api.htgjob;

import java.nio.channels.SocketChannel;
import java.util.logging.Logger;

import com.google.protobuf.Message;

import edu.iu.dsc.tws.common.config.Config;
import edu.iu.dsc.tws.common.net.tcp.Progress;
import edu.iu.dsc.tws.common.net.tcp.StatusCode;
import edu.iu.dsc.tws.common.net.tcp.request.ConnectHandler;
import edu.iu.dsc.tws.common.net.tcp.request.MessageHandler;
import edu.iu.dsc.tws.common.net.tcp.request.RRClient;
import edu.iu.dsc.tws.common.net.tcp.request.RRServer;
import edu.iu.dsc.tws.common.net.tcp.request.RequestID;
import edu.iu.dsc.tws.master.JobMasterContext;
import edu.iu.dsc.tws.proto.jobmaster.JobMasterAPI;
import edu.iu.dsc.tws.proto.system.job.HTGJobAPI;

public class Twister2HTGDriverClient {

  private static final Logger LOG = Logger.getLogger(Twister2HTGDriverClient.class.getName());

  private static Progress looper;
  private boolean stopLooper = false;

  private Config config;

  private String masterAddress;
  private int masterPort;

  private RRClient rrClient;

  private JobMasterAPI.HTGClientInfo thisClient;

  private HTGJobAPI.HTGJob htgJob;
  private HTGJobAPI.ExecuteMessage executeMessage;

  /**
   * the maximum duration this client will try to connect to the Job Master
   * in milli seconds
   */
  private static final long CONNECTION_TRY_TIME_LIMIT = 100000;

  /**
   * to control the connection error when we repeatedly try connecting
   */
  private boolean connectionRefused = false;

  public void setExecuteMessage(HTGJobAPI.ExecuteMessage executeMessage) {
    this.executeMessage = executeMessage;
  }

  public HTGJobAPI.ExecuteMessage getExecuteMessage() {
    return executeMessage;
  }

  public Twister2HTGDriverClient(Config config,
                                 JobMasterAPI.HTGClientInfo thisClient,
                                 HTGJobAPI.HTGJob htgJob) {
    this(config, thisClient, JobMasterContext.jobMasterIP(config),
        JobMasterContext.jobMasterPort(config), htgJob);
  }

  public Twister2HTGDriverClient(Config config,
                                 JobMasterAPI.HTGClientInfo thisClient,
                                 String masterHost,
                                 int masterPort,
                                 HTGJobAPI.HTGJob htgjob) {
    this.config = config;
    this.thisClient = thisClient;
    this.masterAddress = masterHost;
    this.masterPort = masterPort;

    //newly added
    this.htgJob = htgjob;
  }

  private boolean init() {

    looper = new Progress();

    Twister2HTGDriverClient.ClientConnectHandler connectHandler
        = new Twister2HTGDriverClient.ClientConnectHandler();

    rrClient = new RRClient(masterAddress, masterPort, config, looper,
        RRServer.CLIENT_ID, connectHandler);

    JobMasterAPI.HTGJobRequest.Builder htgjobRequestBuilder
        = JobMasterAPI.HTGJobRequest.newBuilder();
    JobMasterAPI.HTGJobResponse.Builder htgjobResponseBUilder
        = JobMasterAPI.HTGJobResponse.newBuilder();

    Twister2HTGDriverClient.ResponseMessageHandler responseMessageHandler
        = new Twister2HTGDriverClient.ResponseMessageHandler();

    rrClient.registerResponseHandler(htgjobRequestBuilder, responseMessageHandler);
    rrClient.registerResponseHandler(htgjobResponseBUilder, responseMessageHandler);

    // try to connect to JobMaster
    tryUntilConnected(CONNECTION_TRY_TIME_LIMIT);

    if (!rrClient.isConnected()) {
      LOG.severe("JobMasterClient can not connect to Job Master. Exiting .....");
      return false;
    }

    return true;
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
   * stop the JobMasterClient
   */
  public void close() {
    stopLooper = true;
    looper.wakeup();
  }

  private void startLooping() {

    while (!stopLooper) {
      looper.loopBlocking();
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

    return jmThread;
  }

  /**
   * start the Job Master Client in a blocking call
   */
  public void startBlocking() {
    // first initialize the client, connect to Job Master
    init();

    startLooping();
  }

  private class ResponseMessageHandler implements MessageHandler {

    @Override
    public void onMessage(RequestID id, int clientId, Message message) {

      if (message instanceof JobMasterAPI.HTGJobResponse) {
        LOG.info("HTG Job Response Received:"
            + ((JobMasterAPI.HTGJobResponse) message).getHtgSubgraphname());
      }
    }
  }

  private class ClientConnectHandler implements ConnectHandler {
    @Override
    public void onError(SocketChannel channel) {
    }

    @Override
    public void onConnect(SocketChannel channel, StatusCode status) {
      if (status == StatusCode.SUCCESS) {
        LOG.info(thisClient.getClientID() + "HTG Client connected to JobMaster: " + channel);
      }

      if (status == StatusCode.CONNECTION_REFUSED) {
        connectionRefused = true;
      }
    }

    @Override
    public void onClose(SocketChannel channel) {
    }
  }

  /**
   * This method sends the htg job object to the job master.
   */
  public boolean sendHTGDriverRequestMessage() {

    JobMasterAPI.HTGJobRequest htgJobRequest = JobMasterAPI.HTGJobRequest.newBuilder()
        .setHtgJob(htgJob)
        .setExecuteMessage(executeMessage)
        .build();

    LOG.info("HTG Job Client Message and Execute Message:" + htgJob + "\t" + executeMessage);

    try {
      rrClient.sendRequestWaitResponse(htgJobRequest,
          JobMasterContext.responseWaitDuration(config));
      return true;
    } catch (edu.iu.dsc.tws.common.net.tcp.request.BlockingSendException e) {
      e.printStackTrace();
      return false;
    }
  }
}
