package py.transfer;

import java.util.List;
import java.util.concurrent.BlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.MessageToMessageEncoder;

public class PYIOTcpServer {
    private static final Logger logger = LoggerFactory.getLogger(PYIOTcpServer.class);
    public static final int TCP_DATA_SERVER_PORT = 10012;
    final int SERVER_THREAD_COUNT = 1;
    private BlockingQueue<PYIOMessage> requestMessageQueue;
    private PYChannelIDManager channelManager;
    private ServerBootstrap bootStrap;
    private EventLoopGroup serverGroup;

    public PYIOTcpServer(BlockingQueue<PYIOMessage> requestMessageQueue) {
        this.requestMessageQueue = requestMessageQueue;
        this.channelManager = new PYChannelIDManager(PYIOParameters.MAX_CHANNELS);
        this.serverGroup = new NioEventLoopGroup(SERVER_THREAD_COUNT);
    }

    public void bind(int port) throws InterruptedException {

        bootStrap = new ServerBootstrap();
        bootStrap.group(serverGroup).channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, PYIOParameters.SERVER_SO_BACKLOG)
                .childOption(ChannelOption.TCP_NODELAY, true).childOption(ChannelOption.SO_KEEPALIVE, true)
                .childHandler(new ChannelInitializer<SocketChannel>() {

                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new PYIOConnectionHandler(channelManager));
                        ch.pipeline().addLast(
                                new PYIOMessageDecoder(PYIOParameters.MAX_MESSAGE_LENGTH, 4,
                                        PYIOParameters.LENGTH_FIELD_LENGTH, channelManager));
                        ch.pipeline().addLast(new PYIOMessageEncoder());
                        ch.pipeline().addLast(new PYIORequestMessageHandler(requestMessageQueue));
                    }
                });

        bootStrap.bind(port).sync();
    }

    public void shutdown() {
        serverGroup.shutdownGracefully();
    }

    public PYChannelIDManager getChannelManager() {
        return channelManager;
    }

    private class PYIOConnectionHandler extends ChannelInboundHandlerAdapter {
        private PYChannelIDManager thechannelManager;

        public PYIOConnectionHandler(PYChannelIDManager channelManager) {
            super();
            this.thechannelManager = channelManager;
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            super.channelActive(ctx);
            Channel channel = ctx.channel();

            thechannelManager.addChannel(channel);
            logger.info("channel is add {}", channel);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            super.channelInactive(ctx);

            Channel channel = ctx.channel();
            thechannelManager.delChannel(channel);
            logger.info("channel is del {}", channel);
        }

    }

    private class PYIOMessageEncoder extends MessageToMessageEncoder<PYIOMessage> {

        @Override
        protected void encode(ChannelHandlerContext ctx, PYIOMessage msg, List<Object> out) throws Exception {

            ByteBuf buf = ctx.alloc().ioBuffer(msg.getHeader().getLength()+ 8);

            buf.writeInt(msg.getHeader().getMagic());
            buf.writeInt(msg.getHeader().getLength());
            logger.warn("a msg is write length:{} requestid:{} channel id:{}", msg.getHeader().getLength(),msg.getHeader().getRequestid(),msg.getHeader().getChannelid());
            buf.writeLong(msg.getHeader().getRequestid());
            buf.writeInt(msg.getHeader().getChannelid());

            ByteBuf body = msg.getBody();
            //logger.warn("a body content:{}", body.array());

            buf.writeBytes(body, body.readerIndex(), body.readableBytes());
            logger.warn("a msg is write out {}", buf.readableBytes());
            out.add(buf);
        }

    }

    private class PYIOMessageDecoder extends LengthFieldBasedFrameDecoder {

        private PYChannelIDManager channelManager;

        public PYIOMessageDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength,
                PYChannelIDManager channelManager) {

            super(maxFrameLength, lengthFieldOffset, lengthFieldLength);
            this.channelManager = channelManager;
        }

        @Override
        protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
            logger.info("server recieve msg :{}", in.readableBytes());
            ByteBuf packet = (ByteBuf) super.decode(ctx, in);
            if (packet == null) {
                logger.info("server recieve msg not long engouh:{}", in.readableBytes());
                return null;
            }
            logger.info("server recieve packet long engouh:{}", packet.readableBytes());
            PYIOMessage message = new PYIOMessage();
            PYIOMessageHeader header = new PYIOMessageHeader();
            header.setMagic(packet.readInt());
            header.setLength(packet.readInt());
            header.setRequestid(packet.readLong());
            logger.info("channel id:{}", channelManager.getId(ctx.channel()));
            packet.readInt();
            header.setChannelid(channelManager.getId(ctx.channel()));

            message.setHeader(header);

            message.setBody(Unpooled.copiedBuffer(packet.slice()));

            logger.info("server decode message:{}", message);
            return message;
        }
    }

}
