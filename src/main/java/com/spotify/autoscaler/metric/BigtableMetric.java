/*-
 * -\-\-
 * bigtable-autoscaler
 * --
 * Copyright (C) 2018 Spotify AB
 * --
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -/-/-
 */

package com.spotify.autoscaler.metric;

import com.codahale.metrics.Gauge;
import com.spotify.autoscaler.ClusterData;
import java.util.ArrayList;
import java.util.List;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class BigtableMetric {

  public static List<String> getAllMetrics() {
    List<String> metrics = new ArrayList<>();
    for (Metrics metric : Metrics.values()) {
      metrics.add(metric.tag);
    }
    for (LoadMetricType loadMetricType : LoadMetricType.values()) {
      metrics.add(loadMetricType.tag);
    }
    for (ErrorCode errorCode : ErrorCode.values()) {
      metrics.add(errorCode.tag);
    }
    return metrics;
  }

  public enum Metrics {
    NODE_COUNT("node-count") {
      @Override
      public Gauge getMetricValue(final ClusterData clusterData) {
        return clusterData::getCurrentNodeCount;
      }
    },
    MIN_NODE_COUNT("min-node-count") {
      @Override
      public Gauge getMetricValue(final ClusterData clusterData) {
        return clusterData::getMinNodeCount;
      }
    },
    MAX_NODE_COUNT("max-node-count") {
      @Override
      public Gauge getMetricValue(final ClusterData clusterData) {
        return clusterData::getMaxNodeCount;
      }
    },
    EFFECTIVE_MIN_NODE_COUNT("effective-min-node-count") {
      @Override
      public Gauge getMetricValue(final ClusterData clusterData) {
        return clusterData::getEffectiveMinNodeCount;
      }
    },
    LAST_CHECK_TIME("last-check-time") {
      @Override
      public Gauge getMetricValue(final ClusterData clusterData) {
        return null;
      }
    },
    CPU_TARGET_RATIO("cpu-target-ratio") {
      @Override
      public Gauge getMetricValue(final ClusterData clusterData) {
        return null;
      }
    };

    public String tag;

    Metrics(final String tag) {
      this.tag = tag;
    }

    public Gauge getMetricValue(final ClusterData clusterData) {
      throw new NotImplementedException();
    }
  }

  public enum LoadMetricType {
    CPU("cpu-util"),
    STORAGE("storage-util");

    public String tag;

    LoadMetricType(final String tag) {
      this.tag = tag;
    }
  }

  public enum ErrorCode {
    CONSECUTIVE_FAILURE_COUNT("consecutive-failure-count");

    public String tag;

    ErrorCode(final String tag) {
      this.tag = tag;
    }
  }
}
