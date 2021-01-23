/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.skywalking.apm.plugin.connectionpool;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.skywalking.apm.agent.core.boot.ServiceManager;
import org.apache.skywalking.apm.agent.core.meter.Gauge;
import org.apache.skywalking.apm.agent.core.meter.Histogram;
import org.apache.skywalking.apm.agent.core.meter.MeterId;
import org.apache.skywalking.apm.agent.core.meter.MeterService;

public class ConnectionPoolInfoImpl implements ConnectionPoolInfo {
    public static final String POOL_ID = "pool_id";
    public static final String CONNECTION_POOL_ACTIVE_COUNTS = "connection_pool_active_counts";
    public static final String CONNECTION_POOL_GET_CONNECTION_LATENCY = "connection_pool_get_connection_latency";
    public static final String CONNECTION_POOL_AWAITING_CONNECTION_THREAD_NUM = "connection_pool_awaiting_connection_thread_num";
    public static final String CONNECTION_POOL_GET_CONNECTION_FAILURE_RATE = "connection_pool_get_connection_failure_rate";
    private static final List<Double> STEPS = Arrays.asList(
        10d, 50d, 100d, 200d, 400d, 800d, 1000d, 1500d, 2000d, 2500d, 3000d, 4000d, 5000d, 10000d, 50000d,
        100000d, 500000d, 1000000d, 5000000d
    );

    private final String poolId;
    private final List<MeterId> meterIds = new ArrayList<>();
    private final PoolCapabilityMetricValueRecorder recorder;
    private Histogram getConnectionLatencyHistogram;
    private FailureRateSupplier failureRateSupplier;

    public ConnectionPoolInfoImpl(final String poolId, PoolCapabilityMetricValueRecorder recorder) {
        this.poolId = poolId;
        this.recorder = recorder;
        registerToMeterSystem(ServiceManager.INSTANCE.findService(MeterService.class));
    }

    private void registerToMeterSystem(MeterService meterService) {
        this.meterIds.addAll(Arrays.asList(
            registerActiveCountMeter(meterService),
            registerAwaitConnectionThreadNumMeter(meterService),

            registerGetConnectionLatencyMeter(meterService),
            registerGetConnectionFailureRateMeter(meterService)
        ));
    }

    private void unregisterFromMeterSystem() {
        this.meterIds.forEach(meterId -> ServiceManager.INSTANCE.findService(MeterService.class).unregister(meterId));
    }

    @Override
    public void recordGetConnectionTime(final long time) {
        this.getConnectionLatencyHistogram.addValue(time);
    }

    @Override
    public void recordGetConnectionStatue(final boolean failed) {
        this.failureRateSupplier.recordGetConnectionStatue(failed);
    }

    private MeterId registerAwaitConnectionThreadNumMeter(final MeterService meterService) {
        return meterService.register(new Gauge.Builder(CONNECTION_POOL_AWAITING_CONNECTION_THREAD_NUM, () -> {
            try {
                return recorder.getAwaitConnectionThreadNumber();
            } catch (ObjectHadBeenRecycledException e) {
                unregisterFromMeterSystem();
                return 0d;
            }
        }).tag(POOL_ID, poolId).build()).getId();
    }

    private MeterId registerActiveCountMeter(final MeterService meterService) {
        return meterService.register(new Gauge.Builder(CONNECTION_POOL_ACTIVE_COUNTS, () -> {
            try {
                return recorder.getActiveConnections();
            } catch (ObjectHadBeenRecycledException e) {
                unregisterFromMeterSystem();
                return 0d;
            }
        }).tag(POOL_ID, poolId).build()).getId();
    }

    private MeterId registerGetConnectionLatencyMeter(final MeterService meterService) {
        this.getConnectionLatencyHistogram = new Histogram.Builder(CONNECTION_POOL_GET_CONNECTION_LATENCY)
            .tag(POOL_ID, poolId).steps(STEPS).build();
        return meterService.register(this.getConnectionLatencyHistogram).getId();
    }

    private MeterId registerGetConnectionFailureRateMeter(final MeterService meterService) {
        this.failureRateSupplier = new FailureRateSupplier();
        return meterService.register(new Gauge.Builder(CONNECTION_POOL_GET_CONNECTION_FAILURE_RATE, failureRateSupplier)
            .tag(POOL_ID, poolId).build()).getId();
    }
}
