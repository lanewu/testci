package py.netty.client;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.metrics.PYMetric;
import py.metrics.PYMetricRegistry;
import py.metrics.PYTimerContext;
import py.netty.core.MethodCallback;
import py.netty.core.Protocol;
import py.netty.core.ResponseTimeoutHandler;
import py.netty.exception.GenericNettyException;
import py.netty.message.Message;
import py.netty.message.MethodType;

import java.util.concurrent.ExecutorService;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class AsyncResponseHandler extends ResponseTimeoutHandler {
    private final static Logger logger = LoggerFactory.getLogger(AsyncResponseHandler.class);

    private final MessageTimeManager messageTimeManager;
    private final Protocol protocol;
    private PYMetric meterFragment;
    private PYMetric timerService;
    private ExecutorService executor;

    public void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    public AsyncResponseHandler(Protocol protocol, MessageTimeManager messageTimeManager) {
        super(messageTimeManager);
        this.protocol = protocol;
        this.messageTimeManager = messageTimeManager;

        PYMetricRegistry registry = PYMetricRegistry.getMetricRegistry();
        meterFragment = registry
                .register(MetricRegistry.name(AsyncResponseHandler.class.getSimpleName(), "meter_fragment"),
                        Meter.class);
        timerService = registry
                .register(MetricRegistry.name(AsyncResponseHandler.class.getSimpleName(), "timer_service"), Timer.class);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object object) throws Exception {
        Message msg = (Message)object;
        if (executor == null) {
            process(msg);
            return;
        }

        executor.execute(() -> process(msg));
    }

    private void process(Message msg) {
        MethodCallback callback = getCallback(msg.getRequestId());
        if (callback == null) {
            logger.warn("the request:{} is timeout", msg.getRequestId());
            msg.release();
            return;
        }

        PYTimerContext serviceContext = timerService.time();
        logger.trace("@@ complete header: {} for response", msg.getHeader());
        // TODO: make sure datanode encode exception
        if (msg.getHeader().hasException()) {
            meterFragment.mark();
            Exception exception;
            ByteBuf byteBuf = msg.getBuffer();
            try {
                exception = protocol.decodeException(byteBuf);
                logger.info("caught exception for header:{}, exception:{}", msg.getHeader(),exception);
            } catch (Throwable e) {
                exception = new GenericNettyException(e);
            } finally {
                msg.release();
            }
            callback.fail(exception);
            serviceContext.stop();
            return;
        }

        Object response;
        try {
            response = protocol.decodeResponse(msg);
        } catch (Throwable t) {
            logger.error("can not decode the response:{}", msg.getRequestId(), t);
            msg.release();
            callback.fail(new GenericNettyException(t));
            serviceContext.stop();
            return;
        }

        try {
            Validate.notNull(response);
            callback.complete(response);
        } catch (Throwable e) {
            logger.error("can not complete callback:{}", msg.getRequestId(), e);
            msg.release();
            throw new RuntimeException("can not complete callback" + msg.getRequestId());
        } finally {
            serviceContext.stop();
        }

        /**
         * only read response which contains data will be released by user,
         * other responses can be parsed to metadata(protocol buffer)
          */

        if (msg.getHeader().getMethodType() != MethodType.READ.getValue()) {
            msg.release();
        }
    }

}
