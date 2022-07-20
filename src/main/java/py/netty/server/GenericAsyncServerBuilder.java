package py.netty.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.common.struct.EndPoint;
import py.consumer.ConsumerService;
import py.consumer.MultiThreadConsumerServiceWithBlockingQueue;
import py.netty.core.ByteToMessageDecoder;
import py.netty.core.IOEventThreadsMode;
import py.netty.core.IOEventUtils;
import py.netty.core.ProtocolFactory;
import py.netty.core.TransferenceConfiguration;
import py.netty.core.TransferenceOption;
import py.netty.core.twothreads.AbstractChannel;
import py.netty.core.twothreads.TTEventGroup;
import py.netty.core.twothreads.TTServerSocketChannel;
import py.netty.memory.PooledByteBufAllocatorWrapper;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static py.netty.server.AsyncRequestHandler.counterPendingRequestsInRPC;

public class GenericAsyncServerBuilder {
    private static final Logger logger = LoggerFactory.getLogger(GenericAsyncServerBuilder.class);
    public static final int MAX_IO_PENDING_REQUESTS = 1000;
    private Object serviceObject;
    private ProtocolFactory protocolFactory;

    // use configuration to configure # of io threads
    private TransferenceConfiguration configuration;
    private ChannelFuture serverChannelFuture;
    private int ioThreadCount;
    private int maxIoPendingRequests = MAX_IO_PENDING_REQUESTS;
    private ByteBufAllocator allocator = PooledByteBufAllocatorWrapper.INSTANCE;

    // a thread pool for executing the logic of processing requests
    private final ConsumerService<Runnable> executorForProcessingRequests;
    // The boss thread
    private final EventLoopGroup parentEventGroup;
    // A group of io handler threads
    //private final MyEventGroup ioEventGroup;
    private final EventLoopGroup ioEventGroup;
    //    private final EventLoopGroup ioEventGroup;

    private py.common.Counter counterPendingRequests;

    public GenericAsyncServerBuilder(Object serviceObject, ProtocolFactory protocolFactory, TransferenceConfiguration configuration) {
        this.serviceObject = serviceObject;
        this.protocolFactory = protocolFactory;
        this.setConfiguration(configuration);
        //                new ThreadPoolExecutor(numWorkerThreads, numWorkerThreads, 60,
        //                TimeUnit.SECONDS, new LinkedBlockingQueue<>(), new NamedThreadFactory("netty-server-io-worker"));

        //        parentEventGroup = new EpollEventLoopGroup(1,
        //        parentEventGroup = new NioEventLoopGroup(1,
        parentEventGroup = new TTEventGroup(1,
                new DefaultThreadFactory("netty-server-boss", false, Thread.NORM_PRIORITY));

        // the event loop group for processing netty events
        //        final EventLoopGroup ioEventGroup = new EpollEventLoopGroup(ioThreadCount,
        //        ioEventGroup = new NioEventLoopGroup(ioThreadCount,
        // The number of io threads can't exceed the number of available processors. The reason is that we are using
        // PooledByteBufAllocated, within which the default configuration allocates '2 * cores' arenas to reduce
        // contention between Netty io threads (smaller number we will run into hotspots as allocation and deallocation
        // needs to be synchronized on the PoolArena, see https://github.com/netty/netty/issues/3888).
        ioThreadCount = IOEventUtils
            .calculateThreads((IOEventThreadsMode) getConfiguration().valueOf(TransferenceServerOption.IO_EVENT_GROUP_THREADS_MODE),
                (Float) getConfiguration().valueOf(TransferenceServerOption.IO_EVENT_GROUP_THREADS_PARAMETER));

        ioEventGroup = new TTEventGroup(ioThreadCount,
                new DefaultThreadFactory("netty-server-io-worker", false, Thread.NORM_PRIORITY));

        // The reason to start executorForProcessingRequests here is to ensure the TTEventGroup can be started first.
        // Otherwise we need to manually stop threads in executorForProcessingRequests
        int numWorkerThreads = IOEventUtils
            .calculateThreads((IOEventThreadsMode) getConfiguration().valueOf(TransferenceServerOption.IO_EVENT_HANDLE_THREADS_MODE),
                (Float) getConfiguration().valueOf(TransferenceServerOption.IO_EVENT_HANDLE_THREADS_PARAMETER));
        this.executorForProcessingRequests = new MultiThreadConsumerServiceWithBlockingQueue<>(numWorkerThreads,
                Runnable::run, new LinkedBlockingQueue<>(), "netty-server-io-handler");
        ((MultiThreadConsumerServiceWithBlockingQueue<Runnable>) this.executorForProcessingRequests)
                .setCounter(n -> counterPendingRequestsInRPC.incCounter(n));
        this.executorForProcessingRequests.start();
    }

    public GenericAsyncServer build(EndPoint endPoint) throws InterruptedException {
        int backLog = (int) getConfiguration().valueOf(TransferenceServerOption.IO_SERVER_SO_BACKLOG);
        logger.warn(">>>>server back log:{}<<<<", backLog);
        int maxFrameSize = (int) getConfiguration().valueOf(TransferenceOption.MAX_MESSAGE_LENGTH);

        protocolFactory.setByteBufAllocator(allocator);

        ServerBootstrap bootStrap = new ServerBootstrap();
        final AtomicInteger pendingIoRequests = new AtomicInteger(0);
        bootStrap.group(parentEventGroup, ioEventGroup).channel(TTServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, backLog).childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.WRITE_SPIN_COUNT, 50).childOption(ChannelOption.SO_KEEPALIVE, true)
                .childHandler(new ChannelInitializer<AbstractChannel>() {
                    @Override
                    protected void initChannel(AbstractChannel ch) throws Exception {
                        ByteToMessageDecoder byteToMessageDecoder = new ByteToMessageDecoder(maxFrameSize, allocator);
                        // Inbound
                        ch.pipeline().addLast(byteToMessageDecoder);
                        AsyncRequestHandler asyncRequestHandler = new AsyncRequestHandler(serviceObject,
                                protocolFactory.getProtocol());
                        asyncRequestHandler.setMaxPendingRequestsCount(maxIoPendingRequests);
                        asyncRequestHandler.setPendingRequestsCount(pendingIoRequests);
                        asyncRequestHandler.setExecutor(executorForProcessingRequests);
                        if (counterPendingRequests != null) {
                            asyncRequestHandler.setCounterPendingRequests(counterPendingRequests);
                        }

                        // Inbound
                        ch.pipeline().addLast(asyncRequestHandler);
                    }
                });

        bootStrap.childOption(ChannelOption.ALLOCATOR, allocator);
        int maxRcvBuf = (int)configuration.valueOf(TransferenceOption.MAX_BYTES_ONCE_ALLOCATE);
        // bootStrap.childOption(ChannelOption.SO_RCVBUF, 256 * 1024);
        bootStrap.childOption(ChannelOption.RCVBUF_ALLOCATOR, new AdaptiveRecvByteBufAllocator(512, 8192, maxRcvBuf));
        serverChannelFuture = bootStrap.bind(endPoint.getHostName(), endPoint.getPort()).sync();

        logger.warn("start to listen on port: {}, io thread count: {}", endPoint, ioThreadCount);
        return new GenericAsyncServer() {
            @Override
            public void shutdown() {
                logger.warn("stop to listen on port: {}", endPoint);
                // shutdown two groups if there is no IOs within 2 seconds, or two groups will exit in 5 seconds no matter what
                try {
                    if (parentEventGroup != null) {
                        parentEventGroup.shutdownGracefully(2, 5, TimeUnit.SECONDS);
                        parentEventGroup.awaitTermination(6, TimeUnit.SECONDS);
                    }

                    if (ioEventGroup != null) {
                        ioEventGroup.shutdownGracefully(2, 5, TimeUnit.SECONDS);
                        ioEventGroup.awaitTermination(6, TimeUnit.SECONDS);
                    }
                } catch (InterruptedException e) {
                    logger.warn("Caught an exception", e);
                }

                try {
                    serverChannelFuture.channel().closeFuture().sync();
                } catch (InterruptedException e) {
                    logger.warn("Caught an exception", e);
                }
            }
        };
    }

    public void close() {
        if (executorForProcessingRequests != null) {
            executorForProcessingRequests.stop();
        }
    }

    public <T> GenericAsyncServerBuilder option(TransferenceServerOption<T> option, T value) {
        getConfiguration().option(option, value);
        return this;
    }

    // comment this function out, since the ioThreadCount is used only with the constructor of this class
    // we need to use a configuration to configure the number of io thread count
    //    public void setIoThreadCount(int ioThreadCount) {
    //        this.ioThreadCount = ioThreadCount;
    //    }
    //
    public int getMaxIoPendingRequests() {
        return maxIoPendingRequests;
    }

    public void setMaxIoPendingRequests(int maxIoPendingRequests) {
        this.maxIoPendingRequests = maxIoPendingRequests;
    }

    public static TransferenceConfiguration defaultConfiguration() {
        TransferenceConfiguration configuration = TransferenceConfiguration.defaultConfiguration();
        configuration.option(TransferenceServerOption.IO_SERVER_SO_BACKLOG, 256);
        return configuration;
    }

    public ByteBufAllocator getAllocator() {
        return allocator;
    }

    public void setAllocator(ByteBufAllocator allocator) {
        this.allocator = allocator;
    }

    public TransferenceConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(TransferenceConfiguration configuration) {
        this.configuration = configuration;
    }

    public void setCounterPendingRequests(py.common.Counter counterPendingRequests) {
        this.counterPendingRequests = counterPendingRequests;
    }
}
