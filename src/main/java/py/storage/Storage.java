package py.storage;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import py.common.DirectAlignedBufferAllocator;
import py.exception.StorageException;

/**
 * This interface provides methods to interact with underlining storage systems.
 * 
 * TODO: we need to make read/write thread safe. Particularly, we allow multiple readers and one writer access pattern.
 * 
 * 
 * @author chenlia
 */
public abstract class Storage implements Comparable<Storage> {
    private final static AtomicLong index = new AtomicLong(0);
    private final long id;
    protected final String identifier;
    private final int hashCode;

    protected int ioDepth = 1;

    public Storage(String identifier) {
        this.identifier = identifier;
        this.id = index.getAndIncrement();
        if (identifier == null) {
            hashCode = 0;
        } else {
            hashCode = identifier.hashCode();
        }
    }

    // false -- storage can write or read
    // true -- when reading or writing this storage, the counter of IOException exceed the threshold
    private final AtomicBoolean isBroken = new AtomicBoolean(false);
    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    /**
     * Close the storage
     * 
     * @throws StorageException
     */
    public void close() throws StorageException {
        isClosed.set(true);
    }

    public boolean isClosed() {
        return isClosed.get();
    }

    /**
     * Open the storage
     * 
     * @throws StorageException
     */
    public void open() throws StorageException {
        isClosed.set(false);
    }

    /**
     * @param pos
     *            position at the storage
     * @param dstBuf
     *            where read data is stored to
     * @param off
     *            starting position at the buffer
     * @param len
     *            the number of bytes to read
     * @throws StorageException
     */
    public abstract void read(long pos, byte[] dstBuf, int off, int len) throws StorageException;

    /**
     * @param pos
     *            position at the storage
     * @param buf
     *            buffer that holds the data to write
     * @param off
     *            starting position at the buffer
     * @param len
     *            the number of bytes to write
     * @throws StorageException
     */
    public abstract void write(long pos, byte[] buf, int off, int len) throws StorageException;

    /**
     * reads into the byte buffer at the offset specified. note the position and limit of this byte buffer, must be set
     * before calling, and the position will be set to the limit after successfully fulfilling the request.
     * 
     * @param pos
     *            position at the storage
     * @param buffer
     *            where read data is stored to.
     * @throws StorageException
     */
    public abstract void read(long pos, ByteBuffer buffer) throws StorageException;

    /**
     * writes the byte buffer to a file at the offset specified. the position and limit must be set before calling, and
     * the position will be set to the limit after successfully fulfilling the request.
     * 
     * @param pos
     *            position at the storage
     * @param buffer
     *            buffer that holds the data to write
     * @throws StorageException
     */
    public abstract void write(long pos, ByteBuffer buffer) throws StorageException;

    /**
     * The size of the storage
     * 
     * @return
     */
    public abstract long size();

    /**
     * @return a string identifier for this storage
     */
    public String identifier() {
        return identifier;
    }

    /**
     * Storage is contained in PageAddress. Therefore, we need to implement equals and hashCode in order to compare
     * PageAddress
     */
    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof Storage)) {
            return false;
        }
        Storage other = (Storage) obj;

        if (hashCode == other.hashCode()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * comparing two storages with storage's size
     */
    @Override
    public int compareTo(Storage out) {
        if (out == null) {
            return 1;
        } else {
            if (this.size() > out.size()) {
                return 1;
            } else if (this.size() == out.size()) {
                return 0;
            } else {
                return -1;
            }
        }
    }

    @Override
    public String toString() {
        return "Storage [" + identifier() + "]";
    }

    public boolean isBroken() {
        return isBroken.get();
    }

    public void setBroken(boolean isBroken) {
        this.isBroken.set(isBroken);
    }

    public long getId() {
        return id;
    }

    public static String getDeviceName(String identifier) {
        String deviceName = identifier;
        if (deviceName != null && deviceName.contains("/")) {
            deviceName = deviceName.substring(deviceName.lastIndexOf('/') + 1);
        }
        return deviceName;
    }

    public int getIoDepth() {
        return ioDepth;
    }


    /**
     * finds the usable size of the storage device. This method first find a sector that cannot be read by starting at
     * the beginning of the device and doubling is search until it find a sector that cannot be read. Then is binary
     * searches until it finds the end of the device.
     *
     * @return the offset of the first sector that cannot be read.
     */
    protected long findUsableStorageSize(int sectorSize) {
        ByteBuffer buffer = DirectAlignedBufferAllocator.allocateAlignedByteBuffer(sectorSize, sectorSize);

        // first find a sector that is beyond the end of the device
        long maxSize = 1;
        boolean done = false;
        while (!done) {
            maxSize *= 2;
            try {
                buffer.clear();
                read(maxSize * sectorSize, buffer);
            } catch (Exception ioe) {
                done = true;
            }
        }

        // now binary search to find the first sector that cannot be read.
        long lo = 0;
        long hi = maxSize;

        while (lo + 1 < hi) {
            long mid = (hi + lo) / 2;
            try {
                buffer.clear();
                read(mid * sectorSize, buffer);
                lo = mid;
            } catch (Exception ex) {
                hi = mid;
            }
        }

        return (lo + 1) * sectorSize;
    }
}
