package py.netty.exception;

import java.nio.charset.Charset;

import org.apache.commons.lang3.Validate;

import io.netty.buffer.ByteBuf;

public abstract class AbstractNettyException extends Exception {
    private static final long serialVersionUID = -8797900855979194346L;
    private ExceptionType exceptionType;
    private static ExceptionType[] exceptionTypes = ExceptionType.values();

    static {
        for (int i = 0; i < exceptionTypes.length; i++) {
            Validate.isTrue(exceptionTypes[i].getValue() == i);
        }
    }

    public AbstractNettyException(ExceptionType exceptionType, String msg) {
        super(msg);
        this.exceptionType = exceptionType;
    }

    public AbstractNettyException(ExceptionType exceptionType) {
        super();
        this.exceptionType = exceptionType;
    }

    public int getSize() {
        String msg = getMessage();
        if (msg == null || msg.isEmpty()) {
            return Integer.BYTES;
        }

        return Integer.BYTES + msg.length();
    }

    public void toBuffer(ByteBuf buffer) {
        buffer.writeInt(getExceptionType().getValue());
        String msg = getMessage();
        if (msg == null || msg.isEmpty()) {
            return;
        }

        buffer.writeBytes(AbstractNettyException.stringToBuffer(getMessage()));
    }

    public static AbstractNettyException parse(ByteBuf byteBuf) throws GenericNettyException {
        int exceptionType = byteBuf.readInt();
        if (exceptionType >= exceptionTypes.length) {
            return new GenericNettyException("can not parse the exception: " + exceptionType);
        }
        return exceptionTypes[exceptionType].fromBuffer(byteBuf);
    }

    public ExceptionType getExceptionType() {
        return exceptionType;
    }

    public static String bufferToString(byte[] src, int off, int len) {
        return new String(src, off, len, Charset.forName("UTF-8"));
    }

    public static String bufferToString(ByteBuf buffer) {
        if (buffer.hasArray()) {
            return new String(buffer.array(), buffer.arrayOffset() + buffer.readerIndex(), buffer.readableBytes(),
                    Charset.forName("UTF-8"));
        } else {
            byte[] data = new byte[buffer.readableBytes()];
            buffer.readBytes(data);
            return new String(data, 0, data.length, Charset.forName("UTF-8"));
        }
    }

    public static byte[] stringToBuffer(String str) {
        return str.getBytes(Charset.forName("UTF-8"));
    }
}
