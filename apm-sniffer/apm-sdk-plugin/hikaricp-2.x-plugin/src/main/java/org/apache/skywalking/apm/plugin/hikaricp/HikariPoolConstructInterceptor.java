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

package org.apache.skywalking.apm.plugin.hikaricp;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.pool.HikariPool;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceConstructorInterceptor;
import org.apache.skywalking.apm.plugin.connectionpool.ConnectionPoolMonitorHelper;
import org.apache.skywalking.apm.plugin.hikaricp.define.HikariPoolMetricValueRecorderSingle;
import org.apache.skywalking.apm.plugin.jdbc.connectionurl.parser.URLParser;
import org.apache.skywalking.apm.plugin.jdbc.trace.ConnectionInfo;

public class HikariPoolConstructInterceptor implements InstanceConstructorInterceptor {

    public static final String FRAMEWORK = "hikariCP";

    @Override
    public void onConstruct(final EnhancedInstance objInst, final Object[] allArguments) throws Throwable {
        HikariPool hikariPool = (HikariPool) (Object) objInst;
        HikariConfig config = (HikariConfig) allArguments[0];

        ConnectionInfo connectionInfo = URLParser.parser(config.getJdbcUrl());

        objInst.setSkyWalkingDynamicField(new EnhanceObjectHolder(ConnectionPoolMonitorHelper.monitor(
                FRAMEWORK, connectionInfo,
                new HikariPoolMetricValueRecorderSingle(hikariPool)
        )));
    }
}
