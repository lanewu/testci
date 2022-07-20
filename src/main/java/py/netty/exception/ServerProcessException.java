package py.netty.exception;

import io.netty.buffer.ByteBuf;

public class ServerProcessException extends AbstractNettyException {
    private static final long serialVersionUID = -8997454085085369500L;

    public ServerProcessException(String message) {
        super(ExceptionType.SERVEREXCEPTION, message);
    }

    public ServerProcessException(Throwable e) {
        super(ExceptionType.SERVEREXCEPTION, "server process exception: " + e);
    }

    public static ServerProcessException fromBuffer(ByteBuf buffer) {
        return new ServerProcessException(AbstractNettyException.bufferToString(buffer));
    }
}
