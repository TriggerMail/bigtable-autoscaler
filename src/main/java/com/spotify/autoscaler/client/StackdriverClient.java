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

package com.spotify.autoscaler.client;

import com.google.cloud.monitoring.v3.MetricServiceClient;
import com.google.monitoring.v3.ListTimeSeriesRequest;
import com.google.monitoring.v3.Point;
import com.google.monitoring.v3.ProjectName;
import com.google.monitoring.v3.TimeInterval;
import com.google.monitoring.v3.TimeSeries;
import com.google.monitoring.v3.TypedValue;
import com.google.protobuf.Timestamp;
import com.spotify.autoscaler.db.BigtableCluster;
import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import javax.validation.constraints.NotNull;

public class StackdriverClient implements Closeable {

  private static final String CPU_METRIC =
      "metric.type=\"bigtable.googleapis.com/cluster/cpu_load\""
          + " AND resource.labels.instance=\"%s\" AND resource.labels.cluster=\"%s\"";

  private static final String DISK_USAGE_METRIC =
      "metric.type=\"bigtable.googleapis.com/cluster/storage_utilization\""
          + " AND resource.labels.instance=\"%s\" AND resource.labels.cluster=\"%s\"";

  private final MetricServiceClient metricServiceClient;

  public StackdriverClient() throws IOException {
    metricServiceClient = MetricServiceClient.create();
  }

  public Double getDiskUtilization(final BigtableCluster bigtableCluster, final Duration duration) {

    final MetricServiceClient.ListTimeSeriesPagedResponse response =
        metricServiceClient.listTimeSeries(
            ProjectName.of(bigtableCluster.projectId()),
            String.format(
                DISK_USAGE_METRIC, bigtableCluster.instanceId(), bigtableCluster.clusterId()),
            interval(duration),
            ListTimeSeriesRequest.TimeSeriesView.FULL);
    return maxValueFromPagedResponse(response, 0.0, TypedValue::getDoubleValue);
  }

  public Double getCpuLoad(final BigtableCluster bigtableCluster, final Duration duration) {
    final MetricServiceClient.ListTimeSeriesPagedResponse response =
        metricServiceClient.listTimeSeries(
            ProjectName.of(bigtableCluster.projectId()),
            String.format(CPU_METRIC, bigtableCluster.instanceId(), bigtableCluster.clusterId()),
            interval(duration),
            ListTimeSeriesRequest.TimeSeriesView.FULL);
    return maxValueFromPagedResponse(response, 0.0, TypedValue::getDoubleValue);
  }

  private <T extends Comparable<T>> T maxValueFromPagedResponse(
      final MetricServiceClient.ListTimeSeriesPagedResponse response,
      final T initialMax,
      final Function<TypedValue, T> converter) {
    T max = initialMax;
    for (final MetricServiceClient.ListTimeSeriesPage page : response.iteratePages()) {
      for (final TimeSeries ts : page.getValues()) {
        for (final Point p : ts.getPointsList()) {
          final T value = converter.apply(p.getValue());
          max = value.compareTo(max) > 0 ? value : max;
        }
      }
    }
    return max;
  }

  public static TimeInterval interval(final Duration duration) {
    return getTimeInterval(duration);
  }

  @NotNull
  private static TimeInterval getTimeInterval(final Duration duration) {
    final long currentTimeSeconds = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());

    final Timestamp pastTimestamp =
        Timestamp.newBuilder().setSeconds(currentTimeSeconds - duration.getSeconds()).build();
    final Timestamp currentTimestamp =
        Timestamp.newBuilder().setSeconds(currentTimeSeconds).build();
    return TimeInterval.newBuilder()
        .setStartTime(pastTimestamp)
        .setEndTime(currentTimestamp)
        .build();
  }

  @Override
  public void close() throws IOException {
    try {
      metricServiceClient.close();
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }
}
