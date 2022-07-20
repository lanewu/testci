package py.netty.core;

import java.util.concurrent.atomic.AtomicLong;

import io.netty.buffer.ByteBufAllocator;

public abstract class ProtocolFactory {
    private final static AtomicLong requestId = new AtomicLong(0);
    protected ByteBufAllocator allocator;

    public static long getRequestId() {
        return requestId.getAndIncrement();
    }

    public abstract Protocol getProtocol();

    public void setByteBufAllocator(ByteBufAllocator allocator) {
        this.allocator = allocator;
    }
}
