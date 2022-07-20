package py.netty.client;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.buffer.ByteBufAllocator;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.Future;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.common.NamedThreadFactory;
import py.common.struct.EndPoint;
import py.connection.pool.BootstrapBuilder;
import py.connection.pool.PYConnection;
import py.connection.pool.PYConnectionPoolImpl;
import py.netty.core.*;
import py.netty.core.twothreads.TTEventGroup;
import py.netty.memory.PooledByteBufAllocatorWrapper;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings({ "rawtypes", "unchecked" }) public class GenericAsyncClientFactory<T> {
    private final static Logger logger = LoggerFactory.getLogger(GenericAsyncClientFactory.class);
    private Class<T> service;
    private ProtocolFactory protocolFactory;

    private final TransferenceConfiguration cfg;

    private HashedWheelTimer hashedWheelTimer;
    private PYConnectionPoolImpl connectionPool;
    private int connectionPoolSize;

    private ThreadLocal<ThreadLocalValue> threadLocal = new ThreadLocal();
    private ByteBufAllocator allocator = PooledByteBufAllocatorWrapper.INSTANCE;
    private BootstrapBuilder bootstrapBuilder;
    // We might be using NioEventLoopGroup (default one in Netty) or EpollEventLoopGroup (native one in Netty) in future
    //    private EpollEventLoopGroup ioEventGroup;
    //    private NioEventLoopGroup ioEventGroup;
    //    private MyEventGroup ioEventGroup;
    private TTEventGroup ioEventGroup;

    // A thread pool for processing responses coming back servers
    private ExecutorService executorForProcessingResponses;

    public GenericAsyncClientFactory(Class<T> serviceInterface, ProtocolFactory protocolFactory,
            TransferenceConfiguration cfg) {
        Validate.isTrue(serviceInterface.isInterface());
        this.service = (Class<T>) findImplementationClass(serviceInterface);
        this.cfg = cfg;
        this.protocolFactory = protocolFactory;

        // We might be using NioEventLoopGroup (default one in Netty) or EpollEventLoopGroup (native one in Netty) in future
        // ioEventGroup = new EpollEventLoopGroup(Runtime.getRuntime().availableProcessors(),
        // ioEventGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors(),

        // The number of io threads can't exceed the number of available processors. The reason is that we are using 
        // PooledByteBufAllocated, within which the default configuration allocates '2 * cores' arenas to reduce 
        // contention between Netty io threads (smaller number we will run into hotspots as allocation and deallocation 
        // needs to be synchronized on the PoolArena, see https://github.com/netty/netty/issues/3888).

        //ioEventGroup = new MyEventGroup(Runtime.getRuntime().availableProcessors(),
        // new DefaultThreadFactory("netty-client-io", false, Thread.NORM_PRIORITY));

        int ioEventGroupThreads = IOEventUtils
            .calculateThreads((IOEventThreadsMode) cfg.valueOf(TransferenceClientOption.CLIENT_IO_EVENT_GROUP_THREADS_MODE),
                (Float) cfg.valueOf(TransferenceClientOption.CLIENT_IO_EVENT_GROUP_THREADS_PARAMETER));
        ioEventGroup = new TTEventGroup(ioEventGroupThreads,
                new DefaultThreadFactory("netty-client-io", false, Thread.NORM_PRIORITY));
        // workerExecutor);
        // ioEventGroup.setIoRatio(100);

        int numberOfThreadsToProcessResponses = IOEventUtils
            .calculateThreads((IOEventThreadsMode) cfg.valueOf(TransferenceClientOption.CLIENT_IO_EVENT_HANDLE_THREADS_MODE),
                (Float) cfg.valueOf(TransferenceClientOption.CLIENT_IO_EVENT_HANDLE_THREADS_PARAMETER));
        executorForProcessingResponses = new ThreadPoolExecutor(numberOfThreadsToProcessResponses,
                numberOfThreadsToProcessResponses, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>(),
                new NamedThreadFactory("netty-client-response-processor"));
        validateService();
    }

    public void validateService() {
        try {
            service.getConstructor(PYConnection.class, Protocol.class);
        } catch (Exception e) {
            logger.error("caught an exception", e);
            throw new RuntimeException();
        }
    }

    public void init() {
        this.hashedWheelTimer = new HashedWheelTimer(
                new ThreadFactoryBuilder().setNameFormat("nett4-wheel-timer-%s").build(), 100, TimeUnit.MILLISECONDS,
                512);
        int ioTimeout = (int) cfg.valueOf(TransferenceClientOption.IO_TIMEOUT_MS);

        protocolFactory.setByteBufAllocator(allocator);

        if (bootstrapBuilder == null) {
            bootstrapBuilder = new BootstrapBuilderImpl(executorForProcessingResponses);
        }

        bootstrapBuilder.setAllocator(allocator);
        bootstrapBuilder.setCfg(cfg);
        bootstrapBuilder.setHashedWheelTimer(hashedWheelTimer);
        bootstrapBuilder.setProtocolFactory(protocolFactory);
        bootstrapBuilder.setIoEventGroup(ioEventGroup);
        // Currently, we think one channel between a client and a server is good enough. We might change 1 to a
        // configurable value later
        connectionPoolSize = (int) cfg.valueOf(TransferenceClientOption.CONNECTION_COUNT_PER_ENDPOINT);
        connectionPoolSize = connectionPoolSize <= 0 ? 1 : connectionPoolSize;
        //connectionPoolSize = 1;
        int maxLastTimeForConnectionMs = (int) cfg.valueOf(TransferenceClientOption.MAX_LAST_TIME_FOR_CONNECTION_MS);
//        maxLastTimeForConnectionMs = 10000000;
        logger.warn(
                "connection pool size is {}\n maxLastTimeForConnectionMS {}\n",
                connectionPoolSize, maxLastTimeForConnectionMs);

        //init the timeout policy Factory.
//        DetectTimeoutPolicyFactory.init(pingHostTimeoutMs, networkDetectRetryMaxTimes);

        this.connectionPool = new PYConnectionPoolImpl(connectionPoolSize, maxLastTimeForConnectionMs,
                bootstrapBuilder);

        //tell the listening port to clint to detect the connection.
        logger.warn("connection pool size: {}. maxLastTimeForConnectionMs={}, ioTimeout={} UDPListenerPort={}",
                connectionPoolSize, maxLastTimeForConnectionMs, ioTimeout);
    }

    /**
     * Build I/O transference client.
     *
     * @return
     */
    public T generate(EndPoint endPoint) {
        ThreadLocalValue value = threadLocal.get();
        if (value == null) {
            value = new ThreadLocalValue();
            threadLocal.set(value);
        }

        T[] existing = value.cacheClients.get(endPoint);
        AtomicInteger sequence = null;
        if (existing == null) {
            existing = (T[]) new Object[connectionPoolSize];
            sequence = new AtomicInteger(0);
            value.cacheClients.put(endPoint, existing);
            value.mapEndPointToSequence.put(endPoint, sequence);
        } else {
            sequence = value.mapEndPointToSequence.get(endPoint);
        }

        int currentIndex = Math.abs(sequence.getAndIncrement()) % connectionPoolSize;
        if (existing[currentIndex] != null) {
            return existing[currentIndex];
        }

        try {
            PYConnection connection = connectionPool.get(endPoint);
            Object[] intArgs = new Object[] { connection, protocolFactory.getProtocol() };
            existing[currentIndex] = (T) service.getConstructor(PYConnection.class, Protocol.class)
                    .newInstance(intArgs);
            return existing[currentIndex];
        } catch (Exception e) {
            logger.error("caught an exception", e);
            throw new RuntimeException();
        }
    }

    public PYConnectionPoolImpl getConnectionPool() {
        return connectionPool;
    }

    /**
     * Get default configuration for I/O transference client.
     *
     * @return
     */
    public static TransferenceConfiguration defaultConfiguration() {
        TransferenceConfiguration clientConfiguration = TransferenceConfiguration.defaultConfiguration();
        clientConfiguration.option(TransferenceClientOption.IO_CONNECTION_TIMEOUT_MS, 3000);
        clientConfiguration.option(TransferenceClientOption.IO_TIMEOUT_MS, 10000);
        clientConfiguration.option(TransferenceClientOption.CONNECTION_COUNT_PER_ENDPOINT, 1);
        clientConfiguration.option(TransferenceClientOption.MAX_LAST_TIME_FOR_CONNECTION_MS, 60 * 1000 * 15);
        return clientConfiguration;
    }

    public void close() {
        connectionPool.close();

        Set<Timeout> timeouts = hashedWheelTimer.stop();
        logger.warn("stop the timer, but there is timeout: {}", timeouts.size());
        for (Timeout timeout : timeouts) {
            timeout.cancel();
        }

        try {
            if (ioEventGroup != null) {
                ioEventGroup.shutdownGracefully(2, 5, TimeUnit.SECONDS);
                ioEventGroup.awaitTermination(6, TimeUnit.SECONDS);
            }
        } catch (InterruptedException e) {
            logger.warn("Caught an exception", e);
        }

        try {
            if (executorForProcessingResponses != null) {
                executorForProcessingResponses.shutdown();
                executorForProcessingResponses.awaitTermination(6, TimeUnit.SECONDS);
            }
        } catch (InterruptedException e) {
            logger.warn("Caught an exception when close client processors", e);
        }
    }

    public TransferenceConfiguration getCfg() {
        return cfg;
    }

    private static <T> Class<? extends T> findImplementationClass(final Class<T> serviceInterface) {
        try {
            return (Class<? extends T>) Iterables
                    .find(ImmutableList.copyOf(serviceInterface.getEnclosingClass().getClasses()),
                            new Predicate<Class<?>>() {
                                @Override
                                public boolean apply(Class<?> inner) {
                                    logger.info("inner: {}", inner);
                                    return !serviceInterface.equals(inner) && serviceInterface.isAssignableFrom(inner);
                                }
                            });
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Could not find a sibling enclosed implementation of " + "service interface: " + serviceInterface);
        }
    }

    public ByteBufAllocator getAllocator() {
        return allocator;
    }

    public void setAllocator(ByteBufAllocator allocator) {
        this.allocator = allocator;
    }

    private class ThreadLocalValue {
        final Map<EndPoint, T[]> cacheClients;
        final Map<EndPoint, AtomicInteger> mapEndPointToSequence;

        public ThreadLocalValue() {
            this.cacheClients = new HashMap<>();
            this.mapEndPointToSequence = new HashMap<>();
        }
    }

    public void setBootstrapBuilder(BootstrapBuilder bootstrapBuilder) {
        this.bootstrapBuilder = bootstrapBuilder;
    }
}
