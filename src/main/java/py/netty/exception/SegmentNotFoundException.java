package py.netty.exception;

import io.netty.buffer.ByteBuf;

public class SegmentNotFoundException extends AbstractSegmentException {
    private static final long serialVersionUID = -4302460900961353619L;

    public SegmentNotFoundException(int segIndex, long volumeId, long instanceId) {
        super(ExceptionType.SEGMENTNOTFOUND, segIndex, volumeId, instanceId);
    }

    public static SegmentNotFoundException fromBuffer(ByteBuf buffer) {
        return new SegmentNotFoundException(buffer.readInt(), buffer.readLong(), buffer.readLong());
    }

    @Override
    public String toString() {
        return "SegmentNotFoundException [super=" + super.toString() + "]";
    }
}
