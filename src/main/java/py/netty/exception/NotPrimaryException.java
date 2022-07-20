package py.netty.exception;

import io.netty.buffer.ByteBuf;
import org.apache.commons.lang3.Validate;

/**
 * Created by kobofare on 2017/6/28.
 */
public class NotPrimaryException extends AbstractStatusException {

    public NotPrimaryException(long volumeId, int segIndex, int status) {
        super(ExceptionType.NOTPRIMARY, volumeId, segIndex, status);
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
    public String toString() {
        return "NotPrimaryException [super=" + super.toString() + "]";
    }
}
