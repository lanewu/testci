package py.netty.core;

import java.net.SocketAddress;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.netty.core.nio.SingleThreadEventLoop;

public class ServerResponseHandlerGeneric extends ChannelOutboundHandlerAdapter {
	private final static Logger logger = LoggerFactory
			.getLogger(ServerResponseHandlerGeneric.class);
	private ChannelHandlerContext ctx;
	private int nWrite = 0;
//	private SingleThreadEventLoop singleThreadEventLoop;
//	private TimeToFlushTask flushTask;
	
	class TimeToFlushTask implements Runnable {
		@Override
		public void run() {
			logger.info("timeToFlushTask get called. nWrite is {} ", nWrite);
			if (nWrite > 0) {
                // must have a write previously
				logger.info("the number of writes before the flush is {}, now flushing data", nWrite);
				// call the flush function in this class to reset nWrite. Note calling ctx.flush() won't result in the invocation of this.flush(ctx) 
                try {
					flush(ctx);
				} catch (Exception e) {
					logger.warn("something wrong with calling flush");
				}
            } else {
            	logger.info("nWrite is zero");
            }
		}
    }

	@Override
	public void bind(ChannelHandlerContext ctx, SocketAddress localAddress,
			ChannelPromise promise) throws Exception {
		ctx.bind(localAddress, promise);
	}

	@Override
	public void write(ChannelHandlerContext ctx, Object msg,
			ChannelPromise promise) throws Exception {
		if (this.ctx == null) {
			this.ctx = ctx;
//			singleThreadEventLoop = getEventLoopFromCtx(ctx);
//			flushTask = new TimeToFlushTask();
			logger.debug("done with initialization");
		} 
		// this function is executed within EventLoop's io thread
		ctx.write(msg, promise);
		nWrite++;
		if(nWrite == 1) {
			// this is the first time a write is being processed. Let's add a flush task to the tail queue within the event loop so that the task can get executed after all tasks have been executed. 
			logger.info("get the first write");
//        	singleThreadEventLoop.executeAfterEventLoopIteration(flushTask);
		} else 
			logger.info("get the {} write", nWrite);
	}
	
    
    @Override
    public void flush(ChannelHandlerContext ctx) throws Exception {
        logger.info("reset the number of writes to 0");
        nWrite = 0;
        ctx.flush();
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
