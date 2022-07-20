package py.netty.exception;

import io.netty.buffer.ByteBuf;

/**
 * network unhealthy exception
 * Created by tyr on 17-5-3.
 */
public class NetworkUnhealthyException extends AbstractNettyException{
    public NetworkUnhealthyException(String msg) {
        super(ExceptionType.NETWORK_UNHEALTHY, msg);
    }

    public NetworkUnhealthyException() {
        super(ExceptionType.NETWORK_UNHEALTHY);
    }

    public static AbstractNettyException fromBuffer(ByteBuf byteBuf) {
        return new NetworkUnhealthyException();
    }
}
