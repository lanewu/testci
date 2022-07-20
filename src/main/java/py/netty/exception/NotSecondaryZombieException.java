package py.netty.exception;

import io.netty.buffer.ByteBuf;
import org.apache.commons.lang3.Validate;

public class NotSecondaryZombieException extends AbstractStatusException {
    private static final long serialVersionUID = -3628483172089682197L;

    public NotSecondaryZombieException(long volumeId, int segIndex, int status) {
        super(ExceptionType.NOTSECONDARYZOMBIE, volumeId, segIndex, status);
    }

    public static NotSecondaryZombieException fromBuffer(ByteBuf buffer) {
        NotSecondaryZombieException exception = new NotSecondaryZombieException(buffer.readLong(), buffer.readInt(),
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
        return "NotSecondaryZombieException [super=" + super.toString() + "]";
    }
}
