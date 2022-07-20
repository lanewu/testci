package py.connection.pool;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;

import io.netty.channel.ChannelFutureListener;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import py.common.struct.EndPoint;
import py.connection.pool.udp.detection.NetworkIoHealthChecker;

/**
 * there is a situation: when someone wants to send message to peer, a channel with the peer should be built and be
 * cached for next use. if the channel is not used for a long time, it will be recycled and closed. if the channel has
 * been closed but someone wants to send message by the channel, a connection request will be sent immediately, the
 * message will be sent to peer after the connection is built successfully, otherwise a timeout exception will be
 * thrown.
 * <p>
 * // the hosts that can response ping request succesfully will be reconnecting.
 *
 * @author lx
 */
public class PYConnectionPoolImpl implements PYConnectionPool, ReconnectionHandler {
    private final static Logger logger = LoggerFactory.getLogger(PYConnectionPoolImpl.class);
    public final static int DEFAULT_CHECK_CONNECTION_INTERVAL_MS = 20;

    private Map<EndPoint, PYChannelPool> mapEndPointToChannelPool;
    private final Thread recycleThread;
    private final Thread detectThread;
    private final BlockingQueue<ConnectionRequest> blockingQueue;
    private int poolSizePerOneEndPoint;
    private int maxLastTimeForConnectionMs;
    private BootstrapBuilder builder;
    private Map<PYChannel, ConnectionListenerManager> mapChannelToListener;
    private volatile boolean stop;

    public PYConnectionPoolImpl(int poolSizePerOneEndPoint, int maxLastTimeForConnectionMs, BootstrapBuilder builder) {
        this.mapEndPointToChannelPool = new ConcurrentHashMap<>();
        this.blockingQueue = new LinkedBlockingQueue<>();
        this.poolSizePerOneEndPoint = poolSizePerOneEndPoint;
        this.maxLastTimeForConnectionMs = maxLastTimeForConnectionMs;
        this.builder = builder;
        this.mapChannelToListener = new ConcurrentHashMap<>();
        this.stop = false;

        recycleThread = new Thread("recycle-thread") {
            public void run() {
                try {
                    while (!stop) {
                        ConnectionRequest request = blockingQueue.take();
                        if (request instanceof FakeConnectionRequest) {
                            logger.warn("exit thread for recycling connection pool");
                            break;
                        }

                        try {
                            processReconnect(request);
                        } catch (Exception e) {
                            logger.warn("caught an exception when processing reconnecting", e);
                        }
                    }
                } catch (Throwable t) {
                    logger.error("recycle thread exit", t);
                }
            }
        };
        recycleThread.start();

        detectThread = new Thread("detect-thread") {
            public void run() {
                try {
                    detect();
                } catch (Throwable t) {
                    logger.error("detect thread exit", t);
                }
            }
        };
        detectThread.start();
    }

    public void retrieveForUnitTest() {
        stop = false;
    }

    private void processDetectResult(PYChannelPool pool, boolean detectResult) {
        if (!detectResult) {
            logger.debug("close the pool information as {}", pool);
            for (PYChannel channel : pool.getAll()) {
                if (mapChannelToListener.get(channel) != null || channel.get() != null) {
                    logger.warn("close the socket when host broken, channel={}", channel);
                    blockingQueue.add(new ExitChannelRequest(channel));
                }
            }
            return;
        }

        /**
         * The detector used to detect whether a channel has been idle for long time and close it.
         * We disable this feature because it results in the system performance, especially system latency, downgrades
         * when a new io request comes. (System has to make a new connection to the server when a new io request
         * comes, and making a new socket connection takes at 100 ms which is slow enough for an IO
         */
        // long time = System.currentTimeMillis();
        for (PYChannel channel : pool.getAll()) {
            /* don't check
            if (time > channel.getLastIoTime() + maxLastTimeForConnectionMs) {
                // close the channel that don't receive io request for a long time.
                if (channel.get() != null) {
                    logger.warn("close the socket when no use, channel={}", channel);
                    blockingQueue.add(new CloseChannelRequest(channel));
                }
            } else { */
            ConnectionListenerManager manager = mapChannelToListener.get(channel);
            if (manager != null) {
                // the channel is being built.
                if (manager.isConnecting()) {
                    logger.info(
                            "We are making connection, dedup the current connection attempt, and wait the last one complete. {}",
                            manager);
                } else {
                    blockingQueue.add(new InnerChannelRequest(channel));
                }
            } else {
                Channel tmp = channel.get();
                if (tmp == null || !tmp.isActive()) {
                    // the channel is broken, just retry.
                    blockingQueue.add(new InnerChannelRequest(channel));
                }
            }
            //   }
        }
    }

    public void detect() throws Exception {
        //UDP detect manager init.
        while (!stop) {
            // get all channels to be reconnected.
            for (Entry<EndPoint, PYChannelPool> entry : mapEndPointToChannelPool.entrySet()) {
                // when the host is not reachable, there is no need to reconnect, and close all channel of the host.
                //Added connect detect request commit ot UDPDetectManager.
                PYChannelPool pool = entry.getValue();

                NetworkIoHealthChecker.INSTANCE.keepChecking(pool.getEndpoint().getHostName());
                boolean result = !NetworkIoHealthChecker.INSTANCE
                    .isUnreachable(pool.getEndpoint().getHostName());

                processDetectResult(pool, result);
                logger.trace("UDP-detection client just checks the status of the connection to {}. connection is {}",
                        pool.getEndpoint(), result);
            }

            Thread.sleep(DEFAULT_CHECK_CONNECTION_INTERVAL_MS);
        }
    }

    protected void processReconnect(ConnectionRequest request) throws Exception {
        if (request instanceof ConnectionRequestResult) {
            logger.warn("ConnectionRequestResult: {}", request);
            PYChannel pyChannel = request.getPyChannel();
            ConnectionListenerManager manager = mapChannelToListener.remove(pyChannel);
            if (manager != null) {
                manager.notifyAllListeners();
                manager.setConnecting(false);
            }

            return;
        } else if (request instanceof ExitChannelRequest) {
            logger.warn("ExitChannelRequest: {}", request);

            // exit the socket
            PYChannel pyChannel = request.getPyChannel();
            Channel oldChannel = pyChannel.set(null);
            if (oldChannel != null && !pyChannel.isClosing()) {
                logger.warn("close the channel={}, connection={}", oldChannel, request.getPyChannel());
                pyChannel.setClosing(true);
                oldChannel.close().addListener((ChannelFutureListener) future -> {
                    blockingQueue.add(new ReconnectAfterCloseDoneRequestResult(request));
                });
            }

            ConnectionListenerManager manager = mapChannelToListener.remove(request.getPyChannel());
            if (manager != null) {
                logger.warn("notify all listener: {} when host is reachable", request.getPyChannel());
                manager.notifyAllListeners();
            } else {
                logger.warn("ExitChannelRequest: manager is null");
            }

            return;
        } else if (request instanceof CloseChannelRequest) {
            logger.warn("CloseChannelRequest result: {}", request);

            // close the socket
            PYChannel channel = request.getPyChannel();
            ConnectionListenerManager manager = mapChannelToListener.get(channel);
            if (manager != null) {
                logger.warn("when closing the socket, someone want to reconnect");
                return;
            } else {
                logger.warn("CloseChannelRequest: manager is null");
            }

            // someone is using the socket.
            if (channel.getLastIoTime() + maxLastTimeForConnectionMs >= System.currentTimeMillis()) {
                logger.warn("CloseChannelRequest: not close: current time:{} last io time:{} max last time:{}",
                        System.currentTimeMillis(), channel.getLastIoTime(), maxLastTimeForConnectionMs);
                return;
            }

            // there is a little window in which someone has sent a request, but the channel is closed internally.
            PYChannel pyChannel = request.getPyChannel();
            Channel oldChannel = pyChannel.set(null);
            if (oldChannel != null && !pyChannel.isClosing()) {
                logger.warn("close old channel:{}, and try connection:{}", oldChannel, channel);
                pyChannel.setClosing(true);
                oldChannel.close().addListener((ChannelFutureListener) future -> {
                    blockingQueue.add(new ReconnectAfterCloseDoneRequestResult(request));
                });
            }
            return;
        } else if (request instanceof ReconnectAfterCloseDoneRequestResult) {
            logger.warn("when channel close done, start to reconnect, result:{}", request);

            PYChannel pyChannel = request.getPyChannel();
            pyChannel.setClosing(false);
            ConnectionListenerManager manager = mapChannelToListener.get(pyChannel);
            if (manager != null && !manager.isConnecting()) {
                logger.warn("for now listener is null, should generate new listener");
                // going to reconnect
                manager.setConnecting(true);
                EndPoint endPoint = pyChannel.getEndPoint();
                builder.build().connect(endPoint.getHostName(), endPoint.getPort()).addListener(manager);
            }
            return;
        }

        // check reconnecting
        PYChannel pyChannel = request.getPyChannel();
        ConnectionListenerManager manager = mapChannelToListener.get(pyChannel);
        if (manager != null) {
            logger.info("the channel:{} is trying to connect, manager:{}", pyChannel, manager);
            if (!(request instanceof InnerChannelRequest)) {
                manager.add(request);
            }
            return;
        }

        // Channel has been connected
        // Also, the request could be either InnerChannelRequest or a message request
        Channel currentChannel = pyChannel.get();
        if (currentChannel != null && currentChannel.isActive()) {
            logger.warn("the channel:{} has been built", pyChannel);
            request.complete(currentChannel);
            return;
        }

        // host is not reachable
        boolean unreachable = NetworkIoHealthChecker.INSTANCE
            .isUnreachable(pyChannel.getEndPoint().getHostName());
        if (unreachable) {
            logger.warn("the host:{} is not reachable", pyChannel);
            // no necessary to reconnect
            request.complete(null);
            return;
        }

        // reconnect process
        manager = new ConnectionListenerManager(pyChannel) {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                try {
                    super.operationComplete(future);
                } finally {
                    // submit to myself to notify all listeners.
                    blockingQueue.add(new ConnectionRequestResult(request));
                }
            }
        };

        if (!(request instanceof InnerChannelRequest)) {
            manager.add(request);
        }

        mapChannelToListener.put(pyChannel, manager);

        // close previous channel, and start a new connection
        currentChannel = pyChannel.set(null);
        if (currentChannel != null) {
            logger.warn("close old channel: {}, and try connection={}", currentChannel, pyChannel);
            pyChannel.setClosing(true);
            currentChannel.close().addListener((ChannelFutureListener) future -> {
                blockingQueue.add(new ReconnectAfterCloseDoneRequestResult(request));
            });
            return;
        } else {
            Validate.isTrue(!manager.isConnecting());
            logger.debug("try connection:{}", pyChannel);
            // set the PYChannel is reconnecting.
            manager.setConnecting(true);
            EndPoint endPoint = pyChannel.getEndPoint();
            builder.build().connect(endPoint.getHostName(), endPoint.getPort()).addListener(manager);
        }

    }

    @Override
    public void close() {
        try {
            stop = true;
            detectThread.join();
        } catch (Exception e) {
            logger.warn("can not wait for detect thread exit", e);
        }

        try {
            blockingQueue.offer(new FakeConnectionRequest());
            recycleThread.join();

        } catch (Exception e) {
            logger.warn("can not wait for reconnect thread exit", e);
        }

        for (Map.Entry<EndPoint, PYChannelPool> entry : mapEndPointToChannelPool.entrySet()) {
            entry.getValue().close();
        }

        mapEndPointToChannelPool.clear();
    }

    public PYConnection get(EndPoint endPoint) {
        PYChannelPool channelPool = mapEndPointToChannelPool.get(endPoint);
        if (channelPool == null) {
            // not associated pool exists
            channelPool = new PYChannelPool(poolSizePerOneEndPoint, endPoint, this);
            PYChannelPool prevAssociatedChannelPool = mapEndPointToChannelPool.putIfAbsent(endPoint, channelPool);

            if (prevAssociatedChannelPool != null) {
                // there is a pool already associated with the endpoint. Let's return the current one.
                channelPool = prevAssociatedChannelPool;
            }
        }

        return new PYConnectionImpl(channelPool.get(endPoint), this);
    }

    /**
     * consider that there are some channels to be removed after a long time no use, then someone uses the {@link
     * PYConnection} to send message, the channel should be reconnected.
     *
     * @param connectionRequest
     */
    @Override
    public void reconnect(ConnectionRequest connectionRequest) {
        // check if the PYChannel exist
        EndPoint endPoint = connectionRequest.getPyChannel().getEndPoint();
        PYChannelPool pool = mapEndPointToChannelPool.get(endPoint);
        Validate.isTrue(pool != null);
        if (NetworkIoHealthChecker.INSTANCE.isUnreachable(endPoint.getHostName())) {
            logger.warn("there is no need to reconnect={}", endPoint);
            connectionRequest.complete(null);
            return;
        }

        Validate.isTrue(blockingQueue.add(connectionRequest));
    }

    class ConnectionRequestResult extends ConnectionRequest {
        public ConnectionRequestResult(ConnectionRequest request) {
            super(request.getPyChannel());
        }
    }

    class ReconnectAfterCloseDoneRequestResult extends ConnectionRequest {
        public ReconnectAfterCloseDoneRequestResult(ConnectionRequest request) {
            super(request.getPyChannel());
        }
    }

    class ExitChannelRequest extends ConnectionRequest {
        public ExitChannelRequest(PYChannel pyChannel) {
            super(pyChannel);
        }
    }

    class CloseChannelRequest extends ConnectionRequest {
        public CloseChannelRequest(PYChannel pyChannel) {
            super(pyChannel);
        }
    }

    class InnerChannelRequest extends ConnectionRequest {
        public InnerChannelRequest(PYChannel pyChannel) {
            super(pyChannel);
        }
    }

    // the class is for the puller thread to exit when the system is asked to shutdown
    class FakeConnectionRequest extends ConnectionRequest {

        public FakeConnectionRequest() {
            super(null);
        }
    }

    // JUST for unit test
    public Map<EndPoint, PYChannelPool> getMapEndPointToChannelPool() {
        return mapEndPointToChannelPool;
    }

    // JUST for unit test
    public void setMapEndPointToChannelPool(Map<EndPoint, PYChannelPool> mapEndPointToChannelPool) {
        this.mapEndPointToChannelPool = mapEndPointToChannelPool;
    }

    // JUST for unit test
    public Map<PYChannel, ConnectionListenerManager> getMapChannelToListener() {
        return mapChannelToListener;
    }

    // JUST for unit test
    public void pauseForUnitTest() {
        stop = true;
    }
}
