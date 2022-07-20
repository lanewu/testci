package py.netty.core;

import java.net.SocketAddress;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoop;
//import io.netty.channel.SingleThreadEventLoop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import py.netty.core.nio.SingleThreadEventLoop;
import py.netty.message.Message;
import py.netty.client.MessageTimeManager;
import py.netty.exception.DisconnectionException;
import py.netty.message.SendMessage;

/**
 * Created by kobofare on 2017/2/17.
 */
public class RequestTimeoutHandlerGeneric extends ChannelOutboundHandlerAdapter {
    private final static Logger logger = LoggerFactory.getLogger(RequestTimeoutHandlerGeneric.class);
    private final MessageTimeManager manager;
    private ChannelHandlerContext ctx;
    private int nWrite = 0;
    private SingleThreadEventLoop singleThreadEventLoop;
    private TimeToFlushTask flushTask;

    public RequestTimeoutHandlerGeneric(MessageTimeManager manager) {
        this.manager = manager;
        flushTask = new TimeToFlushTask();
    }

    class TimeToFlushTask implements Runnable {
		@Override
		public void run() {
			if (nWrite > 0) {
                // must have a write previously
//				logger.info("the number of writes before the flush is {}, now flushing data", nWrite);
				// call the flush function in this class to reset nWrite. Note calling ctx.flush() won't result in the invocation of this.flush(ctx) 
                try {
					flush(ctx);
				} catch (Exception e) {
					logger.warn("something wrong with calling flush");
				}
            }
		}
    }

    @Override
    public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress,
            ChannelPromise promise) throws Exception {
        if (this.ctx == null) {
            this.ctx = ctx;
            singleThreadEventLoop = getEventLoopFromCtx(ctx);
            logger.warn("connected");
        } else {
            logger.warn("connected again?");
        }
        ctx.connect(remoteAddress, localAddress, promise);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        SendMessage message = (SendMessage) msg;
        if (addTimerManager(ctx, message)) {
            if (message.getBuffer() != null) {
            	nWrite++;
            	if (nWrite == 1) {
//            		logger.info("adding a call-back task to the queue prior to write message to the channel {}", message); 
            		// this is the first time a write is being processed. Let's add a flush task to the tail queue within the event loop so that the task can get executed after all tasks have been executed. 
            		singleThreadEventLoop.executeAfterEventLoopIteration(flushTask);
            	} 
                ctx.write(message.getBuffer(), promise);
            }
        } else {
        	logger.warn("can't add timer for message {}", message);
        }
    }
    
    @Override
    public void flush(ChannelHandlerContext ctx) throws Exception {
//        logger.info("reset the number of writes to 0, and flushing data the channle");
        nWrite = 0;
        ctx.flush();
    }

    public boolean addTimerManager(ChannelHandlerContext ctx, Message msg) {
        boolean isBroken = manager.isBroken();
//        logger.debug("message={}, broken={}, channel={}", msg, isBroken, ctx.channel());
        if (isBroken) {
            try {
                msg.getCallback().fail(new DisconnectionException(
                        "channel=" + ctx.channel() + " already broken, message=" + msg));
            } catch (Exception e) {
                logger.warn("caught an exception", e);
            }
            return false;
        }

        manager.addTimer(msg.getRequestId(), msg.getCallback());
        return true;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.warn("caught an exception, size={}", manager.getPendingMessageCount(), cause);
        ctx.fireExceptionCaught(cause);
    }

    private SingleThreadEventLoop getEventLoopFromCtx(ChannelHandlerContext ctx) {
        EventLoop eventLoop = ctx.channel().eventLoop();
        if (!(eventLoop instanceof SingleThreadEventLoop)) {
            logger.error("event loop is not SingleThreadEventLoop");
            return null;
        } else {
            return (SingleThreadEventLoop) eventLoop;
        }
    }
}
