package py.netty.core;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.metrics.PYMetric;
import py.metrics.PYMetricRegistry;
import py.metrics.PYTimerContext;
import py.netty.exception.TooBigFrameException;
import py.netty.memory.PooledByteBufAllocatorWrapper;
import py.netty.message.Header;
import py.netty.message.Message;
import py.netty.message.MessageImpl;

public class ByteToMessageDecoder extends ChannelInboundHandlerAdapter {
    private final static Logger logger = LoggerFactory.getLogger(ByteToMessageDecoder.class);

    private final static int DEFAULT_MAX_FRAME_SIZE = 1024 * 1024 * 16;
    private final ByteBufAllocator allocator;
    private CompositeByteBuf cumulation;
    private ByteBuf data;
    private final int maxFrameSize;
    private Header header;
    private int requestCount;

    private PYMetric histoParsePackageCount;
    private PYMetric histoChannelReadableBytes;
    private PYMetric timerDecodeRequest;
    private PYMetric meterFragmentRequest;
    private PYMetric meterFragmentHeader;

    private PYMetric timerDecodeSingleRequest;
    private PYTimerContext timerDecodeSingleRequestContext;

    private PYMetric timerDecodeRequestInterval;
    private PYMetric timerChannelReadInterval;
    private PYTimerContext timerDecodeRequestIntervalContext;
    private PYTimerContext timerChannelReadIntervalContext;

    private PYMetric timerDurationBetweenProcessingRequestAndWritingResponse;
    private PYMetric timerDurationBetweenProcessingRequestAndFlushingResponse;
    private PYTimerContext timerDurationBetweenProcessingRequestAndWritingResponseContext;
    private PYTimerContext timerDurationBetweenProcessingRequestAndFlushingResponseContext;

    public ByteToMessageDecoder() {
        this(DEFAULT_MAX_FRAME_SIZE, PooledByteBufAllocatorWrapper.INSTANCE);
    }

    public ByteToMessageDecoder(int maxFrameSize, ByteBufAllocator allocator) {
        this.maxFrameSize = maxFrameSize;
        this.cumulation = null;
        this.data = null;
        this.allocator = allocator;
        initMetrics();
    }

    private void initMetrics() {
        PYMetricRegistry metricRegistry = PYMetricRegistry.getMetricRegistry();
        Validate.isTrue(metricRegistry != null);
        timerDecodeRequest = metricRegistry
                .register(MetricRegistry.name(ByteToMessageDecoder.class.getSimpleName(), "timer_decode_request"),
                        Timer.class);
        meterFragmentRequest = metricRegistry
                .register(MetricRegistry.name(ByteToMessageDecoder.class.getSimpleName(), "meter_fragment_request"),
                        Meter.class);
        meterFragmentHeader = metricRegistry
                .register(MetricRegistry.name(ByteToMessageDecoder.class.getSimpleName(), "meter_fragment_header"),
                        Meter.class);
        histoParsePackageCount = metricRegistry
                .register(MetricRegistry.name(ByteToMessageDecoder.class.getSimpleName(), "histo_parse_package_count"),
                        Histogram.class);
        histoChannelReadableBytes = metricRegistry.register(
                MetricRegistry.name(ByteToMessageDecoder.class.getSimpleName(), "histo_channel_readable_bytes"),
                Histogram.class);
        timerDecodeSingleRequest = metricRegistry.register(
                MetricRegistry.name(ByteToMessageDecoder.class.getSimpleName(), "timer_decode_single_request"),
                Timer.class);
        timerDecodeRequestInterval = metricRegistry.register(
                MetricRegistry.name(ByteToMessageDecoder.class.getSimpleName(), "timer_decode_request_interval"),
                Timer.class);
        timerChannelReadInterval = metricRegistry.register(
                MetricRegistry.name(ByteToMessageDecoder.class.getSimpleName(), "timer_channel_read_interval"),
                Timer.class);

        timerDurationBetweenProcessingRequestAndWritingResponse = metricRegistry.register(MetricRegistry
                .name(ByteToMessageDecoder.class.getSimpleName(),
                        "timer_duration_between_processing_request_and_writing_response"), Timer.class);
        timerDurationBetweenProcessingRequestAndFlushingResponse = metricRegistry.register(MetricRegistry
                .name(ByteToMessageDecoder.class.getSimpleName(),
                        "timer_duration_between_processing_request_and_flushing_response"), Timer.class);

    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        logger.warn("deregister inactive channel:{}, header={}", ctx.channel(), header);
        ctx.fireChannelUnregistered();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.warn("inactive channel:{}, header={}", ctx.channel(), header);
        header = null;
        if (cumulation != null) {
            cumulation.release();
            cumulation = null;
        }
        ctx.fireChannelInactive();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        //decoding
        if (!(msg instanceof ByteBuf)) {
            ctx.fireChannelRead(msg);
            return;
        }

        PYTimerContext decodeRequestContext = timerDecodeRequest.time();

        data = (ByteBuf) msg;

        histoChannelReadableBytes.updateHistogram(data.readableBytes());

        if (timerChannelReadIntervalContext != null) {
            timerChannelReadIntervalContext.stop();
        }

        timerChannelReadIntervalContext = timerChannelReadInterval.time();

        // init request count
        requestCount = 0;

        try {
            int processLength = 0;

            // firstly process cumulation and data
            if (cumulation != null) {
                Validate.isTrue(cumulation.isReadable());
                processLength += processCumulation(ctx);

                logger.debug("after process cumulation and data, parsed request count {}, process length:{}",
                        requestCount, processLength);
            }

            // No way that both cumulation and data have content.
//            Validate.isTrue(!(cumulation != null && data != null));

            // secondly process data
            if (cumulation == null && data != null) {
                // cumulation has been consumed and data
                processLength += processData(ctx);
            } else {
                // do nothing, waiting for the next round to accumulate more data
            }

            histoParsePackageCount.updateHistogram(requestCount);
            logger.debug("At last parsed request count {}, process length:{}", requestCount, processLength);

        } catch (Exception e) {
            logger.error("caught an exception", e);
            if (cumulation != null) {
                cumulation.release();
                cumulation = null;
            }
            if (data != null) {
                data.release();
                data = null;
            }
            // close the socket
            ctx.fireChannelInactive();
        } finally {
            decodeRequestContext.stop();
            //decoding
        }
    }

    private int processData(ChannelHandlerContext ctx) throws Exception {
        int processLength = 0;
        while (data.isReadable()) {
            // parse header
            if (header == null) {
                if (timerDecodeSingleRequestContext == null) {
                    timerDecodeSingleRequestContext = timerDecodeSingleRequest.time();
                }

                if (data.readableBytes() < Header.headerLength()) {
                    meterFragmentHeader.mark();
                    // can not parse a header, just break
                    break;
                } else {
                    // can parse a header
                    try {
                        header = Header.fromBuffer(data);
                        processLength += Header.headerLength();
                        logger.trace("receive header:{}", header);
                    } catch (Exception e) {
                        logger.error("caught an exception, read index:{}, write index:{}, capacity:{}", data,
                                data.readerIndex(), data.writerIndex(), data.capacity(), e);
                        throw e;
                    }

                }
            }

            Validate.notNull(header);

            // parse body
            int bodyLength = header.getLength();
            // if the package only has a header, it should be fired to upper level.
            if (bodyLength == 0) {
                fireChanelRead(ctx, new MessageImpl(header, null));
                requestCount++;
                continue;
            }

            if (!data.isReadable()) {
                // there is no data to parse, just break
                break;
            }

            // parse data
            int readableBytes = data.readableBytes();
            if (readableBytes < bodyLength) {
                meterFragmentRequest.mark();
                // can not produce a package, so waiting for next bytebuf for producing a new package.
                break;
            } else {
                // there are more than one packages.
                Message message = new MessageImpl(header, data.retainedSlice(data.readerIndex(), bodyLength));
                fireChanelRead(ctx, message);
                data.skipBytes(bodyLength);
                processLength += bodyLength;
                requestCount++;
            }
        } // end of while

        if (!data.isReadable()) {
            data.release();
            data = null;
        } else {
            Validate.isTrue(cumulation == null);
            cumulation = new CompositeByteBuf(allocator, true, 200);
            cumulation.addComponent(true, data);
        }

        return processLength;
    }

    private int processCumulation(ChannelHandlerContext ctx) throws Exception {
        int cumulationLength = cumulation.readableBytes();
        int processLength = 0;
        // parse header
        if (header == null) {
            if (timerDecodeSingleRequestContext == null) {
                timerDecodeSingleRequestContext = timerDecodeSingleRequest.time();
            }

            cumulation.addComponent(true, data);
            if (cumulation.readableBytes() < Header.headerLength()) {
                meterFragmentHeader.mark();
                // can not parse a header, just break
                data = null;
            } else {
                // can parse a header
                header = Header.fromBuffer(cumulation);
                processLength += Header.headerLength();
                logger.trace("receive header:{}", header);

                Validate.notNull(header);
                Validate.isTrue(processLength > cumulationLength);

                if (data.isReadable()) {
                    data.retain();
                    data.skipBytes(processLength - cumulationLength);
                } else {
                    data = null;
                }
                cumulation.release();
                cumulation = null;

                // if the package only has a header, it should be fired to upper level.
                if (header.getLength() == 0) {
                    fireChanelRead(ctx, new MessageImpl(header, null));
                    requestCount++;
                }
            }
        } else {
            // header exists, parse body from data
            int bodyLength = header.getLength();
            // TODO: length can not be zero
            Validate.isTrue(bodyLength > 0);
            if (cumulation.readableBytes() + data.readableBytes() < bodyLength) {
                meterFragmentRequest.mark();
                // can not produce a package, so waiting for next bytebuf for producing a new package.
                cumulation.addComponent(true, data);
                data = null;
            } else {
                // Now bodyLength <= cumulation.readableBytes() + data.readableBytes()
                // A package can be generated
                int bodySizeInData = bodyLength - cumulation.readableBytes();

                // We have to use retainedSlice, because CompositeByteBuf.addComponent() may release tmpBuf if it
                // does any consolidate internally
                ByteBuf tmpBuf = data.retainedSlice(data.readerIndex(), bodySizeInData);
                // move the reader index
                data.skipBytes(bodySizeInData);

                cumulation.addComponent(true, tmpBuf);
                Message message = new MessageImpl(header, cumulation);
                cumulation = null;

                processLength += bodyLength;
                if (!data.isReadable()) {
                    data.release();
                    data = null;
                }

                fireChanelRead(ctx, message);
                requestCount++;
            }
        }
        return processLength;
    }

    public void fireChanelRead(ChannelHandlerContext ctx, Message message) {
        // first thing is set header == null, very important
        header = null;
        timerDecodeSingleRequestContext.stop();
        timerDecodeSingleRequestContext = null;

        if (timerDecodeRequestIntervalContext != null) {
            timerDecodeRequestIntervalContext.stop();
        }

        timerDecodeRequestIntervalContext = timerDecodeRequestInterval.time();
        timerDurationBetweenProcessingRequestAndWritingResponseContext = timerDurationBetweenProcessingRequestAndWritingResponse
                .time();
        timerDurationBetweenProcessingRequestAndFlushingResponseContext = timerDurationBetweenProcessingRequestAndFlushingResponse
                .time();

        if (message.getHeader().getLength() > maxFrameSize) {
            // the package is too larger, throw a exception to tell the client.
            logger.error("max frame size:{}, message:{}", maxFrameSize, message);
            message.release();
            ctx.fireExceptionCaught(new TooBigFrameException(maxFrameSize, message.getHeader().getLength())
                    .setHeader(message.getHeader()));
        } else {
            logger.trace("complete header:{}", message.getHeader());
            ctx.fireChannelRead(message);
        }
    }

    // just for unit tests
    public ByteBuf getCumulation() {
        return this.cumulation;
    }

    // just for unit tests
    public Header getHeader() {
        return this.header;
    }
/*
    @Override
    public void bind(ChannelHandlerContext ctx, SocketAddress localAddress, ChannelPromise promise) throws Exception {
        ctx.bind(localAddress, promise);
    }

    @Override
    public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress,
            ChannelPromise promise) throws Exception {

        ctx.connect(remoteAddress, localAddress, promise);
    }

    @Override
    public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        ctx.disconnect(promise);
    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        ctx.close(promise);
    }

    @Override
    public void deregister(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        ctx.deregister(promise);

    }

    @Override
    public void read(ChannelHandlerContext ctx) throws Exception {
        ctx.read();
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        ctx.write(msg, promise);
        // task engine has pulled the WriteTask from the queue
        if (timerDurationBetweenProcessingRequestAndWritingResponseContext != null) {
            timerDurationBetweenProcessingRequestAndWritingResponseContext.stop();
        }
    }

    @Override
    public void flush(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
        if (timerDurationBetweenProcessingRequestAndWritingResponseContext != null) {
            timerDurationBetweenProcessingRequestAndFlushingResponseContext.stop();
        }
    }*/
}
