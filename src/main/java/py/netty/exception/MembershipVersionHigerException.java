package py.netty.exception;

import io.netty.buffer.ByteBuf;
import org.apache.commons.lang3.Validate;

/**
 * * message flat format: <br>
 * |--4Byte--|----8Byte----|--4Byte--|--4Byte--|-------------------------------|<br>
 * |--type---|--volume id--|--index--|-membership length-|----membership-------|<br>
 *
 * @author lx
 */
public class MembershipVersionHigerException extends AbstractNettyException {
    private final long volumeId;
    private final int segIndex;
    private byte[] membership;

    public MembershipVersionHigerException(long volumeId, int segIndex) {
        super(ExceptionType.MEMBERSHIPHIGER);
        this.volumeId = volumeId;
        this.segIndex = segIndex;
    }

    public long getVolumeId() {
        return volumeId;
    }

    public int getSegIndex() {
        return segIndex;
    }

    public static MembershipVersionHigerException fromBuffer(ByteBuf buffer) {
        MembershipVersionHigerException exception = new MembershipVersionHigerException(buffer.readLong(),
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
        return "MembershipVersionHigerException [super=" + super.toString() + ", volumeId=" + volumeId + ", segIndex="
                + segIndex + ", membership length=" + (membership == null ? 0 : membership.length) + "]";
    }

    public byte[] getMembership() {
        return membership;
    }

    public void setMembership(byte[] membership) {
        this.membership = membership;
    }

}
