package py.netty.core;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.netty.client.MessageTimeManager;
import py.netty.exception.TooBigFrameException;

import java.io.IOException;

/**
 * Created by kobofare on 2017/3/24.
 */
public abstract class ResponseTimeoutHandler extends ChannelInboundHandlerAdapter {
    private final Logger logger = LoggerFactory.getLogger(ResponseTimeoutHandler.class);
    private final MessageTimeManager messageTimeManager;

    public ResponseTimeoutHandler(MessageTimeManager messageTimeManager) {
        this.messageTimeManager = messageTimeManager;
    }

    public MethodCallback getCallback(long requestId) {
        return messageTimeManager.removeTimer(requestId);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause instanceof TooBigFrameException) {
            TooBigFrameException e = (TooBigFrameException) cause;
            MethodCallback callback = messageTimeManager.removeTimer(e.getHeader().getRequestId());
            if (callback == null) {
                logger.info("the request has timeout: {} for too large frame", e.getHeader());
                return;
            } else {
                callback.fail(e);
            }
        } else if (cause instanceof IOException) {
            logger.warn("caught an exception: {} msg: {}, just close the channel: {}, pending count={}",
                    cause.getClass().getSimpleName(), cause.getMessage(), ctx.channel(),
                    messageTimeManager.getPendingMessageCount());
            messageTimeManager.fireChannelClose();
        } else {
                logger.warn("caught an exception in netty", cause);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.warn("inactive the channel: {}, pending count={}", ctx.channel(),
                messageTimeManager.getPendingMessageCount());
        messageTimeManager.fireChannelClose();
        ctx.fireChannelInactive();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            if (e.state() == IdleState.READER_IDLE) {
                // check if the target service is alive, try to submit a read request.
                try {
                    ctx.channel().read();
                } catch (Exception e1) {
                    logger.warn("channel has broken: {}", ctx.channel(), e1);
                }
            }
        }
    }
}
