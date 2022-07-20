package py.netty.exception;

import org.apache.commons.lang3.Validate;

import io.netty.buffer.ByteBuf;

/**
 * message flat format: <br>
 * |--4Byte--|----8Byte----|--4Byte--|--4Byte--|--4Byte--|-------------------------------|<br>
 * |--type---|--volume id--|--index--|--status-|-membership length-|----membership-------|<br>
 * 
 * @author lx
 *
 */
public abstract class AbstractStatusException extends AbstractNettyException {

    private static final long serialVersionUID = -2121925564385427775L;
    private final long volumeId;
    private final int segIndex;
    private final int status;
    private byte[] membership;

    AbstractStatusException(ExceptionType exceptionType, long volumeId, int segIndex, int status) {
        super(exceptionType);
        this.status = status;
        this.volumeId = volumeId;
        this.segIndex = segIndex;
        this.membership = null;
    }

    public int getStatus() {
        return status;
    }

    public long getVolumeId() {
        return volumeId;
    }

    public int getSegIndex() {
        return segIndex;
    }

    public static AbstractStatusException fromBuffer(ByteBuf buffer) {
        NotPrimaryException exception = new NotPrimaryException(buffer.readLong(), buffer.readInt(), buffer.readInt());
        int membershipLength = buffer.readInt();
        if (buffer.readableBytes() != membershipLength) {
            Validate.isTrue(false,
                    "exception: " + exception + "readableBytes: " + buffer + ", expected length: " + membershipLength);
        }

        byte[] membership = new byte[buffer.readableBytes()];
        buffer.readBytes(membership);
        exception.setMembership(membership);
        return exception;
    }

    @Override
    public void toBuffer(ByteBuf buffer) {
        buffer.writeInt(getExceptionType().getValue());
        buffer.writeLong(volumeId);
        buffer.writeInt(segIndex);
        buffer.writeInt(status);
        if (membership == null) {
            buffer.writeInt(0);
        } else {
            buffer.writeInt(membership.length);
            buffer.writeBytes(membership);
        }
    }

    public byte[] getMembership() {
        return membership;
    }

    public void setMembership(byte[] membership) {
        this.membership = membership;
    }

    @Override
    public int getSize() {
        return Integer.BYTES * 4 + Long.BYTES + (membership == null ? 0 : membership.length);
    }

    @Override
    public String toString() {
        return "AbstractStatusException [super=" + super.toString() + ", volumeId=" + volumeId + ", segIndex=" + segIndex
                + ", status=" + status + ", membership length=" + (membership == null ? 0 : membership.length) + "]";
    }

}
