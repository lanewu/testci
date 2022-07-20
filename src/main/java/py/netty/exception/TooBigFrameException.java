package py.netty.exception;

import io.netty.buffer.ByteBuf;
import py.netty.message.Header;

public class TooBigFrameException extends AbstractNettyException {
    private static final long serialVersionUID = -6516483328493444454L;

    private final int maxFrameSize;
    private final int frameSize;
    private Header header;

    public TooBigFrameException(int maxFrameSize, int frameSize) {
        super(ExceptionType.TOOBIGFRAME);
        this.maxFrameSize = maxFrameSize;
        this.frameSize = frameSize;
    }

    @Override
    public void toBuffer(ByteBuf buffer) {
        buffer.writeInt(getExceptionType().getValue());
        buffer.writeInt(maxFrameSize);
        buffer.writeInt(frameSize);
    }

    @Override
    public int getSize() {
        return Integer.BYTES * 3;
    }

    public static TooBigFrameException fromBuffer(ByteBuf buffer) {
        return new TooBigFrameException(buffer.readInt(), buffer.readInt());
    }

    public int getMaxFrameSize() {
        return maxFrameSize;
    }

    public int getFrameSize() {
        return frameSize;
    }

    @Override
    public String toString() {
        return "TooBigFrameException [maxFrameSize=" + maxFrameSize + ", frameSize=" + frameSize + "]";
    }

    public Header getHeader() {
        return header;
    }

    public TooBigFrameException setHeader(Header header) {
        this.header = header;
        return this;
    }
}
