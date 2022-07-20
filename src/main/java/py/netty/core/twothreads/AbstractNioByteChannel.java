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
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.internal.ChannelUtils;
import io.netty.channel.socket.ChannelInputShutdownEvent;
import io.netty.channel.socket.ChannelInputShutdownReadComplete;
import io.netty.channel.socket.SocketChannelConfig;
import io.netty.util.internal.StringUtil;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.metrics.PYMetric;
import py.metrics.PYMetricRegistry;
import py.metrics.PYTimerContext;

import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;

import static io.netty.channel.internal.ChannelUtils.WRITE_STATUS_SNDBUF_FULL;

/**
 * {@link AbstractNioChannel} base class for {@link Channel}s that operate on bytes.
 */

/**
 * This class is duplicated from AbstractNioByteChannel (4.1.28)
 *
 * @author chenlia
 */
public abstract class AbstractNioByteChannel extends AbstractNioChannel {
    private static final Logger logger = LoggerFactory.getLogger(AbstractNioByteChannel.class);
    private static final ChannelMetadata METADATA = new ChannelMetadata(false, 16);
    private static final String EXPECTED_TYPES =
            " (expected: " + StringUtil.simpleClassName(ByteBuf.class) + ", " + StringUtil
                    .simpleClassName(FileRegion.class) + ')';

    private Runnable flushTask = new Runnable() {
        @Override
        public void run() {
            // Calling flush instead of flush0 to ensure we are on purpose to try to flush messages that were added
            // via write(...) in the meantime.
            ((AbstractNioUnsafe) unsafe()).flush();
        }
    };

    private boolean inputClosedSeenErrorOnRead;

    private PYMetric histoByteReadPerIteration;
    private PYMetric histoTotalByteRead;
    private PYMetric histoCount;
    private PYMetric timerRead;
    private PYMetric meterIncompleteWrites;

    /**
     * Create a new instance
     *
     * @param parent the parent {@link Channel} by which this instance was created. May be {@code null}
     * @param ch     the underlying {@link SelectableChannel} on which it operates
     */
    protected AbstractNioByteChannel(Channel parent, SelectableChannel ch) {
        super(parent, ch, SelectionKey.OP_READ);

        PYMetricRegistry metricRegistry = PYMetricRegistry.getMetricRegistry();
        Validate.isTrue(metricRegistry != null);

        timerRead = metricRegistry
                .register(MetricRegistry.name(AbstractNioByteChannel.class.getSimpleName(), "timer_read"), Timer.class);
        histoByteReadPerIteration = metricRegistry.register(
                MetricRegistry.name(AbstractNioByteChannel.class.getSimpleName(), "histo_byte_read_per_iteration"),
                Histogram.class);
        histoTotalByteRead = metricRegistry
                .register(MetricRegistry.name(AbstractNioByteChannel.class.getSimpleName(), "histo_total_byte_read"),
                        Histogram.class);
        histoCount = metricRegistry
                .register(MetricRegistry.name(AbstractNioByteChannel.class.getSimpleName(), "histo_count"),
                        Histogram.class);

        meterIncompleteWrites = metricRegistry
                .register(MetricRegistry.name(AbstractNioByteChannel.class.getSimpleName(), "meter_incomplete_write"),
                        Meter.class);
    }

    /**
     * Shutdown the input side of the channel.
     */
    protected abstract ChannelFuture shutdownInput();

    protected boolean isInputShutdown0() {
        return false;
    }

    @Override
    protected AbstractNioUnsafe newUnsafe() {
        return new NioByteUnsafe();
    }

    @Override
    public ChannelMetadata metadata() {
        return METADATA;
    }

    final boolean shouldBreakReadReady(ChannelConfig config) {
        return isInputShutdown0() && (inputClosedSeenErrorOnRead || !isAllowHalfClosure(config));
    }

    private static boolean isAllowHalfClosure(ChannelConfig config) {
        return config instanceof SocketChannelConfig && ((SocketChannelConfig) config).isAllowHalfClosure();
    }

    protected class NioByteUnsafe extends AbstractNioUnsafe {

        private void closeOnRead(ChannelPipeline pipeline) {
            logger.warn("closing channel {}" , javaChannel());
            if (!isInputShutdown0()) {
                if (isAllowHalfClosure(config())) {
                    shutdownInput();
                    pipeline.fireUserEventTriggered(ChannelInputShutdownEvent.INSTANCE);
                } else {
                    close(voidPromise());
                }
            } else {
                inputClosedSeenErrorOnRead = true;
                pipeline.fireUserEventTriggered(ChannelInputShutdownReadComplete.INSTANCE);
            }
        }

        private void handleReadException(ChannelPipeline pipeline, ByteBuf byteBuf, Throwable cause, boolean close,
                RecvByteBufAllocator.Handle allocHandle) {
            logger.warn("caught an exception when read data from channel {}. The byteBuf {}. Throwable cause {} and close: {} ", javaChannel(), byteBuf, cause, close);
            if (byteBuf != null) {
                if (byteBuf.isReadable()) {
                    //                    readPending = false;
                    pipeline.fireChannelRead(byteBuf);
                } else {
                    byteBuf.release();
                }
            }
            allocHandle.readComplete();
            pipeline.fireChannelReadComplete();
            pipeline.fireExceptionCaught(cause);
            if (close || cause instanceof IOException) {
                closeOnRead(pipeline);
            }
        }

        @Override
        public final void read() {
            assert eventLoop().inReaderThread();
            final ChannelConfig config = config();
            if (shouldBreakReadReady(config)) {
                return;
            }
            final ChannelPipeline pipeline = pipeline();
            final ByteBufAllocator allocator = config.getAllocator();
            final RecvByteBufAllocator.Handle allocHandle = recvBufAllocHandle();
            allocHandle.reset(config);

            ByteBuf byteBuf = null;
            boolean close = false;

            int count = 0;
            int totalNumByteRead = 0;
            try {
                do {
                    byteBuf = allocHandle.allocate(allocator);

                    // Read bytes from the socket
                    PYTimerContext timerContext = timerRead.time();
                    int numByteRead = doReadBytes(byteBuf);
                    timerContext.stop();

                    allocHandle.lastBytesRead(numByteRead);
                    if (allocHandle.lastBytesRead() <= 0) {
                        // nothing was read. release the buffer.
                        byteBuf.release();
                        byteBuf = null;
                        close = allocHandle.lastBytesRead() < 0;
                        if (close) {
                            logger.warn("A negative value has been returned from the read. The connection {} might have been disconnected", javaChannel());
                            // readPending = false;
                        }
                        break;
                    } else {
                        count++;
                        totalNumByteRead += numByteRead;
                        histoByteReadPerIteration.updateHistogram(numByteRead);
                    }

                    allocHandle.incMessagesRead(1);
                    //                    readPending = false;
                    pipeline.fireChannelRead(byteBuf);
                    byteBuf = null;
                    //} while (allocHandle.continueReading());
                } while (true);

                allocHandle.readComplete();

                histoTotalByteRead.updateHistogram(totalNumByteRead);
                histoCount.updateHistogram(count);
                // fireChannelReadComplete does nothing now
                pipeline.fireChannelReadComplete();

                if (close) {
                    closeOnRead(pipeline);
                }
            } catch (Throwable t) {
                handleReadException(pipeline, byteBuf, t, close, allocHandle);
            } finally {
                // Check if there is a readPending which was not processed yet.
                // This could be for two reasons:
                // * The user called Channel.read() or ChannelHandlerContext.read() in channelRead(...) method
                // * The user called Channel.read() or ChannelHandlerContext.read() in channelReadComplete(...) method
                //
                // See https://github.com/netty/netty/issues/2254
                //                if (!readPending && !config.isAutoRead()) {
                //                    removeReadOp();
                //                }
            }
        }
    }

    /**
     * Write objects to the OS.
     *
     * @param in the collection which contains objects to write.
     * @return The value that should be decremented from the write quantum which starts at {@link
     * ChannelConfig#getWriteSpinCount()}. The typical use cases are as follows: <ul> <li>0 - if no write was attempted.
     * This is appropriate if an empty {@link ByteBuf} (or other empty content) is encountered</li> <li>1 - if a single
     * call to write data was made to the OS</li> <li>{@link ChannelUtils#WRITE_STATUS_SNDBUF_FULL} - if an attempt to
     * write data was made to the OS, but no data was accepted</li> </ul>
     * @throws Exception if an I/O exception occurs during write.
     */
    protected final int doWrite0(ChannelOutboundBuffer in) throws Exception {
        Object msg = in.current();
        if (msg == null) {
            // Directly return here so incompleteWrite(...) is not called.
            return 0;
        }
        return doWriteInternal(in, in.current());
    }

    private int doWriteInternal(ChannelOutboundBuffer in, Object msg) throws Exception {
        if (msg instanceof ByteBuf) {
            ByteBuf buf = (ByteBuf) msg;
            if (!buf.isReadable()) {
                in.remove();
                return 0;
            }

            final int localFlushedAmount = doWriteBytes(buf);
            if (localFlushedAmount > 0) {
                in.progress(localFlushedAmount);
                if (!buf.isReadable()) {
                    in.remove();
                }
                return 1;
            }
        } else if (msg instanceof FileRegion) {
            FileRegion region = (FileRegion) msg;
            if (region.transferred() >= region.count()) {
                in.remove();
                return 0;
            }

            long localFlushedAmount = doWriteFileRegion(region);
            if (localFlushedAmount > 0) {
                in.progress(localFlushedAmount);
                if (region.transferred() >= region.count()) {
                    in.remove();
                }
                return 1;
            }
        } else {
            // Should not reach here.
            throw new Error();
        }
        return WRITE_STATUS_SNDBUF_FULL;
    }

    @Override
    protected void doWrite(ChannelOutboundBuffer in) throws Exception {
        int writeSpinCount = config().getWriteSpinCount();
        do {
            Object msg = in.current();
            if (msg == null) {
                // Wrote all messages.
                clearOpWrite();
                // Directly return here so incompleteWrite(...) is not called.
                return;
            }
            writeSpinCount -= doWriteInternal(in, msg);
        } while (writeSpinCount > 0);

        incompleteWrite(writeSpinCount < 0);
    }

    protected final void incompleteWrite(boolean setOpWrite) throws InterruptedException {
        meterIncompleteWrites.mark();
        // Schedule flush again later so other tasks can be picked up in the meantime
        // In the improved version, put taskThread to sleep 1ms, so that the socket can send out the flushed message
        Thread.sleep(1);
        eventLoop().execute(flushTask);
    }

    @Override
    protected final Object filterOutboundMessage(Object msg) {
        if (msg instanceof ByteBuf) {
            ByteBuf buf = (ByteBuf) msg;
            if (buf.isDirect()) {
                return msg;
            }

            return newDirectBuffer(buf);
        }

        if (msg instanceof FileRegion) {
            return msg;
        }

        throw new UnsupportedOperationException(
                "unsupported message type: " + StringUtil.simpleClassName(msg) + EXPECTED_TYPES);
    }

    /**
     * Write a {@link FileRegion}
     *
     * @param region the {@link FileRegion} from which the bytes should be written
     * @return amount       the amount of written bytes
     */
    protected abstract long doWriteFileRegion(FileRegion region) throws Exception;

    /**
     * Read bytes into the given {@link ByteBuf} and return the amount.
     */
    protected abstract int doReadBytes(ByteBuf buf) throws Exception;

    /**
     * Write bytes form the given {@link ByteBuf} to the underlying {@link java.nio.channels.Channel}.
     *
     * @param buf the {@link ByteBuf} from which the bytes should be written
     * @return amount       the amount of written bytes
     */
    protected abstract int doWriteBytes(ByteBuf buf) throws Exception;

    protected final void setOpWrite() {
        final SelectionKey key = selectionKey();
        // Check first if the key is still valid as it may be canceled as part of the deregistration
        // from the EventLoop
        // See https://github.com/netty/netty/issues/2104
        if (!key.isValid()) {
            return;
        }
        final int interestOps = key.interestOps();
        if ((interestOps & SelectionKey.OP_WRITE) == 0) {
            key.interestOps(interestOps | SelectionKey.OP_WRITE);
        }
    }

    protected final void clearOpWrite() {
        final SelectionKey key = selectionKey();
        // Check first if the key is still valid as it may be canceled as part of the deregistration
        // from the EventLoop
        // See https://github.com/netty/netty/issues/2104
        if (!key.isValid()) {
            return;
        }
        final int interestOps = key.interestOps();
        if ((interestOps & SelectionKey.OP_WRITE) != 0) {
            key.interestOps(interestOps & ~SelectionKey.OP_WRITE);
        }
    }
}