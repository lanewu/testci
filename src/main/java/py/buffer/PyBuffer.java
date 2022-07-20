package py.buffer;

import java.nio.ByteBuffer;

import io.netty.buffer.ByteBuf;

public class PyBuffer {
    private ByteBuf byteBuf;

    public PyBuffer(ByteBuf byteBuf) {
        this.byteBuf = byteBuf;
    }

    public ByteBuf getByteBuf() {
        return byteBuf;
    }

    public ByteBuffer getByteBuffer() {
        return byteBuf.nioBuffer();
    }

    public void release() {
        if (byteBuf != null) {
            byteBuf.release();
            byteBuf = null;
        }
    }

    public void releaseReference() {
        byteBuf = null;
    }
}
