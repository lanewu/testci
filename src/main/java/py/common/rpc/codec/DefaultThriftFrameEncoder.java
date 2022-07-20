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
package py.common.rpc.codec;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.codec.frame.TooLongFrameException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import py.common.rpc.server.ThriftMessage;

public class DefaultThriftFrameEncoder extends ThriftFrameEncoder {
    /**
     * thrift header may occupy some space, so we will left some space for it.
     */
    public static final int DEFAULT_THRIFT_HEADER_SIZE = 1024;
    private final long maxFrameSize;
    private final static Logger logger = LoggerFactory.getLogger(DefaultThriftFrameEncoder.class);

    /**
     * TFramedTransport framing is four bytes long
     */
    public static final int LENGTH_FIELD_LENGTH = 4;

    public DefaultThriftFrameEncoder(long maxFrameSize) {
        this.maxFrameSize = maxFrameSize + DEFAULT_THRIFT_HEADER_SIZE;
    }

    @Override
    protected ChannelBuffer encode(ChannelHandlerContext ctx, Channel channel, ThriftMessage message) throws Exception {
        int frameSize = message.getBuffer().readableBytes();

        if (frameSize > maxFrameSize) {
            logger.error("encode size is bigger than max frame size: encode size {}, max frame size", frameSize,
                    maxFrameSize + DEFAULT_THRIFT_HEADER_SIZE);
            Channels.fireExceptionCaught(
                            ctx,
                            new TooLongFrameException(
                                            String.format("Frame size exceeded on encode: frame was %d bytes, maximum allowed is %d bytes",
                                                            frameSize, maxFrameSize + DEFAULT_THRIFT_HEADER_SIZE)));
            return null;
        }

        switch (message.getTransportType()) {
        case UNFRAMED:
            return message.getBuffer();
        case FRAMED:
            /**
             * the first four bytes has been left to put the size of data to send
             */
            ChannelBuffer temp = message.getBuffer().duplicate();
            temp.clear();
            temp.writeInt(frameSize - LENGTH_FIELD_LENGTH);
            return message.getBuffer();
        case HEADER:
            throw new UnsupportedOperationException("Header transport is not supported");
        case HTTP:
            throw new UnsupportedOperationException("HTTP transport is not supported");
        default:
            throw new UnsupportedOperationException("Unrecognized transport type");
        }
    }
}
