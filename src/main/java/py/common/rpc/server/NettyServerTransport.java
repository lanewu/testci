/*
 * Copyright (C) 2012-2013 Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package py.common.rpc.server;

import org.apache.log4j.Logger;
import org.apache.thrift.protocol.TProtocolFactory;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ServerChannelFactory;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerBossPool;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioWorkerPool;
import org.jboss.netty.handler.timeout.IdleStateHandler;
import org.jboss.netty.util.ExternalResourceReleasable;
import org.jboss.netty.util.ThreadNameDeterminer;

import py.common.rpc.share.ShutdownUtil;

import javax.inject.Inject;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A core channel the decode framed Thrift message, dispatches to the TProcessor given and then encode message back to
 * Thrift frame.
 */
public class NettyServerTransport implements ExternalResourceReleasable {
    private static final Logger log = Logger.getLogger(NettyServerTransport.class);

    private final int port;
    private final String hostname;

    private final ChannelPipelineFactory pipelineFactory;
    private static final int NO_WRITER_IDLE_TIMEOUT = 0;
    private static final int NO_ALL_IDLE_TIMEOUT = 0;
    private ServerBootstrap bootstrap;
    private final ChannelGroup allChannels;
    private ExecutorService bossExecutor;
    private ExecutorService ioWorkerExecutor;
    private ServerChannelFactory channelFactory;
    private Channel serverChannel;
    private final ThriftServerDef def;
    private final NettyServerConfig nettyServerConfig;
    private final ChannelStatistics channelStatistics;

    public NettyServerTransport(final ThriftServerDef def) {
        this(def, NettyServerConfig.newBuilder().build(), new DefaultChannelGroup());
    }

    @Inject
    public NettyServerTransport(final ThriftServerDef def, final NettyServerConfig nettyServerConfig,
                    final ChannelGroup allChannels) {
        this.def = def;
        this.nettyServerConfig = nettyServerConfig;
        this.hostname = def.getHostname();
        this.port = def.getServerPort();
        this.allChannels = allChannels;
        // connectionLimiter must be instantiated exactly once (and thus outside the pipeline factory)
        final ConnectionLimiter connectionLimiter = new ConnectionLimiter(def.getMaxConnections());

        this.channelStatistics = new ChannelStatistics(allChannels);

        this.pipelineFactory = new ChannelPipelineFactory() {
            @Override
            public ChannelPipeline getPipeline() throws Exception {
                ChannelPipeline cp = Channels.pipeline();
                TProtocolFactory inputProtocolFactory = def.getDuplexProtocolFactory().getInputProtocolFactory();
                NiftySecurityHandlers securityHandlers = def.getSecurityFactory().getSecurityHandlers(def,
                                nettyServerConfig);
                cp.addLast("connectionContext", new ConnectionContextHandler());
                cp.addLast("connectionLimiter", connectionLimiter);
                cp.addLast("frameCodec",
                                def.getThriftFrameCodecFactory().create(def.getMaxFrameSize(), inputProtocolFactory));
                if (def.getClientIdleTimeout() != null) {
                    // Add handlers to detect idle client connections and disconnect them
                    cp.addLast("idleTimeoutHandler", new IdleStateHandler(nettyServerConfig.getTimer(), (long) def
                                    .getClientIdleTimeout().toMillis(), NO_WRITER_IDLE_TIMEOUT, NO_ALL_IDLE_TIMEOUT,
                                    TimeUnit.MILLISECONDS));
                    cp.addLast("idleDisconnectHandler", new IdleDisconnectHandler());
                }

                cp.addLast("authHandler", securityHandlers.getAuthenticationHandler());
                cp.addLast("dispatcher", new NiftyDispatcher(def));
                return cp;
            }
        };
    }

    public void start() {
        bossExecutor = nettyServerConfig.getBossExecutor();
        int bossThreadCount = nettyServerConfig.getBossThreadCount();
        ioWorkerExecutor = nettyServerConfig.getWorkerExecutor();
        int ioWorkerThreadCount = nettyServerConfig.getWorkerThreadCount();

        channelFactory = new NioServerSocketChannelFactory(new NioServerBossPool(bossExecutor, bossThreadCount,
                        ThreadNameDeterminer.CURRENT), new NioWorkerPool(ioWorkerExecutor, ioWorkerThreadCount,
                        ThreadNameDeterminer.CURRENT));
        start(channelFactory);
        log.info("worker count: " + ioWorkerThreadCount + ", boss count: " + bossThreadCount + ", server port: "
                        + def.getServerPort());
    }

    public void start(ServerChannelFactory serverChannelFactory) {
        bootstrap = new ServerBootstrap(serverChannelFactory);
        bootstrap.setOptions(nettyServerConfig.getBootstrapOptions());
        bootstrap.setPipelineFactory(pipelineFactory);
        serverChannel = bootstrap.bind(new InetSocketAddress(hostname, port));
        SocketAddress actualSocket = serverChannel.getLocalAddress();
        if (actualSocket instanceof InetSocketAddress) {
            int actualPort = ((InetSocketAddress) actualSocket).getPort();
            log.info("started transport" + ", name: " + def.getName() + ", actualPort: " + actualPort + ", port: "
                            + port);
        } else {
            log.info("started transport" + ", name: " + def.getName() + ", port: " + port);
        }
    }

    public void stop() throws InterruptedException {
        if (serverChannel != null) {
            log.warn("stopping transport" + ", name:" + def.getName() + ", port: " + port);
            // first stop accepting
            final CountDownLatch latch = new CountDownLatch(1);
            serverChannel.close().addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    // stop and process remaining in-flight invocations
                    log.warn("shutdown server: " + def.getName() + ":" + port + " finished");
                    latch.countDown();
                }
            });

            latch.await();
            if (def.getExecutor() instanceof ExecutorService) {
                ExecutorService exe = (ExecutorService) def.getExecutor();
                ShutdownUtil.shutdownExecutor(exe, "dispatcher");
            }
        }

        // If the channelFactory was created by us, we should also clean it up. If the
        // channelFactory was passed in by NiftyBootstrap, then it may be shared so don't clean
        // it up.
        if (channelFactory != null) {
            ShutdownUtil.shutdownChannelFactory(channelFactory, bossExecutor, ioWorkerExecutor, allChannels);
        }
    }

    public Channel getServerChannel() {
        return serverChannel;
    }

    @Override
    public void releaseExternalResources() {
        bootstrap.releaseExternalResources();
    }

    private static class ConnectionLimiter extends SimpleChannelUpstreamHandler {
        private final AtomicInteger numConnections;
        private final int maxConnections;

        public ConnectionLimiter(int maxConnections) {
            this.maxConnections = maxConnections;
            this.numConnections = new AtomicInteger(0);
        }

        @Override
        public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
            if (maxConnections > 0) {
                if (numConnections.incrementAndGet() > maxConnections) {
                    ctx.getChannel().close();
                    // numConnections will be decremented in channelClosed
                    log.info("Accepted connection above limit ({" + maxConnections + "}). Dropping.");
                }
            }
            super.channelOpen(ctx, e);
        }

        @Override
        public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
            if (maxConnections > 0) {
                if (numConnections.decrementAndGet() < 0) {
                    log.error("BUG in ConnectionLimiter");
                }
            }
            super.channelClosed(ctx, e);
        }
    }

    public NiftyMetrics getMetrics() {
        return channelStatistics;
    }
    
    public int getPort() {
        return this.port;
    }
}
