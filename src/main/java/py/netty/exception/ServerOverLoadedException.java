package py.netty.exception;

import java.nio.ByteBuffer;

import io.netty.buffer.ByteBuf;

public class ServerOverLoadedException extends AbstractNettyException {

    private static final long serialVersionUID = 7043661129141862531L;
    private int pendingRequests = 0;
    private int queueLength = 0;

    public ServerOverLoadedException(int queueLength, int pendingRequests) {
        super(ExceptionType.OVERLOADED, "server queue: " + queueLength + ", pending requests: " + pendingRequests);
        this.setPendingRequests(pendingRequests);
        this.setQueueLength(queueLength);
    }

    public int getPendingRequests() {
        return pendingRequests;
    }

    public void toBuffer(ByteBuf buffer) {
        buffer.writeInt(getExceptionType().getValue());
        buffer.writeInt(queueLength);
        buffer.writeInt(pendingRequests);
    }

    public static ServerOverLoadedException fromBuffer(ByteBuf buffer) {
        return new ServerOverLoadedException(buffer.readInt(), buffer.readInt());
    }

    public void setPendingRequests(int pendingRequests) {
        this.pendingRequests = pendingRequests;
    }

    public int getQueueLength() {
        return queueLength;
    }

    public void setQueueLength(int queueLength) {
        this.queueLength = queueLength;
    }

    @Override
    public int getSize() {
        return Integer.BYTES + Integer.BYTES + Integer.BYTES;
    }

}
