package py.netty.exception;

import io.netty.buffer.ByteBuf;

import static py.netty.exception.ExceptionType.NETWORK_UNHEALTHY_BY_SD;

/**
 * Created by fq on 19-1-2.
 */
public class NetworkUnhealthyBySDException extends AbstractNettyException{
    public NetworkUnhealthyBySDException(String msg) {
        super(NETWORK_UNHEALTHY_BY_SD, msg);
    }

    public NetworkUnhealthyBySDException() {
        super(NETWORK_UNHEALTHY_BY_SD);
    }

    public static AbstractNettyException fromBuffer(ByteBuf byteBuf) {
        return new NetworkUnhealthyBySDException();
    }
}
