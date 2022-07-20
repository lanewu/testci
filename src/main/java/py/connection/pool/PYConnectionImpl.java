package py.connection.pool;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.util.concurrent.GenericFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.metrics.PYMetric;
import py.metrics.PYMetricRegistry;
import py.metrics.PYTimerContext;
import py.netty.exception.DisconnectionException;
import py.netty.message.Message;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author lx
 */
public class PYConnectionImpl implements PYConnection {
    private final static Logger logger = LoggerFactory.getLogger(PYConnectionImpl.class);

    private final PYChannel pyChannel;
    private ReconnectionHandler reconnectionHandler;
    private PYMetric timerWrite;
    private PYMetric timerWriteAndFlush;

    public PYConnectionImpl(PYChannel pyChannel, ReconnectionHandler reconnectionHandler) {
        this.pyChannel = pyChannel;
        this.reconnectionHandler = reconnectionHandler;

        PYMetricRegistry metricRegistry = PYMetricRegistry.getMetricRegistry();
        timerWrite = metricRegistry.register(
                MetricRegistry.name(PYConnectionImpl.class.getSimpleName(), "timer_write"),
                        Timer.class);

        timerWriteAndFlush = metricRegistry.register(
                MetricRegistry.name(PYConnectionImpl.class.getSimpleName(), "timer_write_and_flush"),
                Timer.class);

    }

    public boolean isConnected() {
        Channel channel = pyChannel.get();
        if (channel == null) {
            return false;
        } else {
            return channel.isActive();
        }
    }

    @Override
    public void write(Message msg) {
        pyChannel.setLastIoTime(System.currentTimeMillis());
        Channel channel = pyChannel.get();
        /**
         * when there is no available channel, a connection request will be submitted to connection pool, after the
         * connection is built, the message will be sent automatic.
         **/
        if (channel == null) {
            logger.info("there is no channel={}, when writing, msg={}", pyChannel, msg);
            reconnectionHandler.reconnect(buildConnectionRequest(msg, pyChannel));
            return;
        }

        logger.debug("write msg to channel={}, msg={}", pyChannel, msg);

        PYTimerContext timerContext = timerWrite.time();
        channel.write(msg).addListener(new GenericFutureListener<ChannelFuture>() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (!future.isSuccess()) {
                    logger.warn("send failure: {}, {}, {}, {} Channel is:{}, for msg:{}", future.channel(),
                            future.isCancelled(), future.isDone(), future.isSuccess(), pyChannel, msg, future.cause());
                    msg.getCallback().fail(new DisconnectionException("channel=" + pyChannel + " for msg=" + msg));
                }
                logger.debug("message: {} wrote successfully", msg);
                timerContext.stop();
            }
        });
    }

    // avoid resending the request.
    public ConnectionRequest buildConnectionRequest(Message msg, PYChannel pyChannel) {
        final AtomicInteger counter = new AtomicInteger(1);
        return new ConnectionRequest(pyChannel) {
            // when the channel is built, the message should be sent to target again.
            @Override
            public void complete(Channel newChannel) {
                int count = counter.decrementAndGet();
                if (count != 0) {
                    logger.warn("call many times: {}", count);
                    return;
                }

                if (newChannel == null) {
                    logger.debug("buildConnectionRequest newChannel is null: msg={} Channel is {} ", msg, pyChannel);
                    msg.getCallback().fail(new DisconnectionException("channel=" + pyChannel + " for msg=" + msg));
                    // If channel is disconnected, the message has not been given to the netty yet. So we release it here immediately.
                    msg.release();
                    return;
                }

                logger.warn("after connecting successfully, resend={}, channel={}", msg.getRequestId(), newChannel);
                newChannel.write(msg).addListener(new GenericFutureListener<ChannelFuture>() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if (!future.isSuccess()) {
                            logger.warn("send failure: {}, {}, {}, {} Channel is:{}, for msg:{}", future.channel(),
                                    future.isCancelled(), future.isDone(), future.isSuccess(), pyChannel, msg,
                                    future.cause());
                            msg.getCallback()
                                    .fail(new DisconnectionException("channel=" + pyChannel + " for msg=" + msg));
                        }
                    }
                });
            }
        };
    }

    @Override
    public void flush() {
        Channel channel = pyChannel.get();
        if (channel != null) {
            logger.debug("write and flush msg to channel: {}", pyChannel);
            channel.flush();
        }
    }

    @Override
    public void writeAndFlush(Message msg) {
        pyChannel.setLastIoTime(System.currentTimeMillis());
        Channel channel = pyChannel.get();
        if (channel == null) {
            logger.debug("there is no channel={}, when writing and flushing, msg={}", pyChannel, msg);
            reconnectionHandler.reconnect(buildConnectionRequest(msg, pyChannel));
            return;
        }


        logger.info("write and flush msg to channel={}, msg={}", pyChannel, msg);
        PYTimerContext context = timerWriteAndFlush.time();

        channel.writeAndFlush(msg).addListener(new GenericFutureListener<ChannelFuture>() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (!future.isSuccess()) {
                    logger.warn("send failure: {}, {}, {}, {} Channel is:{}, for msg:{}", future.channel(),
                                future.isCancelled(), future.isDone(), future.isSuccess(), pyChannel, msg, future.cause());
                    msg.getCallback().fail(new DisconnectionException("channel=" + pyChannel + " for msg=" + msg));
                }
                context.stop();
            }
        });
    }

    @Override
    public String toString() {
        return "PYConnectionImpl{" + "pyChannel=" + pyChannel + '}';
    }
}
