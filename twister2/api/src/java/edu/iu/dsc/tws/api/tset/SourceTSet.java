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
package edu.iu.dsc.tws.api.tset;

import java.util.Random;

import edu.iu.dsc.tws.api.task.ComputeConnection;
import edu.iu.dsc.tws.api.task.TaskExecutor;
import edu.iu.dsc.tws.api.task.TaskGraphBuilder;
import edu.iu.dsc.tws.api.tset.ops.SourceOp;
import edu.iu.dsc.tws.common.config.Config;
import edu.iu.dsc.tws.dataset.DataObject;
import edu.iu.dsc.tws.dataset.DataObjectImpl;
import edu.iu.dsc.tws.dataset.impl.EntityPartition;

public class SourceTSet<T> extends BaseTSet<T> {
  private Source<T> source;
  private DataObject<T> datapoints = null;

  public SourceTSet(Config cfg, TaskGraphBuilder bldr, Source<T> src, TaskExecutor executor) {
    super(cfg, bldr);
    this.source = src;
    this.name = "source-" + new Random(System.nanoTime()).nextInt(10);
    datapoints = new DataObjectImpl<>(config);
  }

  @Override
  public boolean baseBuild() {
    builder.addSource(getName(), new SourceOp<T>(source), parallel);
    return true;
  }

  @Override
  public void buildConnection(ComputeConnection connection) {
  }

  private class CacheSink implements Sink<T> {

    @Override
    public boolean add(T value) {
      datapoints.addPartition(new EntityPartition<T>(0, value));
      return true;
    }

    @Override
    public void close() {

    }
  }
}
