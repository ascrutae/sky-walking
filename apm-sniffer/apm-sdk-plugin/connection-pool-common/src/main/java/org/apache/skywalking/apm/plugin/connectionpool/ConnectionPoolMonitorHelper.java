/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.skywalking.apm.plugin.connectionpool;

import org.apache.skywalking.apm.plugin.jdbc.trace.ConnectionInfo;
import org.apache.skywalking.apm.util.StringUtil;

import java.text.MessageFormat;

public class ConnectionPoolMonitorHelper {
    public static final String POOL_ID_PATTERN = "{0}-{1}/{2}";

    public static <T> ConnectionPoolInfo monitor(final String framework, final ConnectionInfo connectionInfo,
                                                 final PoolCapabilityMetricValueRecorder<T> recorder) {
        String poolId = MessageFormat.format(
                POOL_ID_PATTERN, framework, connectionInfo.getDatabasePeer(), connectionInfo.getDatabaseName());

        if (StringUtil.isBlank(framework) || recorder == null || connectionInfo == null) {
            throw new IllegalArgumentException("framework , recorder or connectionInfo is null");
        }

        return new ConnectionPoolInfoImpl(poolId, recorder);
    }
}