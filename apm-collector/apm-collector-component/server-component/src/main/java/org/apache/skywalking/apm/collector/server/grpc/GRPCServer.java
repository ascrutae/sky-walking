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
 *
 */


package org.apache.skywalking.apm.collector.server.grpc;

import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyServerBuilder;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;

import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import org.apache.skywalking.apm.collector.server.Server;
import org.apache.skywalking.apm.collector.server.ServerException;
import org.apache.skywalking.apm.collector.server.ServerHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author peng-yongsheng, wusheng
 */
public class GRPCServer implements Server {

    private final Logger logger = LoggerFactory.getLogger(GRPCServer.class);

    private final String host;
    private final int port;
    private io.grpc.Server server;
    private NettyServerBuilder nettyServerBuilder;
    private SslContextBuilder sslContextBuilder;

    public GRPCServer(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * Require for `server.crt` and `server.pem` for open ssl at server side.
     *
     * @param host
     * @param port
     * @param certChainFile  `server.crt` file
     * @param privateKeyFile `server.pem` file
     */
    public GRPCServer(String host, int port, File certChainFile, File privateKeyFile) {
        this.host = host;
        this.port = port;
        this.sslContextBuilder = SslContextBuilder.forServer(certChainFile,
                privateKeyFile);
    }

    @Override
    public String hostPort() {
        return host + ":" + port;
    }

    @Override
    public String serverClassify() {
        return "Google-RPC";
    }

    @Override
    public void initialize() throws ServerException {
        InetSocketAddress address = new InetSocketAddress(host, port);
        nettyServerBuilder = NettyServerBuilder.forAddress(address);
        logger.info("Server started, host {} listening on {}", host, port);
    }

    @Override
    public void start() throws ServerException {
        try {
            if (sslContextBuilder != null) {
                nettyServerBuilder = nettyServerBuilder.sslContext(
                        GrpcSslContexts.configure(sslContextBuilder,
                                SslProvider.OPENSSL).build());
            }
            server = nettyServerBuilder.build();
            server.start();
        } catch (IOException e) {
            throw new GRPCServerException(e.getMessage(), e);
        }
    }

    @Override
    public void addHandler(ServerHandler handler) {
        nettyServerBuilder.addService((io.grpc.BindableService) handler);
    }
}
