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

package edu.iu.dsc.tws.api.tset.sets.streaming;

import edu.iu.dsc.tws.api.task.nodes.INode;
import edu.iu.dsc.tws.api.tset.TSetEnvironment;
import edu.iu.dsc.tws.api.tset.TSetUtils;
import edu.iu.dsc.tws.api.tset.fn.Source;
import edu.iu.dsc.tws.api.tset.ops.SourceOp;

public class StreamingSourceTSet<T> extends StreamingBaseTSet<T> {
  private Source<T> source;

  public StreamingSourceTSet(TSetEnvironment tSetEnv, Source<T> src, int parallelism) {
    super(tSetEnv, TSetUtils.generateName("ssource"), parallelism);
    this.source = src;
  }

/*  public <P> StreamingMapTSet<T, P> map(MapFunction<T, P> mapFn) {
    StreamingDirectTLink<T> direct = new StreamingDirectTLink<>(getTSetEnv(), getParallelism());
    addChildToGraph(direct);
    return direct.map(mapFn);
  }

  public <P> StreamingFlatMapTSet<T, P> flatMap(FlatMapFunction<T, P> mapFn) {
    StreamingDirectTLink<T> direct = new StreamingDirectTLink<>(getTSetEnv(), getParallelism());
    addChildToGraph(direct);
    return direct.flatMap(mapFn);
  }
*/

  @Override
  public StreamingSourceTSet<T> setName(String n) {
    rename(n);
    return this;
  }

  @Override
  public INode getINode() {
    return new SourceOp<>(source);
  }
}
