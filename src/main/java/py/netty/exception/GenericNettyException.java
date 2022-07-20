package py.netty.exception;

import io.netty.buffer.ByteBuf;

public class GenericNettyException extends AbstractNettyException {
    private static final long serialVersionUID = -188977112277767110L;

    public GenericNettyException(Throwable e) {
        super(ExceptionType.GENERICEXCEPTION, "exception: " + e);
    }

    public GenericNettyException() {
        super(ExceptionType.GENERICEXCEPTION);
    }

    public GenericNettyException(String msg) {
        super(ExceptionType.GENERICEXCEPTION, msg);
    }

    public static GenericNettyException fromBuffer(ByteBuf buffer) {
        return new GenericNettyException(AbstractNettyException.bufferToString(buffer));
    }
}
