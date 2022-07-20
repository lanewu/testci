package py.common;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import py.common.struct.EchoFutureMessage;
import py.common.struct.EndPoint;
import py.common.struct.ExchangeMessage;
import py.common.struct.ExchangeMessage.ConstantField;
import py.netty.memory.PooledByteBufAllocatorWrapper;

/**
 * 
 * @author wangzhiyu
 * 
 */

public class UdpTransferImpl implements UdpTransfer {
    private final Logger logger = LoggerFactory.getLogger(UdpTransferImpl.class);
    private final int PORT;
    private final int RESENDTIMEOUT = 500;
    private int readTimeout = 5000;
    private int waitCount = readTimeout / RESENDTIMEOUT;
    // private ConnectionlessBootstrap bootstrap;
    private Bootstrap bootstrap;
    private EventLoopGroup group;
    private Channel channel;

    private AtomicInteger sequnceHandle = new AtomicInteger(0);
    private BlockingQueue<ExchangeMessage> queue = new LinkedBlockingQueue<ExchangeMessage>();

    private ConcurrentHashMap<Integer, EchoFutureMessage> map = new ConcurrentHashMap<Integer, EchoFutureMessage>(1024);

    private ExchangeMessageCallback messageCallBack;

    private HashedWheelTimer whTimer;

    public UdpTransferImpl(int port) {
        this.PORT = port;
        this.readTimeout = 5000;
        this.messageCallBack = null;
        whTimer = new HashedWheelTimer(10, TimeUnit.MILLISECONDS, 50);

        initudp();
    }

    public UdpTransferImpl(int port, int timeout) {
        this.PORT = port;
        this.readTimeout = timeout;
        this.messageCallBack = null;
        waitCount = readTimeout / RESENDTIMEOUT;
        whTimer = new HashedWheelTimer(RESENDTIMEOUT, TimeUnit.MILLISECONDS, waitCount);

        initudp();
    }

    public UdpTransferImpl(int port, int readtimeout, ExchangeMessageCallback messageCallBack) {
        this.PORT = port;
        this.readTimeout = readtimeout;
        this.messageCallBack = messageCallBack;
        waitCount = readTimeout / RESENDTIMEOUT;

        initudp();
    }

    private void initudp() {
        group = new NioEventLoopGroup();

        bootstrap = new Bootstrap();
        bootstrap.group(group).channel(NioDatagramChannel.class);
        bootstrap.handler(new ChannelInitializer<NioDatagramChannel>() {
            @Override
            protected void initChannel(NioDatagramChannel ch) throws Exception {
                ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(1024, 0, 2, 0, 2));
                ch.pipeline().addLast(new LengthFieldPrepender(2));
                ch.pipeline().addLast(new UdpServerHandler());
            }

        });

        logger.debug("udp start at:" + PORT);

        // channel = bootstrap.bind(new InetSocketAddress(PORT)).sync().channel();
        ChannelFuture fu = bootstrap.bind(new InetSocketAddress(PORT));
        System.out.println("get channel");
        try {
            channel = fu.sync().channel();
        } catch (InterruptedException e) {

            e.printStackTrace();
        }

    }

    /**
     * a factory method to create a udp client
     * 
     * @return a udp client
     */
    static public UdpTransferImpl generateClient(int READTIMEOUT) {
        UdpTransferImpl client = new UdpTransferImpl(0, READTIMEOUT);
        return client;
    }

    /**
     * a factory method to create a udp client
     * 
     * @return a udp client
     */
    static public UdpTransferImpl generateServer(int port, ExchangeMessageCallback messageCallBack) {

        UdpTransferImpl server = new UdpTransferImpl(port, 10, messageCallBack);
        return server;
    }

    /**
     * handler to receive data, data is actually queue into a queue
     * 
     * @author wangzhiyu
     * 
     */
    public class UdpServerHandler extends SimpleChannelInboundHandler<DatagramPacket> {

        @Override
        public void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) throws Exception {

            if (messageCallBack != null) {
                ExchangeMessage shorMessage = new ExchangeMessage(ctx, packet.content(), packet.sender());
                messageCallBack.onRecieved(shorMessage);
            } else {
                ByteBuf buffer = packet.content();

                if (buffer.getByte(ConstantField.TYPEOFFSET) == ConstantField.RESPONSEEMESSAGE) {
                    // System.out.println("this is response message");
                    int sequnceid = buffer.getInt(ConstantField.SEQUENCEIDOFFSET);
                    EchoFutureMessage obj = map.remove(sequnceid);
                    if (obj == null) {
                        logger.error("sequnceid={} is not exist, map size={} ", sequnceid, map.size());
                        return;
                    }
                    // System.out.println("before count="+buffer.refCnt());
                    buffer.retain();
                    // System.out.println("after count="+buffer.refCnt());
                    obj.clearTimeout();
                    buffer.skipBytes(ConstantField.TYPELENGTH + ConstantField.SEQUENCEIDLENGTH);
                    // System.out.println("fuse is set response message");
                    obj.onResponseReceived(buffer);
                } else {
                    logger.error("request  is discard , CallBack is not registered ");
                }
            }
        }
    }

    @Override
    public void writeBytes(byte[] bytedata, int length, EndPoint endPoint) {

        writeBytes(bytedata, length, endPoint.getInetSocketAddress());
    }

    @Override
    public void writeBytes(byte[] bytedata, EndPoint endPoint) {

        writeBytes(bytedata, bytedata.length, endPoint.getInetSocketAddress());
    }

    private void writeBytes(byte[] bytedata, int length, InetSocketAddress destaddress) {

        ByteBuf channelBuffer = newBuffer(length + ConstantField.TYPELENGTH + ConstantField.SEQUENCEIDLENGTH);

        channelBuffer.writeInt(sequnceHandle.incrementAndGet());
        channelBuffer.writeByte(ConstantField.INQUIREMESSAGE);
        channelBuffer.writeBytes(bytedata, 0, length);

        if (destaddress == null) {
            System.out.printf("destaddress is null");
            return;
        }

        // channel.writeAndFlush(new DatagramPacket(channelBuffer, destaddress));
        channel.write(new DatagramPacket(channelBuffer, destaddress));
    }

    @Override
    public int readBytes(byte[] bytedata) {

        return readBytes(bytedata, bytedata.length);

    }

    /**
     * this method read udp data from queue , if no data available ,will block for readTimeout seconds
     * 
     * @param bytedata
     * @return zero , if no data available and timeout, else return actual data length
     */
    @Override
    public int readBytes(byte[] bytedata, int length) {
        ExchangeMessage message = null;

        try {
            message = queue.poll(readTimeout, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.debug("udp read timeout at:" + PORT);
            return 0;
        }

        if (message == null) {
            return 0;
        }
        ByteBuf buffer = message.getBuffer();

        int len = Math.min(buffer.readableBytes(), length);
        if (len == 0)
            return 0;

        buffer.getBytes(0, bytedata, 0, len);
        return len;

    }

    public ExchangeMessage readMessage() {
        ExchangeMessage message = null;

        try {
            message = queue.poll(readTimeout, TimeUnit.SECONDS);
            return message;
        } catch (InterruptedException e) {
            logger.debug("udp read timeout at:" + PORT);
            return null;
        }
    }

    /**
     * allocate pooled ByteBuf , this buffer is reused by netty4
     * 
     * @param length
     * @return
     */
    private ByteBuf newBuffer(int length) {
        return PooledByteBufAllocatorWrapper.INSTANCE.heapBuffer(length);
    }

    /**
     * this method send packet to server , then wait response packet back
     * 
     * @param bytebuffer
     * @return zero , if no data available and timeout, else return actual data length
     */
    public EchoFutureMessage ExchangeMessage(ByteBuffer bytebuffer, EndPoint endPoint) {

        byte[] bytedata = bytebuffer.array();
        int length = bytedata.length;
        int sequnceid = sequnceHandle.incrementAndGet();

        EchoFutureMessage obj = new EchoFutureMessage();

        ByteBuf channelBuffer = newBuffer(length + ConstantField.TYPELENGTH + ConstantField.SEQUENCEIDLENGTH);

        channelBuffer.writeInt(sequnceid);
        channelBuffer.writeByte(ConstantField.INQUIREMESSAGE);
        channelBuffer.writeBytes(bytedata, 0, length);

        if (endPoint == null) {
            System.out.printf("destaddress is null");
            return null;
        }

        channelBuffer.retain();
        IoUdpTimerTask timerTask = new IoUdpTimerTask(channel, channelBuffer, endPoint);
        Timeout timeout = whTimer.newTimeout(timerTask, 500, TimeUnit.MILLISECONDS);
        obj.setTimeout(timeout);
        map.put(sequnceid, obj);

        channel.writeAndFlush(new DatagramPacket(channelBuffer, endPoint.getInetSocketAddress()));

        return obj;
    }

    private class IoUdpTimerTask implements TimerTask {

        private final Channel channel;
        private ByteBuf bytebuf;
        private EndPoint endPoint;

        // private final TimerTask timerTask;

        public IoUdpTimerTask(Channel channel, ByteBuf bytebuf, EndPoint endPoint) {
            this.channel = channel;
            this.bytebuf = bytebuf;
            this.endPoint = endPoint;
        }

        @Override
        public void run(Timeout timeout) throws Exception {
            if (timeout.isCancelled()) {
                return;
            }

            channel.writeAndFlush(new DatagramPacket(bytebuf, endPoint.getInetSocketAddress()));
            logger.warn("resond data packet .......");

        }
    }

    /**
     * to release socket resouse through netty
     */
    public void releaseResource() {
        /*
         * System.out.println("to await"); try { channel.closeFuture().await(); } catch (InterruptedException e) {
         * e.printStackTrace(); }
         */
        group.shutdownGracefully();

    }

}
