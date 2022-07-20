package py.app.thrift;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.Validate;
import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.TCompactProtocol;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import py.app.AppEngine;
import py.app.context.AppContext;
import py.app.healthcheck.HealthChecker;
import py.common.NamedThreadFactory;
import py.common.rpc.server.NettyServerConfig;
import py.common.rpc.server.NettyServerConfigBuilder;
import py.common.rpc.server.NettyServerTransport;
import py.common.rpc.server.ThriftServerDef;
import py.common.rpc.server.ThriftServerDefBuilder;
import py.common.rpc.server.ThriftServerDefBuilderBase;
import py.common.rpc.share.TDuplexProtocolFactory;
import py.common.struct.EndPoint;
import py.instance.PortType;

public class ThriftAppEngine implements AppEngine {
    private final static Logger logger = LoggerFactory.getLogger(ThriftAppEngine.class);
    private final static int MAX_DEFAULT_REQUEST_THREAD_COUNT = Runtime.getRuntime().availableProcessors() * 4;// 15;
    private final static String LOCAL_HOST = "0.0.0.0";
    private int maxNumThreads;
    private int minNumThreads;
    private int numWorkerThreads;

    private final TProcessor processor;
    private final ThriftProcessorFactory processorFactory;
    private AppContext context;

    /**
     * every IO can contain the max amount of data, if you set to be 0, then a default value will be set.
     */
    private int maxNetworkFrameSize = ThriftServerDefBuilderBase.MAX_FRAME_SIZE;

    private HealthChecker healthChecker;
    private List<NettyServerTransport> servers;

    public ThriftAppEngine(ThriftProcessorFactory processorFactory, boolean blocking) {
        this.processorFactory = processorFactory;
        this.processor = processorFactory.getProcessor();
        this.servers = new ArrayList<NettyServerTransport>();
    }

    public ThriftAppEngine(ThriftProcessorFactory processorFactory) {
        this(processorFactory, false);
    }

    @Override
    public void start() throws Exception {
        Thread.setDefaultUncaughtExceptionHandler(
                (t, e) -> logger.error("uncaught exception for thread {}", t.getName(), e));
        Validate.notNull(processor, "TProcessor can't be null");
        try {

            Map<PortType, EndPoint> endpoints = context.getEndPointsThrift();
            if (endpoints.isEmpty()) {
                String errMsg = "there is no service on instance: " + context + ", so can't start up";
                logger.error(errMsg);
                throw new Exception(errMsg);
            }

            int numProcessors = Runtime.getRuntime().availableProcessors();
            for (Map.Entry<PortType, EndPoint> entry : endpoints.entrySet()) {
                EndPoint endpoint = entry.getValue();
                ThriftServerDefBuilder thriftServerDefBuilder = ThriftServerDef.newBuilder();
                thriftServerDefBuilder.hostname(LOCAL_HOST);
                thriftServerDefBuilder.listen(endpoint.getPort());

                thriftServerDefBuilder.protocol(TDuplexProtocolFactory
                        .fromSingleFactory(new TCompactProtocol.Factory()));
                thriftServerDefBuilder.withProcessor(processor);

                String processorName = processor.getClass().getEnclosingClass().getSimpleName();
                processorName += "-" + entry.getKey().name();

                if (maxNumThreads == 0) {
                    maxNumThreads = MAX_DEFAULT_REQUEST_THREAD_COUNT;
                }

                if (minNumThreads == 0) {
                    minNumThreads = numProcessors;
                }

                if (numWorkerThreads == 0) {
                    numWorkerThreads = numProcessors;
                }

                ThreadPoolExecutor threadPool = new ThreadPoolExecutor(minNumThreads, maxNumThreads, 60L,
                        TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), new NamedThreadFactory(processorName
                                + "-request-worker-"));
                thriftServerDefBuilder.using(threadPool);
                logger.info("i am ThriftAppEngine, Max Network Frame Size is :{}", maxNetworkFrameSize);
                thriftServerDefBuilder.limitFrameSizeTo(maxNetworkFrameSize);

                /**
                 * A handler for rejected tasks that runs the rejected task directly in the calling thread of the
                 * {@code execute} method, unless the executor has been shut down, in which case the task is discarded.
                 */
                threadPool.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());

                NettyServerConfigBuilder configBuilder = NettyServerConfig.newBuilder();
                /**
                 * because the work thread is just for dispatching the task to request worker pool, so only set one
                 * thread.
                 */
                configBuilder.setWorkerThreadCount(numWorkerThreads);
                configBuilder.setBossThreadCount(1);

                configBuilder.setNiftyName(processorName);
                NettyServerConfig nettyServerConfig = configBuilder.build();
                NettyServerTransport server = new NettyServerTransport(thriftServerDefBuilder.build(),
                        nettyServerConfig, new DefaultChannelGroup());

                server.start();

                logger.warn(
                        "server({}) has started, boss count: {}, worker count: {}, server request min thread: {}, max thread: {}",
                        endpoint, nettyServerConfig.getBossThreadCount(), nettyServerConfig.getWorkerThreadCount(),
                        minNumThreads, maxNumThreads);
                servers.add(server);
            }

        } catch (Exception e) {
            logger.error("caught an exception ", e);
            throw e;
        }

        if (healthChecker != null) {
            logger.warn("start health checker");
            healthChecker.startHealthCheck();
        }

    }

    public void setHealthChecker(HealthChecker healthChecker) {
        this.healthChecker = healthChecker;
    }

    @Override
    public void stop() {
        /**
         * stop to check health of service, because there are a lot of errors in logger file due to the closed of
         * service
         */
        if (healthChecker != null) {
            healthChecker.stopHealthCheck();
            healthChecker = null;
        }
        logger.warn("going to stop servers:{}", servers);
        for (NettyServerTransport server : servers) {

            try {
                logger.warn("shut down server, port is {}", server.getPort());
                server.stop();
            } catch (InterruptedException e) {
                logger.warn("stop server: " + server.getServerChannel().getLocalAddress() + " failure", e);
            }

            int count = 5;
            while (server.getServerChannel().isBound() && count != 0) {
                logger.warn("server is still being stopped. Sleep 1 second");
                try {
                    Thread.sleep(1000l);
                } catch (InterruptedException e) {
                    logger.warn("caught an exception", e);
                }
                count--;
            }

            if (server.getServerChannel().isBound()) {
                logger.warn("The service " + server.getServerChannel().getLocalAddress() + " is still running");
            } else {
                logger.warn("The service " + server.getServerChannel().getLocalAddress() + " has been stopped");
            }
        }
        logger.warn("all servers have been stopped");
    }

    public void setContext(AppContext context) {
        this.context = context;
    }

    public AppContext getContext() {
        return context;
    }

    public int getMaxNumThreads() {
        return maxNumThreads;
    }

    public void setMaxNumThreads(int maxNumThreads) {
        this.maxNumThreads = maxNumThreads;
    }

    public int getMinNumThreads() {
        return minNumThreads;
    }

    public void setMinNumThreads(int minNumThreads) {
        this.minNumThreads = minNumThreads;
    }

    public void setMaxNetworkFrameSize(int maxNetworkFrameSize) {
        this.maxNetworkFrameSize = maxNetworkFrameSize;
    }

    public int getMaxNetworkFrameSize() {
        return maxNetworkFrameSize;
    }

    public int getNumWorkerThreads() {
        return numWorkerThreads;
    }

    public void setNumWorkerThreads(int numWorkerThreads) {
        this.numWorkerThreads = numWorkerThreads;
    }

    public ThriftProcessorFactory getProcessorFactory() {
        return processorFactory;
    }

    public HealthChecker getHealthChecker() {
        return healthChecker;
    }
}
