package py.netty.exception;

import io.netty.buffer.ByteBuf;

public class SegmentDeleteException extends AbstractSegmentException {
    private static final long serialVersionUID = -7873358488620733774L;

    public SegmentDeleteException(int segIndex, long volumeId, long instanceId) {
        super(ExceptionType.SEGMENTDELETE, segIndex, volumeId, instanceId);
    }

    public static SegmentDeleteException fromBuffer(ByteBuf buffer) {
        return new SegmentDeleteException(buffer.readInt(), buffer.readLong(), buffer.readLong());
    }

    @Override
    public String toString() {
        return "SegmentDeleteException [super=" + super.toString() + "]";
    }
}
