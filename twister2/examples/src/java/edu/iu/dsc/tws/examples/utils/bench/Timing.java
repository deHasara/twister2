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
package edu.iu.dsc.tws.examples.utils.bench;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class Timing {

  private static volatile Map<String, List<Long>> timestamps = new ConcurrentHashMap<>();

  private Timing() {
  }

  /**
   * This method will allocate initial capacity required for array list
   */
  public static void defineFlag(String flag, int size, boolean accept) {
    if (accept) {
      timestamps.put(flag, new ArrayList<>(size));
    }
  }

  public static void mark(String flag, TimingUnit unit, boolean accept) {
    if (accept) {
      timestamps.computeIfAbsent(flag, s -> new ArrayList<>())
          .add(unit.getTime());
    }
  }

  public static void markMili(String flag, boolean accept) {
    mark(flag, TimingUnit.MILLI_SECONDS, accept);
  }

  public static void markNano(String flag, boolean accept) {
    mark(flag, TimingUnit.NANO_SECONDS, accept);
  }

  private static void verifyTwoFlags(String flagA, String flagB) {
    if (timestamps.get(flagA).size() != timestamps.get(flagB).size()) {
      throw new RuntimeException(
          "Collected data for two flags mismatches. FlagA : " + timestamps.get(flagA).size()
              + " , FlagB : " + timestamps.get(flagB).size()
      );
    }
  }

  public static double averageDiff(String flagA, String flagB, boolean accept) {
    if (!accept) {
      return -1;
    }
    verifyTwoFlags(flagA, flagB);

    List<Long> flagALongs = timestamps.get(flagA);
    List<Long> flagBLongs = timestamps.get(flagB);

    BigDecimal totalDiffs = BigDecimal.ZERO;
    for (int i = 0; i < flagALongs.size(); i++) {
      totalDiffs = BigDecimal.valueOf(flagBLongs.get(i))
          .subtract(BigDecimal.valueOf(flagALongs.get(i)));
    }

    return totalDiffs.divide(
        BigDecimal.valueOf(flagALongs.size())
    ).doubleValue();
  }

  public static List<Long> diffs(String flagA, String flagB, boolean accept) {
    if (!accept) {
      return Collections.emptyList();
    }
    verifyTwoFlags(flagA, flagB);

    List<Long> flagALongs = timestamps.get(flagA);
    List<Long> flagBLongs = timestamps.get(flagB);

    List<Long> diffs = new ArrayList<>(flagALongs.size());

    for (int i = 0; i < flagALongs.size(); i++) {
      diffs.add(flagBLongs.get(i) - flagALongs.get(i));
    }

    return diffs;
  }

}
