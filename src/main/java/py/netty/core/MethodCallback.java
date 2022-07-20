package py.netty.core;

import io.netty.buffer.ByteBufAllocator;

public interface MethodCallback<T> {
    public void complete(T object);

    public void fail(Exception e);

    public ByteBufAllocator getAllocator();
}
