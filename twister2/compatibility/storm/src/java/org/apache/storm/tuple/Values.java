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
package org.apache.storm.tuple;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A convenience class for making tuple values using new Values("field1", 2, 3)
 * syntax.
 */
public class Values extends ArrayList<Object> {
  public Values() {

  }

  public Values(Object... vals) {
    super(vals.length);
    for (Object o : vals) {
      add(o);
    }
  }

  public Values(List vals) {
    this.addAll(vals);
  }

  public Values(Iterator itr) {
    while (itr.hasNext()) {
      this.add(itr.next());
    }
  }
}