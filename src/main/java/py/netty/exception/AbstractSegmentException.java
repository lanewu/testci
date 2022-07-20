package py.netty.exception;

import io.netty.buffer.ByteBuf;

/**
 * Created by kobofare on 2017/6/28.
 */
public class AbstractSegmentException extends AbstractNettyException {
    private final int segIndex;
    private final long volumeId;
    private final long instanceId;

    public AbstractSegmentException(ExceptionType type, int segIndex, long volumeId, long instanceId) {
        super(type);
        this.instanceId = instanceId;
        this.volumeId = volumeId;
        this.segIndex = segIndex;
    }

    public long getVolumeId() {
        return volumeId;
    }

    public long getInstanceId() {
        return instanceId;
    }

    public int getSegIndex() {
        return segIndex;
    }

    @Override
    public void toBuffer(ByteBuf buffer) {
        buffer.writeInt(getExceptionType().getValue());
        buffer.writeInt(segIndex);
        buffer.writeLong(volumeId);
        buffer.writeLong(instanceId);
    }

    @Override
    public int getSize() {
        return Integer.BYTES + Integer.BYTES + Long.BYTES + Long.BYTES;
    }

    @Override
    public String toString() {
        return "AbstractSegmentException [super=" + super.toString() + ", instanceId=" + instanceId + ", volumeId="
                + volumeId + ", segIndex=" + segIndex + "]";
    }
}
