package py.netty.message;

public class ResponseHeader extends Header {
    private static final int NO_EXCEPTION_TYPE = -1;
    private int exceptionType;

    public ResponseHeader(byte methodType, int metadataLength, long requestId) {
        this(methodType, metadataLength, 0, requestId);
    }

    public ResponseHeader(byte methodType, int metaLength, int dataLength, long requestId) {
        super(methodType, metaLength, dataLength, requestId);
        this.exceptionType = NO_EXCEPTION_TYPE;
    }

    public int getExceptionType() {
        return exceptionType;
    }

    public void setExceptionType(int exceptionType) {
        this.exceptionType = exceptionType;
    }

}
