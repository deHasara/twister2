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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.reflect.TypeToken;

import edu.iu.dsc.tws.api.task.TaskGraphBuilder;
import edu.iu.dsc.tws.api.tset.link.AllGatherTLink;
import edu.iu.dsc.tws.api.tset.link.AllReduceTLink;
import edu.iu.dsc.tws.api.tset.link.BaseTLink;
import edu.iu.dsc.tws.api.tset.link.DirectTLink;
import edu.iu.dsc.tws.api.tset.link.GatherTLink;
import edu.iu.dsc.tws.api.tset.link.PartitionTLink;
import edu.iu.dsc.tws.api.tset.link.ReduceTLink;
import edu.iu.dsc.tws.api.tset.link.ReplicateTLink;
import edu.iu.dsc.tws.common.config.Config;

public abstract class BaseTSet<T> implements TSet<T> {
  private static final Logger LOG = Logger.getLogger(BaseTSet.class.getName());
  /**
   * The children of this set
   */
  protected List<TBase<?>> children;

  /**
   * The builder to use to building the task graph
   */
  protected TaskGraphBuilder builder;

  /**
   * Name of the data set
   */
  protected String name;

  /**
   * The parallelism of the set
   */
  protected int parallel = 4;
  /**
   * Defines if the TSet is Mutable or not
   */
  private boolean isMutable = false;
  /**
   * The configuration
   */
  protected Config config;

  /**
   * Possible Types of state in a TSet
   */
  private enum StateType {
    /**
     * Local state which is updated and maintained by each parallel task
     */
    LOCAL,
    /**
     * Distributed state is when each task has only access to a subset of the whole data
     * for example if the data set has N points and T tasks each task will access N/T points
     */
    DISTRIBUTED,
    /**
     * Replicated state is state that is made available as a whole to each task
     */
    REPLICATED
  }

  /**
   * The type of the TSet
   */
  private StateType stateType = StateType.DISTRIBUTED;

  public BaseTSet(Config cfg, TaskGraphBuilder bldr) {
    this.children = new ArrayList<>();
    this.builder = bldr;
    this.config = cfg;
  }

  public String getName() {
    return name;
  }

  public int getParallelism() {
    return parallel;
  }

  @Override
  public TSet<T> setParallelism(int parallelism) {
    this.parallel = parallelism;
    return this;
  }

  @Override
  public TSet<T> setName(String n) {
    this.name = n;
    return this;
  }

  @Override
  public DirectTLink<T> direct() {
    DirectTLink<T> direct = new DirectTLink<>(config, builder, this);
    children.add(direct);
    return direct;
  }

  @Override
  public ReduceTLink<T> reduce(ReduceFunction<T> reduceFn) {
    ReduceTLink<T> reduce = new ReduceTLink<T>(config, builder, this, reduceFn);
    children.add(reduce);
    return reduce;
  }

  public PartitionTLink<T> partition(PartitionFunction<T> partitionFn) {
    PartitionTLink<T> partition = new PartitionTLink<>(config, builder, this, partitionFn);
    children.add(partition);
    return partition;
  }

  @Override
  public GatherTLink<T> gather() {
    GatherTLink<T> gather = new GatherTLink<>(config, builder, this);
    children.add(gather);
    return gather;
  }

  @Override
  public AllReduceTLink<T> allReduce(ReduceFunction<T> reduceFn) {
    AllReduceTLink<T> reduce = new AllReduceTLink<T>(config, builder, this, reduceFn);
    children.add(reduce);
    return reduce;
  }

  @Override
  public AllGatherTLink<T> allGather() {
    AllGatherTLink<T> gather = new AllGatherTLink<>(config, builder, this);
    children.add(gather);
    return gather;
  }

  @Override
  public <K> GroupedTSet<T, K> groupBy(PartitionFunction<K> partitionFunction,
                                       Selector<T, K> selector) {
    GroupedTSet<T, K> groupedTSet = new GroupedTSet<>(config, builder, this,
        partitionFunction, selector);
    children.add(groupedTSet);
    return groupedTSet;
  }

  @Override
  public ReplicateTLink<T> replicate(int replications) {
    if (parallel != 1) {
      String msg = "TSets with parallelism 1 can be replicated: " + parallel;
      LOG.log(Level.SEVERE, msg);
      throw new RuntimeException(msg);
    }

    ReplicateTLink<T> cloneTSet = new ReplicateTLink<>(config, builder, this, replications);
    children.add(cloneTSet);
    return cloneTSet;
  }

  @Override
  public TSet<T> cache() {
    // todo: why cant we add a single cache tset here?
    DirectTLink<T> direct = new DirectTLink<>(config, builder, this);
    children.add(direct);
    CacheTSet<T> cacheTSet = new CacheTSet<T>(config, builder, direct);
    direct.getChildren().add(cacheTSet);
    return cacheTSet;
  }

  @Override
  public void build() {
    // first build our selves
    baseBuild();

    // then build children
    for (TBase<?> c : children) {
      c.build();
    }
  }

  public boolean isMutable() {
    return isMutable;
  }

  public void setMutable(boolean mutable) {
    isMutable = mutable;
  }

  public StateType getStateType() {
    return stateType;
  }

  public void setStateType(StateType stateType) {
    this.stateType = stateType;
  }

  protected Class getType() {
    TypeToken<T> typeToken = new TypeToken<T>(getClass()) {
    };
    return typeToken.getRawType();
  }

  /**
   * Override the parallelism
   *
   * @return if overide, return value, otherwise -1
   */
  protected int overrideParallelism() {
    return -1;
  }

  /**
   * Override the parallelism if operations require differently
   *
   * @return new parallelism
   */
  protected <K> int calculateParallelism(BaseTLink<K> parent) {
    int p;
    if (parent.overrideParallelism() != -1) {
      p = parent.overrideParallelism();
      LOG.log(Level.WARNING, String.format("Overriding parallelism "
          + "specified %d override value %d", parallel, p));
    } else {
      p = parallel;
    }
    return p;
  }

  protected String generateName(String prefix, BaseTLink parent) {
    if (name != null) {
      return name;
    } else {
      if (parent == null) {
        return prefix + "-" + new Random().nextInt(100);
      } else {
        return prefix + "-" + parent.getName();
      }
    }
  }
}
