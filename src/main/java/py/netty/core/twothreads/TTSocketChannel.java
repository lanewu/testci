/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package py.netty.core.twothreads;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.socket.DefaultSocketChannelConfig;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannelConfig;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.SocketUtils;
import io.netty.util.internal.UnstableApi;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.metrics.PYMetric;
import py.metrics.PYMetricRegistry;
import py.metrics.PYNullMetric;
import py.metrics.PYTimerContext;
import py.netty.core.nio.MyNioSocketChannel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.concurrent.Executor;

import static io.netty.channel.internal.ChannelUtils.MAX_BYTES_PER_GATHERING_WRITE_ATTEMPTED_LOW_THRESHOLD;

/**
 * {@link io.netty.channel.socket.SocketChannel} which uses NIO selector based implementation.
 */

/**
 * This class is duplicated from NioServerSocketChannel. The reason of doing so is that it is very hard to extend
 * AbstractNioMessageChannel which eventually inherits from AbstractNioChannel.
 * <p>
 * AbstractNioChannel uses isCompatible to check if its event loop is NioEventLoop. Because of this, we have to drag all
 * its subclass into py.netty.core, unfortunately.
 * <p>
 * The only reason to to duplicate this class is due to eventloop's compatibility check.
 *
 * @author chenlia
 */
public class TTSocketChannel extends AbstractNioByteChannel implements io.netty.channel.socket.SocketChannel {
    private static final Logger logger = LoggerFactory.getLogger(TTSocketChannel.class);
    private static final SelectorProvider DEFAULT_SELECTOR_PROVIDER = SelectorProvider.provider();
    private PYMetric timerChannelWrite;
    private PYMetric timerDoWrite;
    private PYMetric histoTotalWrittenBytePerWrite;
    private PYMetric histoWrittenBytePerIteration;
    private PYMetric histoMaxBytesPerIteration;
    private PYMetric meterOneBuffer;

    private static SocketChannel newSocket(SelectorProvider provider) {
        try {
            /**
             *  Use the {@link SelectorProvider} to open {@link SocketChannel} and so remove condition in
             *  {@link SelectorProvider#provider()} which is called by each SocketChannel.open() otherwise.
             *
             *  See <a href="https://github.com/netty/netty/issues/2308">#2308</a>.
             */
            return provider.openSocketChannel();
        } catch (IOException e) {
            throw new ChannelException("Failed to open a socket.", e);
        }
    }

    private final SocketChannelConfig config;

    /**
     * Create a new instance
     */
    public TTSocketChannel() {
        this(DEFAULT_SELECTOR_PROVIDER);
    }

    /**
     * Create a new instance using the given {@link SelectorProvider}.
     */
    public TTSocketChannel(SelectorProvider provider) {
        this(newSocket(provider));
    }

    /**
     * Create a new instance using the given {@link SocketChannel}.
     */
    public TTSocketChannel(SocketChannel socket) {
        this(null, socket);
    }

    /**
     * Create a new instance
     *
     * @param parent the {@link Channel} which created this instance or {@code null} if it was created by the user
     * @param socket the {@link SocketChannel} which will be used
     */
    public TTSocketChannel(Channel parent, SocketChannel socket) {
        super(parent, socket);
        config = new NioSocketChannelConfig(this, socket.socket());
        PYMetricRegistry metricRegistry = PYMetricRegistry.getMetricRegistry();
        Validate.isTrue(metricRegistry != null);
        timerChannelWrite = metricRegistry
                .register(MetricRegistry.name(TTSocketChannel.class.getSimpleName(), "timer_channel_write"),
                        Timer.class);

        timerDoWrite = metricRegistry
                .register(MetricRegistry.name(TTSocketChannel.class.getSimpleName(), "timer_do_write"), Timer.class);

        histoWrittenBytePerIteration = metricRegistry.register(
                MetricRegistry.name(TTSocketChannel.class.getSimpleName(), "histo_written_bytes_per_iteration"),
                Histogram.class);

        histoTotalWrittenBytePerWrite = metricRegistry.register(
                MetricRegistry.name(TTSocketChannel.class.getSimpleName(), "histo_total_written_bytes_per_write"),
                Histogram.class);

        histoMaxBytesPerIteration = metricRegistry
                .register(MetricRegistry.name(TTSocketChannel.class.getSimpleName(), "histo_max_bytes_per_iteration"),
                        Histogram.class);

        meterOneBuffer = metricRegistry
                .register(MetricRegistry.name(TTSocketChannel.class.getSimpleName(), "meter_one_buffer"), Meter.class);
    }

    @Override
    public ServerSocketChannel parent() {
        return (ServerSocketChannel) super.parent();
    }

    @Override
    public SocketChannelConfig config() {
        return config;
    }

    @Override
    protected SocketChannel javaChannel() {
        return (SocketChannel) super.javaChannel();
    }

    @Override
    public boolean isActive() {
        SocketChannel ch = javaChannel();
        return ch.isOpen() && ch.isConnected();
    }

    @Override
    public boolean isOutputShutdown() {
        return javaChannel().socket().isOutputShutdown() || !isActive();
    }

    @Override
    public boolean isInputShutdown() {
        return javaChannel().socket().isInputShutdown() || !isActive();
    }

    @Override
    public boolean isShutdown() {
        Socket socket = javaChannel().socket();
        return socket.isInputShutdown() && socket.isOutputShutdown() || !isActive();
    }

    @Override
    public InetSocketAddress localAddress() {
        return (InetSocketAddress) super.localAddress();
    }

    @Override
    public InetSocketAddress remoteAddress() {
        return (InetSocketAddress) super.remoteAddress();
    }

    @UnstableApi
    @Override
    protected final void doShutdownOutput() throws Exception {
        if (PlatformDependent.javaVersion() >= 7) {
            javaChannel().shutdownOutput();
        } else {
            javaChannel().socket().shutdownOutput();
        }
    }

    @Override
    public ChannelFuture shutdownOutput() {
        return shutdownOutput(newPromise());
    }

    @Override
    public ChannelFuture shutdownOutput(final ChannelPromise promise) {
        final TTEventLoop loop = eventLoop();
        if (loop.inEventLoop()) {
            ((AbstractUnsafe) unsafe()).shutdownOutput(promise);
        } else {
            loop.execute(new Runnable() {
                @Override
                public void run() {
                    ((AbstractUnsafe) unsafe()).shutdownOutput(promise);
                }
            });
        }
        return promise;
    }

    @Override
    public ChannelFuture shutdownInput() {
        return shutdownInput(newPromise());
    }

    @Override
    protected boolean isInputShutdown0() {
        return isInputShutdown();
    }

    @Override
    public ChannelFuture shutdownInput(final ChannelPromise promise) {
        EventLoop loop = eventLoop();
        if (loop.inEventLoop()) {
            shutdownInput0(promise);
        } else {
            loop.execute(new Runnable() {
                @Override
                public void run() {
                    shutdownInput0(promise);
                }
            });
        }
        return promise;
    }

    @Override
    public ChannelFuture shutdown() {
        return shutdown(newPromise());
    }

    @Override
    public ChannelFuture shutdown(final ChannelPromise promise) {
        ChannelFuture shutdownOutputFuture = shutdownOutput();
        if (shutdownOutputFuture.isDone()) {
            shutdownOutputDone(shutdownOutputFuture, promise);
        } else {
            shutdownOutputFuture.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(final ChannelFuture shutdownOutputFuture) throws Exception {
                    shutdownOutputDone(shutdownOutputFuture, promise);
                }
            });
        }
        return promise;
    }

    private void shutdownOutputDone(final ChannelFuture shutdownOutputFuture, final ChannelPromise promise) {
        ChannelFuture shutdownInputFuture = shutdownInput();
        if (shutdownInputFuture.isDone()) {
            shutdownDone(shutdownOutputFuture, shutdownInputFuture, promise);
        } else {
            shutdownInputFuture.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture shutdownInputFuture) throws Exception {
                    shutdownDone(shutdownOutputFuture, shutdownInputFuture, promise);
                }
            });
        }
    }

    private static void shutdownDone(ChannelFuture shutdownOutputFuture, ChannelFuture shutdownInputFuture,
            ChannelPromise promise) {
        Throwable shutdownOutputCause = shutdownOutputFuture.cause();
        Throwable shutdownInputCause = shutdownInputFuture.cause();
        if (shutdownOutputCause != null) {
            if (shutdownInputCause != null) {
                logger.debug("Exception suppressed because a previous exception occurred.", shutdownInputCause);
            }
            promise.setFailure(shutdownOutputCause);
        } else if (shutdownInputCause != null) {
            promise.setFailure(shutdownInputCause);
        } else {
            promise.setSuccess();
        }
    }

    private void shutdownInput0(final ChannelPromise promise) {
        try {
            shutdownInput0();
            promise.setSuccess();
        } catch (Throwable t) {
            promise.setFailure(t);
        }
    }

    private void shutdownInput0() throws Exception {
        if (PlatformDependent.javaVersion() >= 7) {
            javaChannel().shutdownInput();
        } else {
            javaChannel().socket().shutdownInput();
        }
    }

    @Override
    protected SocketAddress localAddress0() {
        return javaChannel().socket().getLocalSocketAddress();
    }

    @Override
    protected SocketAddress remoteAddress0() {
        return javaChannel().socket().getRemoteSocketAddress();
    }

    @Override
    protected void doBind(SocketAddress localAddress) throws Exception {
        doBind0(localAddress);
    }

    private void doBind0(SocketAddress localAddress) throws Exception {
        if (PlatformDependent.javaVersion() >= 7) {
            SocketUtils.bind(javaChannel(), localAddress);
        } else {
            SocketUtils.bind(javaChannel().socket(), localAddress);
        }
    }

    @Override
    protected boolean doConnect(SocketAddress remoteAddress, SocketAddress localAddress) throws Exception {
        if (localAddress != null) {
            doBind0(localAddress);
        }

        boolean success = false;
        try {
            logger.warn("connecting start");
            boolean connected = SocketUtils.connect(javaChannel(), remoteAddress);
            logger.warn("connecting end: connected:{} ", connected);
            if (!connected) {
                eventLoop().wakeupSelector();
                try {
                    logger.warn("changing interest set ");
                    selectionKey().interestOps(SelectionKey.OP_CONNECT);
                    logger.warn("changed interest set ");
                } finally {
                    // we have to call waitSelectorDone() once wakeupSelector is called
                    eventLoop().waitSelectorDone();
                }
            }
            success = true;
            return connected;
        } catch (Throwable t) {
            logger.warn("caught a throwable when making connection to {} ", remoteAddress, t);
            return false;
        } finally {
            if (!success) {
                doClose();
            }
        }
    }

    @Override
    protected void doFinishConnect() throws Exception {
        eventLoop().wakeupSelector();
        boolean ok = false;
        try {
            logger.warn("finishing connection");
            ok = javaChannel().finishConnect();
            logger.warn("finished connection ");
        } finally {
            eventLoop().waitSelectorDone();
        }

        if (!ok) {
            throw new Error();
        }
    }

    @Override
    protected void doDisconnect() throws Exception {
        doClose();
    }

    @Override
    protected void doClose() throws Exception {
        super.doClose();
        javaChannel().close();
    }

    @Override
    protected int doReadBytes(ByteBuf byteBuf) throws Exception {
        final RecvByteBufAllocator.Handle allocHandle = unsafe().recvBufAllocHandle();
        allocHandle.attemptedBytesRead(byteBuf.writableBytes());
        return byteBuf.writeBytes(javaChannel(), allocHandle.attemptedBytesRead());
    }

    @Override
    protected int doWriteBytes(ByteBuf buf) throws Exception {
        final int expectedWrittenBytes = buf.readableBytes();
        return buf.readBytes(javaChannel(), expectedWrittenBytes);
    }

    @Override
    protected long doWriteFileRegion(FileRegion region) throws Exception {
        final long position = region.transferred();
        return region.transferTo(javaChannel(), position);
    }

    private void adjustMaxBytesPerGatheringWrite(int attempted, int written, int oldMaxBytesPerGatheringWrite) {
        // By default we track the SO_SNDBUF when ever it is explicitly set. However some OSes may dynamically change
        // SO_SNDBUF (and other characteristics that determine how much data can be written at once) so we should try
        // make a best effort to adjust as OS behavior changes.
        if (attempted == written) {
            if (attempted << 1 > oldMaxBytesPerGatheringWrite) {
                ((NioSocketChannelConfig) config).setMaxBytesPerGatheringWrite(attempted << 1);
            }
        } else if (attempted > MAX_BYTES_PER_GATHERING_WRITE_ATTEMPTED_LOW_THRESHOLD && written < attempted >>> 1) {
            ((NioSocketChannelConfig) config).setMaxBytesPerGatheringWrite(attempted >>> 1);
        }
    }

    @Override
    protected void doWrite(ChannelOutboundBuffer in) throws Exception {
        SocketChannel ch = javaChannel();
        int totalBytes = 0;
        PYTimerContext doWriteContext = timerDoWrite.time();
        try {
            do {
                if (in.isEmpty()) {
                    // All written so clear OP_WRITE
                    // do nothing, because no one set OP_WRITE
                    //                clearOpWrite();
                    // Directly return here so incompleteWrite(...) is not called.
                    return;
                }

                // Ensure the pending writes are made of ByteBufs only.
                int maxBytesPerGatheringWrite = ((NioSocketChannelConfig) config).getMaxBytesPerGatheringWrite();

                histoMaxBytesPerIteration.updateHistogram(maxBytesPerGatheringWrite);
                ByteBuffer[] nioBuffers = in.nioBuffers(1024, maxBytesPerGatheringWrite);
                int nioBufferCnt = in.nioBufferCount();

                // Always us nioBuffers() to workaround data-corruption.
                // See https://github.com/netty/netty/issues/2761
                switch (nioBufferCnt) {
                case 0:
                    // We have something else beside ByteBuffers to write so fallback to normal writes.
                    Validate.isTrue(false, "should not be here");
                    //                    writeSpinCount -= doWrite0(in);
                    break;
                case 1: {
                    // Only one ByteBuf so use non-gathering write
                    // Zero length buffers are not added to nioBuffers by ChannelOutboundBuffer, so there is no need
                    // to check if the total size of all the buffers is non-zero.
                    ByteBuffer buffer = nioBuffers[0];
                    int attemptedBytes = buffer.remaining();
                    PYTimerContext channelWriteContext = timerChannelWrite.time();
                    final int localWrittenBytes = ch.write(buffer);
                    channelWriteContext.stop();

                    totalBytes += localWrittenBytes;
                    if (localWrittenBytes <= 0) {
                        incompleteWrite(true);
                        return;
                    } else {
                        histoWrittenBytePerIteration.updateHistogram(localWrittenBytes);
                    }

                    adjustMaxBytesPerGatheringWrite(attemptedBytes, localWrittenBytes, maxBytesPerGatheringWrite);
                    in.removeBytes(localWrittenBytes);
                    meterOneBuffer.mark();
                    break;
                }
                default: {
                    // Zero length buffers are not added to nioBuffers by ChannelOutboundBuffer, so there is no need
                    // to check if the total size of all the buffers is non-zero.
                    // We limit the max amount to int above so cast is safe
                    long attemptedBytes = in.nioBufferSize();
                    PYTimerContext channelWriteContext = timerChannelWrite.time();
                    final long localWrittenBytes = ch.write(nioBuffers, 0, nioBufferCnt);
                    channelWriteContext.stop();
                    totalBytes += localWrittenBytes;

                    if (localWrittenBytes <= 0) {
                        incompleteWrite(true);
                        return;
                    } else {
                        histoWrittenBytePerIteration.updateHistogram(localWrittenBytes);
                    }
                    // Casting to int is safe because we limit the total amount of data in the nioBuffers to int above.
                    adjustMaxBytesPerGatheringWrite((int) attemptedBytes, (int) localWrittenBytes,
                            maxBytesPerGatheringWrite);
                    in.removeBytes(localWrittenBytes);
                    //                    --writeSpinCount;
                    break;
                }
                }
            } while (true);
        } finally {
            doWriteContext.stop();
            histoTotalWrittenBytePerWrite.updateHistogram(totalBytes);
        }
    }

    @Override
    protected AbstractNioUnsafe newUnsafe() {
        return new NioSocketChannelUnsafe();
    }

    @Override
    protected boolean isCompatible(EventLoop loop) {
        return loop instanceof TTEventLoop;
    }

    private final class NioSocketChannelUnsafe extends NioByteUnsafe {
        @Override
        protected Executor prepareToClose() {
            try {
                if (javaChannel().isOpen() && config().getSoLinger() > 0) {
                    // We need to cancel this key of the channel so we may not end up in a eventloop spin
                    // because we try to read or write until the actual close happens which may be later due
                    // SO_LINGER handling.
                    // See https://github.com/netty/netty/issues/4449
                    doDeregister();
                    return GlobalEventExecutor.INSTANCE;
                }
            } catch (Throwable ignore) {
                // Ignore the error as the underlying channel may be closed in the meantime and so
                // getSoLinger() may produce an exception. In this case we just return null.
                // See https://github.com/netty/netty/issues/4449
            }
            return null;
        }
    }

    private final class NioSocketChannelConfig extends DefaultSocketChannelConfig {
        private volatile int maxBytesPerGatheringWrite = Integer.MAX_VALUE;

        private NioSocketChannelConfig(TTSocketChannel channel, Socket javaSocket) {
            super(channel, javaSocket);
            calculateMaxBytesPerGatheringWrite();
        }

        @Override
        protected void autoReadCleared() {
            //            clearReadPending();
        }

        @Override
        public NioSocketChannelConfig setSendBufferSize(int sendBufferSize) {
            super.setSendBufferSize(sendBufferSize);
            calculateMaxBytesPerGatheringWrite();
            return this;
        }

        void setMaxBytesPerGatheringWrite(int maxBytesPerGatheringWrite) {
            this.maxBytesPerGatheringWrite = maxBytesPerGatheringWrite;
        }

        int getMaxBytesPerGatheringWrite() {
            return maxBytesPerGatheringWrite;
        }

        private void calculateMaxBytesPerGatheringWrite() {
            // Multiply by 2 to give some extra space in case the OS can process write data faster than we can provide.
            int newSendBufferSize = getSendBufferSize() << 1;
            if (newSendBufferSize > 0) {
                setMaxBytesPerGatheringWrite(getSendBufferSize() << 1);
            }
        }
    }
}
