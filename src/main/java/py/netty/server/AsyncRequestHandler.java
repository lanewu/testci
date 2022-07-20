package py.netty.server;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

import com.codahale.metrics.Counter;

import io.netty.channel.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.protobuf.AbstractMessage;

import io.netty.buffer.ByteBufAllocator;
import py.consumer.ConsumerService;
import py.metrics.PYMetric;
import py.metrics.PYMetricRegistry;
import py.metrics.PYTimerContext;
import py.netty.core.AbstractMethodCallback;
import py.netty.core.MethodCallback;
import py.netty.core.Protocol;
import py.netty.exception.*;
import py.netty.message.Header;
import py.netty.message.Message;

/**
 * There is only one the thread for dispatching the requests to services in backend, so the implemented services should
 * be not time-consuming, otherwise it will influence the performance of others services.
 *
 * @author lx
 */
public class AsyncRequestHandler extends SimpleChannelInboundHandler<Message> {
    private final static Logger logger = LoggerFactory.getLogger(AsyncRequestHandler.class);
    private final Object serviceObject;
    private final Protocol protocol;
    private AtomicInteger pendingRequestsCount;
    private int maxPendingRequestsCount;
    private ConsumerService<Runnable> requestExecutor;

    public final static String className = "AsyncRequestHandler";
    private static PYMetric timerService;
    private static PYMetric timerInvokeMethod;
    private static py.common.Counter counterPendingRequests;
    static PYMetric counterPendingRequestsInRPC;

    public void setExecutor(ConsumerService<Runnable> executor) {
        this.requestExecutor = executor;
    }

    public AsyncRequestHandler(Object serviceObject, Protocol protocol) {
        super(false);
        this.serviceObject = serviceObject;
        this.protocol = protocol;
        this.pendingRequestsCount = new AtomicInteger(0);
    }

    static {
        PYMetricRegistry registry = PYMetricRegistry.getMetricRegistry();
        timerService = registry
            .register(MetricRegistry.name(className, "timer_service"), Timer.class);
        timerInvokeMethod = registry
            .register(MetricRegistry.name(className, "timer_invoke_method"), Timer.class);
        counterPendingRequestsInRPC = registry
            .register(MetricRegistry.name(className, "counter_pending_requests_in_rpc"),
                Counter.class);
        counterPendingRequests = registry.register(
            MetricRegistry.name(AsyncRequestHandler.className, "counter_pending_requests"),
            Counter.class)::incCounter;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message msg) throws Exception {
        if (this.requestExecutor != null) {
            this.requestExecutor.submit(() -> process(ctx, msg));
        } else {
            process(ctx, msg);
        }
    }

    private void process(ChannelHandlerContext ctx, Message msg) {
        logger.debug("receive message: {}", msg);
        PYTimerContext serviceContext = timerService.time();
        Header header = msg.getHeader();
        if (counterPendingRequests != null) {
            counterPendingRequests.increment();
        }
        int pending = pendingRequestsCount.incrementAndGet();
        if (pending > maxPendingRequestsCount) {
            logger.info("current length: {}, max length: {}, header: {}", pending, maxPendingRequestsCount, header);
            fireResponse(msg, ctx, protocol.encodeException(header.getRequestId(),
                    new ServerOverLoadedException(maxPendingRequestsCount, pending)));
            serviceContext.stop();
            return;
        }

        Object object;
        try {
            object = protocol.decodeRequest(msg);
        } catch (InvalidProtocolException e) {
            logger.error("can not decode the body: {}", msg, e);
            fireResponse(msg, ctx, protocol.encodeException(header.getRequestId(), e));
            serviceContext.stop();
            return;
        }

        Method method;
        try {
            method = protocol.getMethod(header.getMethodType());
        } catch (NotSupportedException e) {
            logger.error("can not support the method: {}", msg, e);
            e.setServer(true);
            fireResponse(msg, ctx, protocol.encodeException(header.getRequestId(), e));
            serviceContext.stop();
            return;
        }

        try {
            MethodCallback<Object> callback = generateCallback(msg, ctx);
            Object[] args;
            if (object == null) {
                args = new Object[] { callback };
            } else {
                args = new Object[] { object, callback };
            }

            PYTimerContext context = timerInvokeMethod.time();
            method.invoke(serviceObject, args);
            context.stop();

        } catch (Throwable e) {
            logger.warn("can not invoke the method", e);
            fireResponse(msg, ctx, protocol.encodeException(header.getRequestId(), new ServerProcessException(e)));
        }

        serviceContext.stop();
    }

    private MethodCallback<Object> generateCallback(Message msg, ChannelHandlerContext ctx) {
        final Header header = msg.getHeader();
        header.setDataLength(0);
        header.setMetadataLength(0);

        MethodCallback<Object> callback = new AbstractMethodCallback<Object>() {
            @Override
            public void complete(Object object) {
                Object response;
                try {
                    response = protocol.encodeResponse(header, (AbstractMessage) object);
                    logger.trace("@@ complete header: {} for request", header);
                } catch (InvalidProtocolException e) {
                    logger.error("caught an exception", e);
                    response = protocol.encodeException(header.getRequestId(), e);
                } catch (Throwable e) {
                    logger.error("caught an exception", e);
                    response = protocol.encodeException(header.getRequestId(), new ServerProcessException(e));
                }
                fireResponse(msg, ctx, response);
            }

            @Override
            public void fail(Exception e) {
                logger.info("caught an exception for msg: {}", msg.getRequestId(), e);
                Object response;
                if (e instanceof AbstractNettyException) {
                    response = protocol.encodeException(header.getRequestId(), (AbstractNettyException) e);
                } else {
                    response = protocol.encodeException(header.getRequestId(), new ServerProcessException(e));
                }

                fireResponse(msg, ctx, response);
            }

            @Override
            public ByteBufAllocator getAllocator() {
                return ctx.alloc();
            }
        };

        return callback;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.warn("caught an exception", cause);
        if (cause instanceof TooBigFrameException) {
            TooBigFrameException e = (TooBigFrameException) cause;
            ctx.channel().writeAndFlush(protocol.encodeException(e.getHeader().getRequestId(), e)).addListener((ChannelFutureListener) future -> {
                if (!future.isSuccess()) {
                    logger.error("fail response channel:{}", future.channel().remoteAddress());
                }
            });
        }
    }

    private void fireResponse(Message request, ChannelHandlerContext ctx, Object msg) {
        request.release();
        if (counterPendingRequests != null) {
            counterPendingRequests.decrement();
        }
        pendingRequestsCount.decrementAndGet();
        ctx.channel().write(msg).addListener((ChannelFutureListener) future -> {
            if (!future.isSuccess()) {
                logger.error("fail response channel:{}", future.channel().remoteAddress());
            }
        });
    }

    public void setMaxPendingRequestsCount(int maxPendingRequestsCount) {
        this.maxPendingRequestsCount = maxPendingRequestsCount;
    }

    public void setPendingRequestsCount(AtomicInteger pendingRequestsCount) {
        this.pendingRequestsCount = pendingRequestsCount;
    }

    public void setCounterPendingRequests(py.common.Counter counterPendingRequests) {
        this.counterPendingRequests = counterPendingRequests;
    }
}
