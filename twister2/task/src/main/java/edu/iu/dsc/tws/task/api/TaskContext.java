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
package edu.iu.dsc.tws.task.api;

public class TaskContext {
  private int taskIndex;

  private int taskId;

  private String taskName;

  private int parallelism;

  public TaskContext(int taskIndex, int taskId, String taskName, int parallelism) {
    this.taskIndex = taskIndex;
    this.taskId = taskId;
    this.taskName = taskName;
    this.parallelism = parallelism;
  }

  /**
   * The task index
   * @return
   */
  int taskIndex() {
    return taskIndex;
  }

  /**
   * Task id
   * @return the task id
   */
  int taskId() {
    return taskId;
  }

  /**
   * Name of the task
   */
  String taskName() {
    return taskName;
  }

  public int getParallelism() {
    return parallelism;
  }
}
