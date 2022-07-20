package py.netty.message;

import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCounted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.netty.core.MethodCallback;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class MessageImpl<T extends Object> implements Message<T> {
    private final static Logger logger = LoggerFactory.getLogger(MessageImpl.class);

    private final Header header;
    private ByteBuf buffer;

    public MessageImpl(Header header, ByteBuf buffer) {
        this.header = header;
        this.buffer = buffer;
    }

    public Header getHeader() {
        return header;
    }

    @Override
    public long getRequestId() {
        return header.getRequestId();
    }

    @Override
    public ByteBuf getBuffer() {
        return buffer;
    }

    @Override
    public boolean release() {
        return release(1);
    }

    public MethodCallback<T> getCallback() {
        throw new NotImplementedException();
    }

    @Override
    public void releaseReference() {
        this.buffer = null;
    }

    @Override
    public String toString() {
        return "MessageImpl [header=" + header + "]";
    }

    @Override
    public int refCnt() {
        return buffer.refCnt();
    }

    @Override
    public ReferenceCounted retain() {
        buffer.retain();
        return this;
    }

    @Override
    public ReferenceCounted retain(int increment) {
        buffer.retain(increment);
        return this;
    }

    @Override
    public ReferenceCounted touch() {
        buffer.touch();
        return this;
    }

    @Override
    public ReferenceCounted touch(Object hint) {
        buffer.touch(hint);
        return this;
    }

    @Override
    public boolean release(int decrement) {
        boolean release = true;
        if (buffer != null) {
/*            try {
                throw new RuntimeException();
            } catch (RuntimeException e) {
                logger.warn("decrement {} from buf's refcnt {}. buf {}, buf.memoryAddr {}", decrement, buffer.refCnt(),
                        buffer, buffer.memoryAddress(), e);
            } */
            release = buffer.release(decrement);
            if (release) {
                buffer = null;
            }
        }
        return release;
    }
}
