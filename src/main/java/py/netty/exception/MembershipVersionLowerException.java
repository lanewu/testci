package py.netty.exception;

import org.apache.commons.lang3.Validate;

import io.netty.buffer.ByteBuf;

/**
 * * message flat format: <br>
 * |--4Byte--|----8Byte----|--4Byte--|--4Byte--|-------------------------------|<br>
 * |--type---|--volume id--|--index--|-membership length-|----membership-------|<br>
 * 
 * @author lx
 *
 */
public class MembershipVersionLowerException extends AbstractNettyException {
    private static final long serialVersionUID = 3439607303471220618L;
    private final long volumeId;
    private final int segIndex;
    private byte[] membership;

    public MembershipVersionLowerException(long volumeId, int segIndex) {
        super(ExceptionType.MEMBERSHIPLOWER);
        this.volumeId = volumeId;
        this.segIndex = segIndex;
    }

    public long getVolumeId() {
        return volumeId;
    }

    public int getSegIndex() {
        return segIndex;
    }

    public static MembershipVersionLowerException fromBuffer(ByteBuf buffer) {
        MembershipVersionLowerException exception = new MembershipVersionLowerException(buffer.readLong(),
                buffer.readInt());
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
        if (membership == null) {
            buffer.writeInt(0);
        } else {
            buffer.writeInt(membership.length);
            buffer.writeBytes(membership);
        }
    }

    @Override
    public int getSize() {
        return Integer.BYTES * 3 + Long.BYTES + (membership == null ? 0 : membership.length);
    }

    @Override
    public String toString() {
        return "MembershipVersionLowerException [super=" + super.toString() + ", volumeId=" + volumeId + ", segIndex="
                + segIndex + ", membership length=" + (membership == null ? 0 : membership.length) + "]";
    }

    public byte[] getMembership() {
        return membership;
    }

    public void setMembership(byte[] membership) {
        this.membership = membership;
    }

}
