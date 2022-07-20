package py.netty.exception;

import io.netty.buffer.ByteBuf;

public class ServerShutdownException extends AbstractNettyException {
    private static final long serialVersionUID = 8329427903717206234L;

    public ServerShutdownException() {
        super(ExceptionType.SERVERSHUTDOWN);
    }

    public ServerShutdownException(String msg) {
        super(ExceptionType.SERVERSHUTDOWN, msg);
    }

    public static ServerShutdownException fromBuffer(ByteBuf buffer) {
        return new ServerShutdownException(AbstractNettyException.bufferToString(buffer));
    }
}
