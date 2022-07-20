package py.netty.exception;

import io.netty.buffer.ByteBuf;

/**
 * incomplete group exception
 * Created by tyr on 17-4-10.
 */
public class IncompleteGroupException extends AbstractNettyException {

    public IncompleteGroupException() {
        super(ExceptionType.INCOMPLETE_GROUP);
    }

    public static AbstractNettyException fromBuffer(ByteBuf byteBuf) {
        return new IncompleteGroupException();
    }
}
