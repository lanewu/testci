package py.netty.core;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.netty.core.nio.SingleThreadEventLoop;

import java.net.SocketAddress;

public class ServerResponseHandler extends ChannelOutboundHandlerAdapter {
    private final static Logger logger = LoggerFactory.getLogger(ServerResponseHandler.class);
    private ChannelHandlerContext ctx;
    private PYTimeToFlush ttf;

    @Override
    public void bind(ChannelHandlerContext ctx, SocketAddress localAddress, ChannelPromise promise) throws Exception {

        ctx.bind(localAddress, promise);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (this.ctx == null) {
            this.ctx = ctx;
            SingleThreadEventLoop singleThreadEventLoop = getEventLoopFromCtx(ctx);
            if (ttf == null) {
                ttf = new PYTimeToFlushImpl(singleThreadEventLoop);
            }

            singleThreadEventLoop.setTimeToFlushCallback(ttf);
            logger.debug("done with initialization");
        }
        // this function is executed within EventLoop's io thread
        // it doesn't matter we reset justFlushed flag before ctx.write
        // or after.
        // logger.warn("before write a message {}", message);
        SingleThreadEventLoop singleThreadEventLoop = getEventLoopFromCtx(ctx);
        singleThreadEventLoop.addChannelToFlush(ctx);
        ttf.incRequestCount();
        ctx.write(msg, promise);
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
