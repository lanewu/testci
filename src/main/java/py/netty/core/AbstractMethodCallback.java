package py.netty.core;

import io.netty.buffer.ByteBufAllocator;
import py.netty.memory.PooledByteBufAllocatorWrapper;

public abstract class AbstractMethodCallback<T> implements MethodCallback<T> {

    public ByteBufAllocator getAllocator() {
        return PooledByteBufAllocatorWrapper.INSTANCE;
    }
}
