package py.netty.exception;

import io.netty.buffer.ByteBuf;

public class SnapshotMismatchException extends AbstractNettyException {
    private static final long serialVersionUID = 8898558822099233894L;

    private byte[] snapshotManager;

    public SnapshotMismatchException(byte[] snapshotManager) {
        super(ExceptionType.SNAPSHOTMISMATCH);
        this.snapshotManager = snapshotManager;
    }

    public static SnapshotMismatchException fromBuffer(ByteBuf buffer) {
        byte[] snapshotManager = new byte[buffer.readableBytes()];
        buffer.readBytes(snapshotManager);
        return new SnapshotMismatchException(snapshotManager);
    }

    @Override
    public int getSize() {
        return Integer.BYTES + snapshotManager.length;
    }

    @Override
    public void toBuffer(ByteBuf buffer) {
        buffer.writeInt(getExceptionType().getValue());
        buffer.writeBytes(snapshotManager);
    }

    public void setSnapshotManager(byte[] bytes) {
        snapshotManager = bytes;
    }

    public byte[] getSnapshotManager() {
        return snapshotManager;
    }
}
