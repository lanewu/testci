/*
 * Copyright (C) 2012-2013 Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package py.common.rpc.server;

import org.apache.thrift.TApplicationException;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TMessage;
import org.apache.thrift.protocol.TMessageType;
import org.apache.thrift.protocol.TProtocol;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.socket.nio.NioSocketChannel;
import org.jboss.netty.handler.codec.frame.TooLongFrameException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

import py.common.rpc.share.TDuplexProtocolFactory;
import py.common.rpc.share.TProtocolPair;
import py.common.rpc.share.TTransportPair;
import py.metrics.PYMetric;
import py.metrics.PYMetricRegistry;
import py.metrics.PYTimerContext;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Dispatch TNiftyTransport to the TProcessor and write output back.
 * <p>
 * Note that all current async thrift clients are capable of sending multiple requests at once and capable of handling
 * out-of-order responses to those requests, so this dispatcher sends the requests ignore the order. (Eventually this
 * will be conditional on a flag in the thrift message header for future async clients that can handle out-of-order
 * responses).
 */
public class NiftyDispatcher extends SimpleChannelUpstreamHandler {
    private final Logger logger = LoggerFactory.getLogger(NiftyDispatcher.class);
    private final NiftyProcessorFactory processorFactory;
    private final ThreadPoolExecutor exe;
    private final long taskTimeoutMillis;
    private final TDuplexProtocolFactory duplexProtocolFactory;
    private final Semaphore semaphore;
    private PYMetric meterSubmitRequests;
    private PYMetric meterRejectRequests;
    private PYMetric timerWaitAvailableThread;
    private PYMetric timerSubmitResult;
    private PYMetric timerWriteResponse;

    public NiftyDispatcher(ThriftServerDef def) {
        this.processorFactory = def.getProcessorFactory();
        this.duplexProtocolFactory = def.getDuplexProtocolFactory();
        this.exe = (ThreadPoolExecutor) def.getExecutor();
        logger.info("the max pool size is {} ", exe.getMaximumPoolSize());
        this.semaphore = new Semaphore(exe.getMaximumPoolSize());
        this.taskTimeoutMillis = (long) (def.getTaskTimeout() == null ? 0 : def.getTaskTimeout().toMillis());
    }

    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        PYMetricRegistry registry = PYMetricRegistry.getMetricRegistry();
        String remoteAddress = ctx.getChannel().getRemoteAddress().toString();
        if (!remoteAddress.isEmpty()) {
            // remove the char '\'
            remoteAddress = remoteAddress.substring(1);
            int index = 0;
            if (!remoteAddress.isEmpty() && ((index = remoteAddress.indexOf(':')) != -1)) {
                remoteAddress = remoteAddress.substring(0, index);
            }
        }
        logger.info("remote address({}) connected", remoteAddress);
        meterSubmitRequests = registry.register(MetricRegistry.name(NiftyDispatcher.class.getSimpleName(),
                remoteAddress, "meter_submit_requests"), Meter.class);
        meterRejectRequests = registry.register(MetricRegistry.name(NiftyDispatcher.class.getSimpleName(),
                remoteAddress, "meter_reject_requests"), Meter.class);
        timerWaitAvailableThread = registry.register(MetricRegistry.name(NiftyDispatcher.class.getSimpleName(),
                remoteAddress, "timer_wait_available_thread"), Timer.class);
        timerSubmitResult = registry.register(MetricRegistry.name(NiftyDispatcher.class.getSimpleName(),
                remoteAddress, "timer_submit_result"), Timer.class);
        timerWriteResponse = registry.register(MetricRegistry.name(NiftyDispatcher.class.getSimpleName(),
                remoteAddress, "timer_write_response"), Timer.class);
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        if (e.getMessage() instanceof ThriftMessage) {
            ThriftMessage message = (ThriftMessage) e.getMessage();
            if (taskTimeoutMillis > 0) {
                message.setProcessStartTimeMillis(System.currentTimeMillis());
            }

            /**
             * create a out transport and separate the in transport and out transport for reusing the input buffer, and
             * left the first four bytes for saving the size of data to send.
             */
            TNiftyTransport messageTransport = new TNiftyTransport(ctx.getChannel(), message.getBuffer(),
                    message.getTransportType());

            /**
             * upstream and downstream share the same buffer, so after reading all data, then it can be used to store
             * the data to send,
             */
            TTransportPair transportPair = TTransportPair.fromSingleTransport(messageTransport);
            duplexProtocolFactory.getInputProtocolFactory();
            TProtocolPair protocolPair = duplexProtocolFactory.getProtocolPair(transportPair);
            processRequest(ctx, message, messageTransport, protocolPair.getInputProtocol(),
                    protocolPair.getOutputProtocol());
        } else {
            ctx.sendUpstream(e);
        }
    }

    private void processRequest(final ChannelHandlerContext ctx, final ThriftMessage message,
                                final TNiftyTransport messageTransport, final TProtocol inProtocol, final TProtocol outProtocol) {
        try {
            meterSubmitRequests.mark();
            PYTimerContext waitTime = timerWaitAvailableThread.time();
            try {
                if (!semaphore.tryAcquire(100, TimeUnit.MILLISECONDS)) {
                    throw new Exception("timeout: 100");
                }
            } finally {
                waitTime.stop();
            }

            exe.execute(new Runnable() {
                @Override
                public void run() {
                    PYTimerContext submitResultContext = null;
                    try {
                        /**
                         * it contains two operators: one for processing the request, another for dealing with send the
                         * response.
                         */
                        processorFactory.getProcessor(messageTransport).process(inProtocol, outProtocol, null);
                        submitResultContext = timerSubmitResult.time();

                        /**
                         * now write the message on the channel's I/O thread
                         */
                        executeInIoThread(ctx.getChannel(), new Runnable() {
                            @Override
                            public void run() {
                                PYTimerContext writeResponseTimer = timerWriteResponse.time();
                                try {
                                    ThriftMessage response = message.getMessageFactory().create(
                                            messageTransport.getOutputBuffer());
                                    writeResponse(ctx, response);
                                } finally {
                                    writeResponseTimer.stop();
                                }
                            }
                        });

                    } catch (Throwable t) {
                        try {
                            logger.warn("can't deal with the request from {}, local: {} {} ",
                                    ctx.getChannel().getRemoteAddress(), ctx.getChannel().getLocalAddress(), t);
                        } finally {
                            onDispatchException(ctx, t);
                        }
                    } finally {
                        submitResultContext.stop();
                        semaphore.release();
                    }
                }
            });
        } catch (RejectedExecutionException ex) {
            try {
                semaphore.release();
                meterRejectRequests.mark();
                logger.info("Server: {} overloaded to deal with the request of client: {} ",
                        ctx.getChannel().getLocalAddress(), ctx.getChannel().getRemoteAddress());
            } finally {
                TApplicationException x = new TApplicationException(TApplicationException.INTERNAL_ERROR,
                        "server overloaded");
                sendTApplicationException(x, ctx, message, messageTransport, inProtocol, outProtocol);
            }
        } catch (Exception e) {
            logger.warn("caught an exception", e);
            TApplicationException x = new TApplicationException(TApplicationException.INTERNAL_ERROR,
                    "there is no available thread");
            sendTApplicationException(x, ctx, message, messageTransport, inProtocol, outProtocol);
        }
    }

    public void executeInIoThread(Channel channel, Runnable runnable) {
        NioSocketChannel nioSocketChannel = (NioSocketChannel) channel;
        nioSocketChannel.getWorker().executeInIoThread(runnable, true);
    }

    /**
     * send an exception to remote, and receive response
     */
    private void sendTApplicationException(TApplicationException x, ChannelHandlerContext ctx, ThriftMessage request,
                                           TNiftyTransport requestTransport, TProtocol inProtocol, TProtocol outProtocol) {
        if (ctx.getChannel().isConnected()) {
            try {
                // read from IN protocol
                TMessage message = inProtocol.readMessageBegin();
                // write the EXCEPTION type message to OUT protocol
                outProtocol.writeMessageBegin(new TMessage(message.name, TMessageType.EXCEPTION, message.seqid));
                x.write(outProtocol);
                outProtocol.writeMessageEnd();
                // flush means send
                outProtocol.getTransport().flush();
                // after send, we wait response
                ThriftMessage response = request.getMessageFactory().create(requestTransport.getOutputBuffer());
                writeResponse(ctx, response);
            } catch (TException ex) {
                try {
                    logger.warn("server: {} can't response to client: {} ", ctx.getChannel().getLocalAddress()
                            , ctx.getChannel().getLocalAddress(), ex);
                } finally {
                    onDispatchException(ctx, ex);
                }
            }
        }
    }

    private void onDispatchException(ChannelHandlerContext ctx, Throwable t) {
        Channels.fireExceptionCaught(ctx, t);
        closeChannel(ctx);
    }

    private void writeResponse(ChannelHandlerContext ctx, ThriftMessage response) {
        // No ordering required, just write the response immediately
        Channels.write(ctx.getChannel(), response);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {

        // Send for logging
        ctx.sendUpstream(e);

        // Any out of band exception are caught here and we tear down the socket
        if (e instanceof TooLongFrameException) {

        } else {
            closeChannel(ctx);
        }


    }

    private void closeChannel(ChannelHandlerContext ctx) {
        if (ctx.getChannel().isOpen()) {
            ctx.getChannel().close();
        }
    }

    @Override
    public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        // Reads always start out unblocked
        super.channelOpen(ctx, e);
    }
}
