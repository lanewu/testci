package py.netty.message;

import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCounted;
import py.netty.core.MethodCallback;

public interface Message<T> extends ReferenceCounted {

    Header getHeader();

    long getRequestId();

    ByteBuf getBuffer();

    MethodCallback<T> getCallback();

    void releaseReference();
}
