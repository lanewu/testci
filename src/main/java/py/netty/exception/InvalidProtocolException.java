package py.netty.exception;

import io.netty.buffer.ByteBuf;

public class InvalidProtocolException extends AbstractNettyException {

    private static final long serialVersionUID = -679344312789138108L;

    public InvalidProtocolException(String msg) {
        super(ExceptionType.INVALIDPROTOCOL, msg);
    }

    public static InvalidProtocolException fromBuffer(ByteBuf buffer) {
        return new InvalidProtocolException(AbstractNettyException.bufferToString(buffer));
    }
}
