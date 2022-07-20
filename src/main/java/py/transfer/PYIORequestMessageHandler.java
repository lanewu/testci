package py.transfer;

import java.util.concurrent.BlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class PYIORequestMessageHandler extends SimpleChannelInboundHandler<PYIOMessage> {
    private static final Logger logger = LoggerFactory.getLogger(PYIOTcpServer.class);
    BlockingQueue<PYIOMessage> requestMessageQueue;

    public PYIORequestMessageHandler(BlockingQueue<PYIOMessage> queue) {
        this.requestMessageQueue = queue;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, PYIOMessage msg) throws Exception {
        logger.debug("receive a msg {}", msg);
        requestMessageQueue.put(msg);
    }

}
