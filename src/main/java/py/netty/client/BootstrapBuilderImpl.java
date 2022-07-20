package py.netty.client;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import py.connection.pool.AbstractBootstrapBuilder;
import py.connection.pool.udp.detection.NetworkIoHealthChecker;
import py.netty.core.ByteToMessageDecoder;
import py.netty.core.RequestTimeoutHandler;
import py.netty.core.TransferenceOption;
import py.netty.core.twothreads.AbstractChannel;
import py.netty.core.twothreads.TTSocketChannel;

/**
 * Created by kobofare on 2017/3/23.
 */
public class BootstrapBuilderImpl extends AbstractBootstrapBuilder {

    private final ExecutorService executor;

    public BootstrapBuilderImpl(ExecutorService executor){
        this.executor = executor;
    }

    @Override
    public Bootstrap build() {
        int ioTimeout = (int) cfg.valueOf(TransferenceClientOption.IO_TIMEOUT_MS);
        int maxFrameSize = (int) cfg.valueOf(TransferenceOption.MAX_MESSAGE_LENGTH);
        ChannelInitializer<AbstractChannel> initializer = new ChannelInitializer<AbstractChannel>() {
//            ChannelInitializer<Channel> initializer = new ChannelInitializer<Channel>() {
            @Override
//            protected void initChannel(Channel ch) throws Exception {
                protected void initChannel(AbstractChannel ch) throws Exception {
                ChannelPipeline p = ch.pipeline();
                MessageTimeManager messageTimeManager = new MessageTimeManager(hashedWheelTimer, ioTimeout);
                //p.addLast(new IdleStateHandler(100, 0, 0, TimeUnit.MILLISECONDS));
                // Inbound
                p.addLast(new ChannelInboundHandlerAdapter() {
                    @Override
                    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                        //mark the ip, when client receive server msg successfully
                      NetworkIoHealthChecker.INSTANCE.markReachableAndKeepChecking((InetSocketAddress) ctx.channel().remoteAddress());
                        super.channelRead(ctx, msg);
                    }
                });
                p.addLast(new ByteToMessageDecoder(maxFrameSize, allocator));
                // Outbound
                p.addLast(new RequestTimeoutHandler(messageTimeManager));

                AsyncResponseHandler asyncResponseHandler = new AsyncResponseHandler(protocolFactory.getProtocol(),
                        messageTimeManager);
                asyncResponseHandler.setExecutor(executor);
                // Inbound
                p.addLast(asyncResponseHandler);
            }
        };

        Bootstrap bootstrap = new Bootstrap();

        int maxRcvBuf = (int)cfg.valueOf(TransferenceOption.MAX_BYTES_ONCE_ALLOCATE);
        // TODO: make sure connect timeout less than message timeout
        int connectionTimeout = (int) cfg.valueOf(TransferenceClientOption.IO_CONNECTION_TIMEOUT_MS);
        bootstrap.group(ioEventGroup).channel(TTSocketChannel.class).option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionTimeout).handler(initializer);
        bootstrap.option(ChannelOption.ALLOCATOR, allocator);
        bootstrap.option(ChannelOption.RCVBUF_ALLOCATOR, new AdaptiveRecvByteBufAllocator(512, 8192, maxRcvBuf));
        return bootstrap;
    }
}
