package py.netty.core;

import java.lang.reflect.Method;

import com.google.protobuf.AbstractMessage;

import io.netty.buffer.ByteBuf;
import py.netty.exception.AbstractNettyException;
import py.netty.exception.InvalidProtocolException;
import py.netty.exception.NotSupportedException;
import py.netty.message.Header;
import py.netty.message.Message;

/**
 * all interface DO NOT handle any ByteBuf, just do encode/decode things.
 * @author lx
 *
 */
public interface Protocol {
    public ByteBuf encodeRequest(Header header, AbstractMessage metadata) throws InvalidProtocolException;

    public ByteBuf encodeResponse(Header header, AbstractMessage metadata) throws InvalidProtocolException;

    public Object decodeRequest(Message msg) throws InvalidProtocolException;

    public Object decodeResponse(Message msg) throws InvalidProtocolException;

    public AbstractNettyException decodeException(ByteBuf buffer) throws InvalidProtocolException;

    public ByteBuf encodeException(long requestId, AbstractNettyException e);

    public Method getMethod(int methodType) throws NotSupportedException;
}
