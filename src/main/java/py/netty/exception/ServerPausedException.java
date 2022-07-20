package py.netty.exception;

import io.netty.buffer.ByteBuf;

public class ServerPausedException extends AbstractNettyException {

    public ServerPausedException() {
        super(ExceptionType.SERVER_PAUSED);
    }

    public ServerPausedException(String msg) {
        super(ExceptionType.SERVER_PAUSED, msg);
    }

    public static ServerPausedException fromBuffer(ByteBuf buffer) {
        return new ServerPausedException(AbstractNettyException.bufferToString(buffer));
    }
}
