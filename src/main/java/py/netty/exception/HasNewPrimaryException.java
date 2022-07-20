package py.netty.exception;

import static py.netty.exception.ExceptionType.HAS_NEW_PRIMARY;

import io.netty.buffer.ByteBuf;

/**
 * Created by fq on 19-2-21.
 */
public class HasNewPrimaryException extends AbstractNettyException{
  public HasNewPrimaryException(String msg) {
    super(HAS_NEW_PRIMARY, msg);
  }

  public HasNewPrimaryException() {
    super(HAS_NEW_PRIMARY);
  }

  public static AbstractNettyException fromBuffer(ByteBuf byteBuf) {
    return new HasNewPrimaryException();
  }
}
