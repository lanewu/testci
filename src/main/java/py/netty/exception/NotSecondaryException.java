package py.netty.exception;

import org.apache.commons.lang3.Validate;

import io.netty.buffer.ByteBuf;

public class NotSecondaryException extends AbstractStatusException {
    private static final long serialVersionUID = -3628483172089682197L;

    public NotSecondaryException(long volumeId, int segIndex, int status) {
        super(ExceptionType.NOTSECONDARY, volumeId, segIndex, status);
    }

    public static NotSecondaryException fromBuffer(ByteBuf buffer) {
        NotSecondaryException exception = new NotSecondaryException(buffer.readLong(), buffer.readInt(),
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
    public String toString() {
        return "NotSecondaryException [super=" + super.toString() + "]";
    }
}
