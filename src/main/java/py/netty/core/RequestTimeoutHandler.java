package py.netty.core;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import py.metrics.PYMetric;
import py.metrics.PYMetricRegistry;
import py.metrics.PYTimerContext;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import py.netty.client.MessageTimeManager;
import py.netty.exception.DisconnectionException;
import py.netty.message.Message;
import py.netty.message.SendMessage;

/**
 * Created by kobofare on 2017/2/17.
 */
public class RequestTimeoutHandler extends ChannelOutboundHandlerAdapter {
    private final static Logger logger = LoggerFactory.getLogger(RequestTimeoutHandler.class);
    private final MessageTimeManager manager;
    private PYMetric timerWrite;

    public RequestTimeoutHandler(MessageTimeManager manager) {
        this.manager = manager;
        PYMetricRegistry metricRegistry = PYMetricRegistry.getMetricRegistry();
        timerWrite = metricRegistry
                .register(MetricRegistry.name(RequestTimeoutHandler.class.getSimpleName(), "timer_write"),
                        Timer.class);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        PYTimerContext context = timerWrite.time();

        SendMessage message = (SendMessage) msg;
        ByteBuf dataToSend = message.getBuffer();
        Validate.notNull(dataToSend);
        if (addTimerManager(ctx, message)) {
            try {
                // this function is executed within EventLoop's io thread
                ctx.write(dataToSend, promise);
                message.releaseReference();
            } catch (Exception e) {
                logger.error("caught an exception when channel write", e);
            }
        }

        context.stop();
    }

    protected boolean addTimerManager(ChannelHandlerContext ctx, Message msg) {
        boolean isBroken = manager.isBroken();
        if (isBroken) {
            try {
                msg.getCallback().fail(new DisconnectionException(
                        "channel=" + ctx.channel() + " already broken, message=" + msg));
            } catch (Exception e) {
                logger.warn("caught an exception", e);
            } finally {
                msg.release();
            }
            return false;
        }

        manager.addTimer(msg.getRequestId(), msg.getCallback());
        return true;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.warn("caught an exception, message count:{}", manager.getPendingMessageCount(), cause);
        ctx.fireExceptionCaught(cause);
    }
}
