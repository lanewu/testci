package py.exception;

import py.storage.Storage;

/**
 * A generic backend Exception, useful for encapsulating all the different Exceptions that could happen.
 */
public class StorageException extends Exception {

    private static final long serialVersionUID = 1L;
    private long offset;
    private long length;
    protected boolean ioException = false;

    public StorageException setOffsetAndLength(long offset, long length) {
        this.offset = offset;
        this.length = length;
        return this;
    }

    public boolean isIoException() {
        return ioException;
    }

    public StorageException setIoException(boolean ioException) {
        this.ioException = ioException;
        return this;
    }

    public long getOffset() {
        return offset;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }

    public long getLength() {
        return length;
    }

    public void setLength(long length) {
        this.length = length;
    }

    public StorageException() {
        super();
    }

    public StorageException(String s) {
        super(s);
    }

    public StorageException(Throwable ex1) {
        super(ex1);
    }

    public StorageException(String s, Throwable ex1) {
        super(s, ex1);
    }

    public StorageException(long offset, long length) {
        super();
        this.offset = offset;
        this.length = length;
    }

    @Override
    public String toString() {
        return "StorageException{super=" + super.toString() + ", offset=" + offset + ", length=" + length
                + ", ioException=" + ioException + '}';
    }
}
